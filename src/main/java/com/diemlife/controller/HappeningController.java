package com.diemlife.controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Dispute;
import com.typesafe.config.ConfigFactory;
import com.diemlife.dao.HappeningDAO;
import com.diemlife.dao.HappeningParticipantDAO;
import com.diemlife.dao.PaymentTransactionDAO;
import com.diemlife.models.Happening;
import com.diemlife.models.HappeningAddOnType;
import com.diemlife.models.HappeningParticipant;
import com.diemlife.models.LeaderboardMember;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.TicketPurchaseTransaction;
import com.diemlife.models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import play.Logger;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.LeaderboardService;
import services.StripeConnectService;
import services.StripeConnectService.ExportedProduct;
import services.StripeConnectService.ExportedProductVariant;
import services.UserProvider;
import com.diemlife.utils.TransactionResponse;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static com.diemlife.utils.QuestSecurityUtils.canEditQuest;

public class HappeningController extends Controller {

    private static final String FREE_TICKETS_ORDER = "FREE";
    private static final String DATE_FORMAT_MM_DD_YYYY = "MM/dd/yyyy";

    private final JPAApi jpaApi;
    private final UserProvider userProvider;
    private final LeaderboardService leaderboardService;
    private final StripeConnectService stripeConnectService;
    private final MessagesApi messages;

    @Inject
    public HappeningController(final JPAApi jpaApi,
                               final UserProvider userProvider,
                               final LeaderboardService leaderboardService,
                               final StripeConnectService stripeConnectService,
                               final MessagesApi messages) {
        this.jpaApi = jpaApi;
        this.userProvider = userProvider;
        this.leaderboardService = leaderboardService;
        this.stripeConnectService = stripeConnectService;
        this.messages = messages;
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result refreshLeaderboardMembers(final Integer questId) {
        return doIfQuestIsHappening(questId, this::doRefreshLeaderboardMembers);
    }

    @Transactional(readOnly = true)
    @JwtSessionLogin(required = true)
    public Result getEventParticipantsReport(final Integer questId) {
        return doIfQuestIsHappening(questId, this::doGetEventParticipantsReport);
    }

    private Result doRefreshLeaderboardMembers(final Happening event, final User user) {
        if (canEditQuest(event.quest, user)) {
            final List<LeaderboardMember> newMembers = leaderboardService.updateLeaderboardMembers(event);
            if (newMembers.isEmpty()) {
                return ok();
            } else {
                return ok(Json.toJson(newMembers.size()));
            }
        } else {
            return forbidden();
        }
    }

    //TODO This eventually needs to updated so that this report is downloaded in a similar fashion to the donor reports (see functional features doc)
    private Result doGetEventParticipantsReport(final Happening event, final User user) {
        final Messages messages = this.messages.preferred(request());
        final EntityManager em = this.jpaApi.em();

        if (!canEditQuest(event.quest, user)) {
            return forbidden();
        }

        final StripeAccount merchant = userProvider.getStripeCustomerByUserId(event.quest.getCreatedBy(), StripeAccount.class);
        if (merchant == null) {
            return forbidden();
        }
        final ExportedProduct product = stripeConnectService.retrieveProduct(merchant, event.stripeProductId);
        if (product == null) {
            return notFound();
        }

        final Workbook workbook = new XSSFWorkbook();
        final String formattedQuestName = event.quest.getTitle().replaceAll("[^a-zA-Z0-9]", "");
        final Sheet sheet = workbook.createSheet(formattedQuestName);

        final AtomicInteger rowsCounter = new AtomicInteger(0);
        final Row headerRow = sheet.createRow(rowsCounter.getAndIncrement());

        populateHeaderRow(headerRow, event, messages);

        final PaymentTransactionDAO transactionDAO = new PaymentTransactionDAO(em);
        final HappeningParticipantDAO participantDAO = new HappeningParticipantDAO(em);
        final List<TicketPurchaseTransaction> orders = transactionDAO.getHappeningTransactionsForQuest(event.quest.getId());
        final List<HappeningParticipant> participants = participantDAO.findByHappeningId(event.id);

        orders.forEach(order -> {
            final List<HappeningParticipant> orderParticipants = participants.stream()
                    .filter(participant -> {
                        /*
                          We have had to manually enter participants for leaderboards
                          which causes this to blow up. This should be removed once
                          the leaderboard epic is complete.
                         */
                        if (participant.order != null && participant.order.id != null) {
                            return participant.order.id.equals(order.id);
                        } else {
                            return false;
                        }
                    })
                    .collect(toList());
            final List<HappeningParticipant> actualParticipants = orderParticipants.isEmpty()
                    ? singletonList((HappeningParticipant) null)
                    : orderParticipants;

            for (final @Nullable HappeningParticipant participant : actualParticipants) {
                final Row row = sheet.createRow(rowsCounter.getAndIncrement());
                final AtomicInteger columnsCounter = new AtomicInteger(0);

                final Cell columnPaymentId = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                columnPaymentId.setCellValue(order.id);

                final Cell columnDate = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                columnDate.setCellValue(new SimpleDateFormat(DATE_FORMAT_MM_DD_YYYY).format(order.createdOn));

                if (event.participantFields.hasName) {
                    final Cell columnFirstName = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnFirstName.setCellValue(participant.person.firstName);
                    }
                    final Cell columnLastName = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnLastName.setCellValue(participant.person.lastName);
                    }
                }
                if (event.participantFields.hasEmail) {
                    final Cell columnEmail = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnEmail.setCellValue(participant.person.email);
                    }
                }
                if (event.participantFields.hasPhone) {
                    final Cell columnPhone = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnPhone.setCellValue(participant.person.cellPhone);
                    }
                }
                if (event.participantFields.hasGender) {
                    final Cell columnGender = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnGender.setCellValue(format("%s", participant.person.gender));
                    }
                }
                if (event.participantFields.hasBirthDate && !event.participantFields.hasAge) {
                    final Cell columnBirthDate = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnBirthDate.setCellValue(participant.person.birthDate == null ? "" : new SimpleDateFormat(DATE_FORMAT_MM_DD_YYYY).format(participant.person.birthDate));
                    }
                } else if (!event.participantFields.hasBirthDate && event.participantFields.hasAge) {
                    final Cell columnAge = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnAge.setCellValue(participant.person.age == null ? "" : format("%s", participant.person.age));
                    }
                }
                //TODO left commented out since it was adding an extra blank column that was shifting everything incorrectly to the right
//                else {
//                    final Cell columnEmpty = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
//                    if (participant != null) {
//                        columnEmpty.setCellValue("");
//                    }
//                }
                if (event.participantFields.hasZipOnly) {
                    final Cell columnAddress = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnAddress.setCellValue(Stream.of(
                                participant.address.zip).filter(StringUtils::isNotBlank).collect(joining(" ")));
                    }
                }
                if (event.participantFields.hasAddress) {
                    final Cell columnAddress = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnAddress.setCellValue(participant.address == null ? "" : Stream.of(
                                participant.address.lineOne,
                                participant.address.lineTwo,
                                participant.address.zip,
                                participant.address.city,
                                participant.address.state,
                                participant.address.country).filter(StringUtils::isNotBlank).collect(joining(" "))
                        );
                    }
                }
                if (event.participantFields.hasEmergency) {
                    final Cell columnEmergency = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnEmergency.setCellValue(participant.contact == null ? "" : Stream.of(
                                participant.contact.name,
                                participant.contact.email,
                                participant.contact.phone).filter(StringUtils::isNotBlank).collect(joining(" "))
                        );
                    }
                }
                if (event.addOns.stream().anyMatch(addOn -> HappeningAddOnType.T_SHIRT.equals(addOn.addOnType))) {
                    final Cell columnTShirt = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (participant != null) {
                        columnTShirt.setCellValue(participant.person.shirtSize);
                    }
                }

                final Cell columnCoupon = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                columnCoupon.setCellValue(order.couponUsed);

                final ExportedProductVariant variant = participant != null && isNotBlank(participant.stripeSkuId)
                        ? Stream.of(product.variants).filter(sku -> participant.stripeSkuId.equals(sku.id)).findFirst().orElse(null)
                        : null;

                final Cell columnSku = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                columnSku.setCellValue(variant == null ? "" : variant.attributes.entrySet()
                        .stream()
                        .filter(entry -> !entry.getKey().equalsIgnoreCase("order"))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.joining(",")));

                if (FREE_TICKETS_ORDER.equalsIgnoreCase(order.stripeTransactionId)) {
                    final Cell columnTransaction = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    columnTransaction.setCellValue("0");
                } else {
                    final Charge stripeCharge = stripeConnectService.retrieveChargeInformation(order.stripeTransactionId, merchant, true);
                    final Cell columnTransaction = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    final Pair<Long, String> amountAndCurrency = Optional.ofNullable(stripeCharge)
                            .map(Charge::getTransferObject)
                            .map(transfer -> new ImmutablePair<>(transfer.getAmount(), transfer.getCurrency()))
                            .orElse(Optional.ofNullable(stripeCharge)
                                    .map(Charge::getBalanceTransactionObject)
                                    .map(transaction -> new ImmutablePair<>(transaction.getNet().longValue(), transaction.getCurrency()))
                                    .orElse(Optional.ofNullable(stripeCharge)
                                            .map(charge -> new ImmutablePair<>(charge.getAmount(), charge.getCurrency()))
                                            .orElse(new ImmutablePair<>(0L, "usd"))
                                    )
                            );
                    if (participant != null) {
                        if (participant.skuPrice != null) {
                            columnTransaction.setCellValue(format("%.2f", participant.skuPrice / 100.0f));
                        } else {
                            columnTransaction.setCellValue(format("%.2f", amountAndCurrency.getLeft() / 100.0f));
                        }
                    } else {
                        columnTransaction.setCellValue(format("%.2f", amountAndCurrency.getLeft() / 100.0f));
                    }

                    // adding currency column
                    final Cell columnCurrency = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    columnCurrency.setCellValue(amountAndCurrency.getRight());

                    final Cell columnRefunded = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (stripeCharge == null || stripeCharge.getRefunded() == null) {
                        columnRefunded.setCellValue(false);
                    } else {
                        columnRefunded.setCellValue(stripeCharge.getRefunded());
                    }

                    //Added logic for partial refunds since getRefunded() returns TRUE only for full refunds
                    if (stripeCharge != null && !stripeCharge.getRefunded() && stripeCharge.getAmountRefunded() != 0) {
                        columnRefunded.setCellValue("PARTIAL REFUND");
                    }

                    final Cell columnRefundedAmount = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (stripeCharge == null || stripeCharge.getRefunded() == null) {
                        columnRefundedAmount.setCellValue(format("%.2f", Float.valueOf(0) / 100.0f));
                    } else {
                        columnRefundedAmount.setCellValue(format("%.2f", Float.valueOf(stripeCharge.getAmountRefunded()) / 100.0f));
                    }

                    final Cell columnDisputed = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                    if (stripeCharge == null) {
                        columnDisputed.setCellValue(false);
                    } else {
                        columnDisputed.setCellValue(stripeCharge.getDisputed());
                    }

                    //Need to grab secret key in order to use the Dispute.retrieve call
                    if (stripeCharge != null && stripeCharge.getDisputed() && stripeCharge.getDispute() != null) {
                        if (ConfigFactory.load().getString("play.env").equals("LOCAL")) {
                            Stripe.apiKey = ConfigFactory.load().getString("stripe.api.key");
                        } else if (ConfigFactory.load().getString("play.env").equals("DEV")) {
                            Stripe.apiKey = ConfigFactory.load().getString("stripe.api.key");
                        } else {
                            Stripe.apiKey = ConfigFactory.load().getConfig("play-authenticate").getConfig("stripe").getString("clientSecret");
                        }
                        Dispute dispute = null;
                        try {
                            dispute = Dispute.retrieve(stripeCharge.getDispute());
                        } catch (StripeException e) {
                            e.printStackTrace();
                        }
                        if (dispute != null) {
                            final Cell columnDisputeAmount = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                            columnDisputeAmount.setCellValue(format("%.2f", Float.valueOf(dispute.getAmount()) / 100.0f));

                            final Cell columnDisputeStatus = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                            columnDisputeStatus.setCellValue(dispute.getStatus());

                            final Cell columnDisputeReason = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                            columnDisputeReason.setCellValue(dispute.getReason());

                            final Cell columnDisputeDate = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                            columnDisputeDate.setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(dispute.getCreated() * 1000)));

                            final Cell columnDisputeEvidenceBy = row.createCell(columnsCounter.getAndIncrement(), CellType.STRING);
                            columnDisputeEvidenceBy.setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(dispute.getEvidenceDetails() == null ?
                                    null : dispute.getEvidenceDetails().getDueBy() * 1000)));
                        }
                    }

                }

            }
        });

        autoSizeColumns(workbook, sheet, headerRow);

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            workbook.write(output);
        } catch (final IOException e) {
            Logger.error("Cannot write event participants report to file", e);

            return internalServerError();
        }

        response().setHeader(CACHE_CONTROL, "no-cache");

        return ok(output.toByteArray())
                .as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .withHeader(CONTENT_DISPOSITION, "attachment; filename=\"event-participants.xlsx\"");
    }

    private void populateHeaderRow(final Row headerRow, final Happening event, final Messages messages) {
        final AtomicInteger headerCounter = new AtomicInteger(0);

        final Cell headerPaymentId = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerPaymentId.setCellValue(messages.at(ParticipantReportHeaders.PAYMENT_ID.translationKey));

        final Cell headerDate = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerDate.setCellValue(messages.at(ParticipantReportHeaders.DATE.translationKey));

        if (event.participantFields.hasName) {
            final Cell headerFirstName = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerFirstName.setCellValue(messages.at(ParticipantReportHeaders.FIRST_NAME.translationKey));
            final Cell headerLastName = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerLastName.setCellValue(messages.at(ParticipantReportHeaders.LAST_NAME.translationKey));
        }
        if (event.participantFields.hasEmail) {
            final Cell headerEmail = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerEmail.setCellValue(messages.at(ParticipantReportHeaders.EMAIL.translationKey));
        }
        if (event.participantFields.hasPhone) {
            final Cell headerPhone = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerPhone.setCellValue(messages.at(ParticipantReportHeaders.PHONE.translationKey));
        }
        if (event.participantFields.hasGender) {
            final Cell headerGender = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerGender.setCellValue(messages.at(ParticipantReportHeaders.GENDER.translationKey));
        }
        if (event.participantFields.hasBirthDate && !event.participantFields.hasAge) {
            final Cell headerBirthDate = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerBirthDate.setCellValue(messages.at(ParticipantReportHeaders.BIRTH_DATE.translationKey));
        }
        if (!event.participantFields.hasBirthDate && event.participantFields.hasAge) {
            final Cell headerBirthDate = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerBirthDate.setCellValue(messages.at(ParticipantReportHeaders.AGE.translationKey));
        }
        if (event.participantFields.hasAddress) {
            final Cell headerAddress = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerAddress.setCellValue(messages.at(ParticipantReportHeaders.ADDRESS.translationKey));
        }
        if (event.participantFields.hasZipOnly) {
            final Cell headerAddress = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerAddress.setCellValue(messages.at(ParticipantReportHeaders.ADDRESS.translationKey));
        }
        if (event.participantFields.hasEmergency) {
            final Cell headerEmergency = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerEmergency.setCellValue(messages.at(ParticipantReportHeaders.EMERGENCY_CONTACT.translationKey));
        }
        if (event.addOns.stream().anyMatch(addOn -> HappeningAddOnType.T_SHIRT.equals(addOn.addOnType))) {
            final Cell headerTShirt = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
            headerTShirt.setCellValue(messages.at(ParticipantReportHeaders.T_SHIRT_SIZE.translationKey));
        }

        final Cell headerCoupon = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerCoupon.setCellValue(messages.at(ParticipantReportHeaders.COUPON_USED.translationKey));

        final Cell headerSku = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerSku.setCellValue(messages.at(ParticipantReportHeaders.PRODUCT_VARIANT.translationKey));

        final Cell headerTransaction = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerTransaction.setCellValue(messages.at(ParticipantReportHeaders.TRANSACTION_INFO.translationKey));

        final Cell headerCurrency = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerCurrency.setCellValue(messages.at(ParticipantReportHeaders.TRANSACTION_CURRENCY.translationKey));

        final Cell headerRefunded = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerRefunded.setCellValue(messages.at(ParticipantReportHeaders.REFUNDED.translationKey));

        final Cell headerRefundedAmount = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerRefundedAmount.setCellValue(messages.at(ParticipantReportHeaders.AMOUNT_REFUNDED.translationKey));

        final Cell headerDisputed = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerDisputed.setCellValue(messages.at(ParticipantReportHeaders.DISPUTED.translationKey));

        final Cell headerDisputeAmount = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerDisputeAmount.setCellValue(messages.at(ParticipantReportHeaders.DISPUTE_AMOUNT.translationKey));

        final Cell headerDisputeStatus = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerDisputeStatus.setCellValue(messages.at(ParticipantReportHeaders.DISPUTE_STATUS.translationKey));

        final Cell headerDisputeReason = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerDisputeReason.setCellValue(messages.at(ParticipantReportHeaders.DISPUTE_REASON.translationKey));

        final Cell headerDisputeDate = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerDisputeDate.setCellValue(messages.at(ParticipantReportHeaders.DISPUTE_CREATED.translationKey));

        final Cell headerDisputeEvidence = headerRow.createCell(headerCounter.getAndIncrement(), CellType.STRING);
        headerDisputeEvidence.setCellValue(messages.at(ParticipantReportHeaders.DISPUTE_EVIDENCE.translationKey));
    }

    private void autoSizeColumns(final Workbook workbook, final Sheet sheet, final Row headerRow) {
        final Iterator<Cell> headerCellIterator = headerRow.cellIterator();
        final CellStyle boldStyle = workbook.createCellStyle();
        final Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        while (headerCellIterator.hasNext()) {
            final Cell cell = headerCellIterator.next();
            final int columnIndex = cell.getColumnIndex();
            cell.setCellStyle(boldStyle);
            sheet.autoSizeColumn(columnIndex);
        }
    }


    private Result doIfQuestIsHappening(final Integer questId, final BiFunction<Happening, User, Result> function) {
        if (questId == null) {
            return badRequest();
        }
        final User user = userProvider.getUser(session());
        if (user == null) {
            return forbidden();
        }
        final EntityManager em = jpaApi.em();
        final HappeningDAO happeningDAO = new HappeningDAO(em);
        final Happening event = happeningDAO.getHappeningByQuestId(questId);
        if (event == null) {
            return notFound();
        } else {
            return function.apply(event, user);
        }
    }

    private enum ParticipantReportHeaders {
        DATE("event.report.participant.date"),
        FIRST_NAME("event.report.participant.first-name"),
        LAST_NAME("event.report.participant.last-name"),
        EMAIL("event.report.participant.email"),
        PHONE("event.report.participant.phone"),
        GENDER("event.report.participant.gender"),
        BIRTH_DATE("event.report.participant.birth-date"),
        AGE("event.report.participant.age"),
        ADDRESS("event.report.participant.address"),
        EMERGENCY_CONTACT("event.report.participant.emergency-contact"),
        T_SHIRT_SIZE("event.report.participant.t-shirt-size"),
        PAYMENT_ID("event.report.participant.payment-id"),
        TRANSACTION_INFO("event.report.participant.transaction-info"),
        TRANSACTION_CURRENCY("event.report.participant.transaction-currency"),
        COUPON_USED("event.report.participant.coupon-used"),
        PRODUCT_VARIANT("event.report.participant.product-variant"),
        REFUNDED("event.report.participant.refunded"),
        AMOUNT_REFUNDED("event.report.participant.refunded-amount"),
        DISPUTED("event.report.participant.disputed"),
        DISPUTE_AMOUNT("event.report.participant.disputed-amount"),
        DISPUTE_STATUS("event.report.participant.dispute-status"),
        DISPUTE_REASON("event.report.participant.dispute-reason"),
        DISPUTE_CREATED("event.report.participant.dispute.created"),
        DISPUTE_EVIDENCE("event.report.participant.dispute.evidence");

        private final String translationKey;

        ParticipantReportHeaders(final String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
