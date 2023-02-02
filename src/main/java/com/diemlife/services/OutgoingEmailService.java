package com.diemlife.services;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.ConfigurationSetDoesNotExistException;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.MailFromDomainNotVerifiedException;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.MessageRejectedException;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.constants.MailingTags;
import com.diemlife.constants.QuestMode;
import com.diemlife.dto.EmailTicketsDTO;
import com.diemlife.dto.FundraisingLinkDTO;
import com.diemlife.dto.StripeShippingDTO;
import com.diemlife.dto.SystemFailureDTO;
import models.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.format.number.CurrencyStyleFormatter;
import play.Logger;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.F.Tuple;
import play.libs.Json;
import play.libs.ws.WSAuthScheme;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.mvc.Http.RequestHeader;
import com.diemlife.services.StripeConnectService.ExportedProductVariant;
import com.diemlife.services.StripeConnectService.TicketsPurchaseOrder;
import com.diemlife.utils.TransactionBreakdown;

import javax.activation.DataSource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.apache.commons.mail.EmailAttachment.ATTACHMENT;
import static org.springframework.util.StringUtils.hasText;
import static com.diemlife.utils.URLUtils.seoFriendlyPublicQuestPath;

@Singleton
public class OutgoingEmailService {

    private static final String PLATFORM_EMAIL_ADDRESS = "admin@diemlife.com";
    private static final String PLATFORM_EMAIL_TEMPLATE = "DIEMlife <%s>";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";


    private final Config config;
    private final MessagesApi messages;
    private final WSClient wsClient;
    private final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Inject
    public OutgoingEmailService(final Config config, final MessagesApi messages, final WSClient wsClient) {
        this.config = config;
        this.messages = messages;
        this.wsClient = wsClient;
    }

    public void sendAccountCreatedEmail(final User recipient) {
        final Messages message = messages.preferred(emptyList());
        final String subject = message.at("email.account.creation.subject", recipient.getFirstName());

        sendEmail(buildEmailAddresses(recipient.getEmail()), subject, context -> new EmailContent(
                message.at("email.account.creation.body", recipient.getFirstName()),
                message.at("email.account.creation.body.text", recipient.getFirstName())
        ));
    }

    public void sendQuestInvitationEmail(final RequestHeader request, final User recipient, final User user, final Quests quest) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.quest.share.subject", user.getFirstName() + " " + user.getLastName());

        sendEmail(buildEmailAddresses(recipient.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, user);
            return new EmailContent(
                    message.at("email.quest.share.body.no.message.html", user.getFirstName(), questUrl, quest.getTitle(), quest.getQuestFeed(), recipient.getName()),
                    message.at("email.quest.share.body.text", user.getFirstName(), questUrl)
            );
        });
    }

    public void sendUserActivationPinCodeEmail(final RequestHeader request, final User user, final UserActivationPinCode code) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.activation.pin.subject", code.pinCode);
        sendEmail(buildEmailAddresses(user.getEmail()), subject, context -> new EmailContent(
                message.at("email.activation.pin.body", user.getFirstName(), code.pinCode),
                message.at("email.activation.pin.body.text", user.getFirstName(), code.pinCode)
        ));
    }

    public void sendFundraisingStartCreatorEmail(final RequestHeader request, final User creator, final Quests quest) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.fundraising.creator.start.subject", quest.getTitle());
        sendEmail(buildEmailAddresses(creator.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, creator);
            return new EmailContent(
                    message.at("email.fundraising.creator.start.body", quest.getTitle(), creator.getFirstName(), questUrl),
                    message.at("email.fundraising.creator.start.body.text", quest.getTitle(), creator.getFirstName(), questUrl)
            );
        });
    }

    public void sendFundraisingStartFundraiserEmail(final RequestHeader request, final User creator, final User fundraiser, final Quests quest) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.fundraising.fundraiser.start.subject", creator.getFirstName(), quest.getTitle());
        sendEmail(buildEmailAddresses(fundraiser.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, fundraiser);
            return new EmailContent(
                    message.at("email.fundraising.fundraiser.start.body", fundraiser.getFirstName(), quest.getTitle(), creator.getFirstName(), questUrl),
                    message.at("email.fundraising.fundraiser.start.body.text", fundraiser.getFirstName(), quest.getTitle(), creator.getFirstName(), questUrl)
            );
        });
    }


    public void sendFundraisingNotificationEmail(final RequestHeader request, final User creator, final User fundraiser, final Quests quest) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.fundraising.creator.notification.subject", quest.getTitle());
        sendEmail(buildEmailAddresses(creator.getEmail()), subject, context -> {
            final String creatorQuestUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, creator);
            final String fundraiserQuestUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, fundraiser);
            return new EmailContent(
                    message.at("email.fundraising.creator.notification.body", fundraiser.getFirstName(), fundraiserQuestUrl, creatorQuestUrl, quest.getTitle(), creator.getFirstName()),
                    message.at("email.fundraising.creator.notification.body.text", fundraiser.getFirstName(), fundraiserQuestUrl, creatorQuestUrl, quest.getTitle(), creator.getFirstName())
            );
        });
    }

    public void sendFundraisingBackingBeneficiaryEmail(final RequestHeader request, final User beneficiary, final UserEmailPersonal backer, final Quests quest, final Long amount, final String currency) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.fundraising.fundraiser.backing.subject", quest.getTitle());
        sendEmail(buildEmailAddresses(beneficiary.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, beneficiary);
            final CurrencyStyleFormatter amountFormatter = new CurrencyStyleFormatter();
            amountFormatter.setCurrency(Currency.getInstance(upperCase(currency)));
            final String formattedAmount = amountFormatter.print(Double.valueOf(amount) / 100.0D, Locale.US);
            return new EmailContent(
                    message.at("email.fundraising.fundraiser.backing.body", beneficiary.getFirstName(), backer.getFirstName(), quest.getTitle(), formattedAmount, questUrl),
                    message.at("email.fundraising.fundraiser.backing.body.text", beneficiary.getFirstName(), backer.getFirstName(), quest.getTitle(), formattedAmount, questUrl)
            );
        });
    }

    public void sendShareQuestEmail(final RequestHeader request, final User user, final Quests quest, final String email,
                                    final String note, final String toName) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.quest.share.subject", user.getFirstName() + " " + user.getLastName());
        sendEmail(buildEmailAddresses(email), subject, context -> {
            String questUrl;
            if (quest.getMode() == QuestMode.SUPPORT_ONLY || quest.getMode() == QuestMode.TEAM) {
                questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, quest.getUser());
            } else {
                questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, user);
            }
            if (note != null && !note.isEmpty()) {
                return new EmailContent(
                        message.at("email.quest.share.body.html", user.getFirstName(), questUrl, quest.getTitle(),
                                quest.getQuestFeed(), "'" + note + "'", toName),
                        message.at("email.quest.share.body.text", user.getFirstName(), questUrl)
                );
            } else {
                return new EmailContent(
                        message.at("email.quest.share.body.no.message.html", user.getFirstName(), questUrl, quest.getTitle(),
                                quest.getQuestFeed(), toName),
                        message.at("email.quest.share.body.text", user.getFirstName(), questUrl)
                );
            }
        });
    }

    public void sendQuestBackingConfirmationEmail(final RequestHeader request, final UserEmailPersonal user, final User backee, final Quests quest, Long amount,
                                                  Long transactionId) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.stripe.quest.backing.confirm.subject", backee.getFirstName());
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        final String dollarConversion = format.format(amount / 100.0);

        sendEmail(buildEmailAddresses(user.getEmail(), quest.getUser().getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, backee);
            return new EmailContent(
                    message.at("email.stripe.quest.backing.confirm.body",
                            dollarConversion,
                            quest.getTitle(),
                            transactionId, questUrl),
                    message.at("email.stripe.quest.backing.confirm.body.text",
                            dollarConversion,
                            quest.getTitle(),
                            transactionId, questUrl)
            );
        });
    }

    public void sendFundraisingBackingConfirmationEmail(final RequestHeader request, final UserEmailPersonal backer, final User backee, final Quests quest, final Long amount,
                                                        final Long totalAmt, final Long fees, final Double tip,final String last4,
                                                        final FundraisingLink link, final QuestTeam questTeam,
                                                        final List<Quests> parentQuestList) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.fundraising.backing.confirm.subject");
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        final String dollarConversionAmt = format.format(amount / 100.0);
        final String dollarConversionTotalAmt = format.format(totalAmt / 100.0);
        final String dollarConversionFees = format.format(fees / 100.0);
        final Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        final String username = quest.getUser().getUserName();
        final String _uname = username.length() > 10 ? username.substring(0, 10) : username;
        String donateToStr = "";
        for (Quests parentQuest: parentQuestList) {
            donateToStr += (donateToStr == "") ? parentQuest.getTitle() : " > " + parentQuest.getTitle();
        }
        donateToStr += (link.getCampaignName() == null) ? "" : (((donateToStr == "") ? "" : " > ") + link.getCampaignName());
        final String campaignStr = donateToStr;
        final String tipAmount = (tip == null || tip <= 0.0D) ? "0" : format.format(tip * amount / 100.0);

        sendEmail(buildEmailAddresses(backer.getEmail(), quest.getUser().getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, backee);
            return new EmailContent(
                    message.at("email.fundraising.backing.confirm.body",
                            sdf.format(date),
                            quest.getTitle(),
                            backer.getFirstName() + " " + backer.getLastName(),
                            questUrl,
                            dollarConversionAmt,
                            backee.isAbsorbFees() ? "$0.00" : dollarConversionFees,
                            dollarConversionTotalAmt,
                            "Payment Method: ..." + last4,
                            "DIEMLIFE Des: " + _uname.toUpperCase(),
                            backee.getFirstName() + " " + backee.getLastName(),
                            (questTeam != null && questTeam.getName() != null) ? questTeam.getName() : "",
                            campaignStr,
                            tipAmount == "0" ? "" : ("<b>Tip to DIEMlife:</b> "+tipAmount+"<br/>")), //leaving this here for now as we want a better way to convey billing info
                    message.at("email.fundraising.backing.confirm.body.text",
                            sdf.format(date),
                            quest.getTitle(),
                            backer.getFirstName() + " " + backer.getLastName(),
                            questUrl,
                            dollarConversionAmt,
                            backee.isAbsorbFees() ? "$0.00" : dollarConversionFees,
                            dollarConversionTotalAmt,
                            "Payment Method: ..." + last4,
                            "DIEMLIFE Des: " + _uname.toUpperCase(),
                            backee.getFirstName() + " " + backee.getLastName(),
                            (questTeam != null && questTeam.getName() != null) ? questTeam.getName() : "",
                            campaignStr,
                            tipAmount == "0" ? "" : ("<b>Tip to DIEMlife:</b> "+tipAmount+"<br/>"))
            );
        });
    }

    public void sendQuestBackedNotificationEmail(final RequestHeader request, final UserEmailPersonal user, final Quests quest, Long amount,
                                                 String note, User backee) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.stripe.quest.backer.confirm.subject");
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        final String dollarConversion = format.format(amount / 100.0);

        sendEmail(buildEmailAddresses(backee.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, backee);
            if (note == "" || note == null) {
                return new EmailContent(
                        message.at("email.stripe.quest.backer.confirm.no.note.body",
                                quest.getTitle(),
                                dollarConversion,
                                user.getFirstName(),
                                note,
                                questUrl,
                                backee.getFirstName()),
                        message.at("email.stripe.quest.backer.confirm.body.text", quest.getTitle(), dollarConversion,
                                backee.getFirstName(),
                                user.getFirstName(),
                                note)
                );
            }
            else {
                return new EmailContent(
                        message.at("email.stripe.quest.backer.confirm.body",
                                quest.getTitle(),
                                dollarConversion,
                                user.getFirstName(),
                                note,
                                questUrl,
                                backee.getFirstName()),
                        message.at("email.stripe.quest.backer.confirm.body.text", quest.getTitle(), dollarConversion,
                                backee.getFirstName(),
                                user.getFirstName(),
                                note)
                );
            }
        });
    }

    public void sendQuestBackedAnonNotificationEmail(final RequestHeader request, final UserEmailPersonal user, final Quests quest, Long amount,
                                                     String note, User backee) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.stripe.quest.backer.confirm.subject");
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        final String dollarConversion = format.format(amount / 100.0);

        sendEmail(buildEmailAddresses(backee.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, backee);
            return new EmailContent(
                    message.at("email.stripe.quest.backer.anon.confirm.body",
                            quest.getTitle(),
                            dollarConversion,
                            note,
                            questUrl,
                            backee.getFirstName()),
                    message.at("email.stripe.quest.backer.confirm.body.text", quest.getTitle(), amount)
            );
        });
    }

    public void sendOrderTicketConfirmationEmail(final RequestHeader request,
                                                 final Quests quest,
                                                 final UserEmailPersonal customerUser,
                                                 final PaymentTransaction transaction,
                                                 final TransactionBreakdown breakdown,
                                                 final TicketsPurchaseOrder order,
                                                 final StripeShippingDTO buyerInfo) {

        final Messages message = messages.preferred(request);
        final String subject = message.at("email.ticket.default.subject", quest.getTitle());

        sendEmail(buildEmailAddresses(customerUser.getEmail(), quest.getUser().getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, quest.getUser());
            final EmailTicketsDTO content = new EmailTicketsDTO();
            content.language = Locale.US.getLanguage();
            content.title = quest.getTitle();
            content.orderNumber = transaction.id.toString();
            content.orderDate = transaction.createdOn;
            content.orderTotal = breakdown.brutTotal;
            content.orderFees = breakdown.brutTotal - (breakdown.netTotal - breakdown.discount);
            content.orderDiscount = breakdown.discount;
            content.questUrl = questUrl;
            content.quest = quest;
            content.user = customerUser;
            content.tickets = order.orderItems.values().stream().filter(value -> value.getRight() > 0).collect(toList());
            content.orderQuantity = content.tickets.stream()
                    .map(Pair::getRight)
                    .reduce(0, Integer::sum);
            content.buyer = buyerInfo;

            return new EmailContent(
                    views.html.email.tickets.render(content).body(),
                    views.txt.email.tickets.render().body()
            );
        });
    }

    public void sendGenericOrderTicketConfirmationEmail(final RequestHeader request, final User user, final Quests quest, final Long orderId,
                                                        final String billingAddr1, final String billingAddr2, final String city, final String state,
                                                        final String zip, final String phone, final String purchaserEmail, final Long totalAmt,
                                                        final List<ExportedProductVariant> productVariant,
                                                        final Map<String, Integer> orderItems, final List<HappeningParticipant> participants, final List<String> details) {

        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        final String dollarConversion = format.format(totalAmt / 100.0);

        final String formattedAttributes = sortAttributes(productVariant, orderItems);

        final Messages message = messages.preferred(request);
        final String subject = message.at("email.ticket.default.subject", quest.getTitle());

        sendEmail(buildEmailAddresses(purchaserEmail, quest.getUser().getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, user);
            return new EmailContent(
                    message.at("email.ticket.generic.body.html",
                            user.getFirstName(),
                            questUrl,
                            quest.getTitle(),
                            quest.getUser().getFirstName() + " " + quest.getUser().getLastName(),
                            orderId,
                            stampTime(),
                            user.getFirstName() + " " + user.getLastName(),
                            billingAddr1,
                            billingAddr2 == null || billingAddr2.isEmpty() ? "" : billingAddr2,
                            city,
                            state,
                            zip,
                            user.getEmail(),
                            phone == null ? "" : phone,
                            dollarConversion,
                            formattedAttributes,
                            buildParticipantDetails(participants),
                            buildOrderDetails(participants, findTicketQuantity(productVariant, orderItems))),
                    message.at("email.ticket.generic.body.text", quest.getTitle())
            );
        });

    }

    public void sendContentReportEmail(final RequestHeader request, final User reporter, final String reportSubject, final Number id) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.admin.report.subject", reporter.getName());

        sendEmail(buildEmailAddresses(getAdminEmail()), subject, context -> new EmailContent(
                message.at("email.admin.report.body.html", reportSubject, id),
                message.at("email.admin.report.body.text", reportSubject, id)
        ));
    }

    public void sendAdminAddedEmail(final RequestHeader request, final User addedUser, final User adminUser, final Quests quest) {

        checkNotNull(addedUser, "addedUser is null");
        checkNotNull(adminUser, "adminUser is null");
        final Messages message = messages.preferred(request);
        final String userFullName = adminUser.getFirstName() + " " + adminUser.getLastName();
        final String subject = message.at("email.admin.added.subject", userFullName);

        sendEmail(buildEmailAddresses(addedUser.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, adminUser);
            return new EmailContent(
                    message.at("email.admin.added.body.html",
                            addedUser.getFirstName(),
                            adminUser.getFirstName(),
                            quest.getTitle(),
                            quest.getQuestFeed(),
                            questUrl),
                    message.at("email.admin.added.body.text",
                            addedUser.getFirstName(),
                            adminUser.getFirstName(),
                            quest.getTitle(),
                            quest.getQuestFeed(),
                            questUrl)
            );
        });
    }

    public void sendAdminRemovedEmail(final RequestHeader request, final User addedUser, final User adminUser, final Quests quest) {

        checkNotNull(addedUser, "addedUser is null");
        checkNotNull(adminUser, "adminUser is null");
        final Messages message = messages.preferred(request);
        final String userFullName = adminUser.getFirstName() + " " + adminUser.getLastName();
        final String subject = message.at("email.admin.removed.subject", userFullName);

        sendEmail(buildEmailAddresses(addedUser.getEmail()), subject, context -> {
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, adminUser);
            return new EmailContent(
                    message.at("email.admin.removed.body.html",
                            addedUser.getFirstName(),
                            adminUser.getFirstName(),
                            quest.getTitle(),
                            quest.getQuestFeed(),
                            questUrl),
                    message.at("email.admin.removed.body.text",
                            addedUser.getFirstName(),
                            adminUser.getFirstName(),
                            quest.getTitle(),
                            quest.getQuestFeed(),
                            questUrl)
            );
        });
    }

    public void sendProductionFailureEmail(final RequestHeader request, final SystemFailureDTO failure) {
        final Messages message = messages.preferred(request);
        final String subject = message.at("email.failure.report.subject", config.getString("play.env"), failure.id);

        final Date now = Calendar.getInstance().getTime();
        failure.date = new SimpleDateFormat(DATE_PATTERN).format(now);
        failure.ip = request.remoteAddress();
        failure.method = request.method();
        failure.uri = request.uri();

        final String body = StringEscapeUtils.unescapeJava(prettyPrint(failure));

        getSupportEmailAddresses()
                .forEach(email -> sendMultipartEmail(buildEmailAddresses(email), subject, context -> new EmailContent(null, body)));
    }

    private <T> String prettyPrint(final T object) {
        try {
            return writer.writeValueAsString(object);
        } catch (final JsonProcessingException e) {
            Logger.error(e.getMessage(), e);
            return StringUtils.EMPTY;
        }
    }

    public void sendMessageAllDoersEmail(final RequestHeader request, final Collection<String> emails, final Quests quest,
                                         final String messageSubject, final String messageInput, final boolean plain,
                                         final User adminUser, final User currentUser) {
        checkNotNull(quest, "quest");

        final Messages message = messages.preferred(request);
        final String subject = isBlank(messageSubject)
                ? message.at("email.message.all.doers.subject", quest.getTitle(), currentUser.getName())
                : trim(messageSubject);
        final String tag = MailingTags.Quest.name().concat(quest.getId().toString());

        emails.forEach(email -> sendEmailViaMailgun(buildEmailAddressesWithCC(email, adminUser.getEmail()), subject, tag, context -> {
            Logger.info(format("sending message all doers email to [%s]", email));
            final String questUrl = config.getString(context.environment.getBaseUrlKey()) + seoFriendlyPublicQuestPath(quest, adminUser);
            final String cleanHtmlMessage = Jsoup.clean(messageInput, Whitelist.basic());
            final String htmlBody = plain
                    ? cleanHtmlMessage
                    : message.at("email.message.all.doers.body.html", cleanHtmlMessage, questUrl, currentUser.getName());
            final String textBody = plain
                    ? Jsoup.parse(cleanHtmlMessage).text()
                    : message.at("email.message.all.doers.body.text", Jsoup.parse(cleanHtmlMessage).text(), questUrl);
            return new EmailContent(htmlBody, textBody);
        }));
    }

    private void sendEmailViaMailgun(final EmailAddresses addresses,
                                     final String subject,
                                     final String tag,
                                     final Function<EmailContentContext, EmailContent> emailContentProvider) {
        final EmailContent emailContent = emailContentProvider.apply(buildEmailContentContext());
        final WSRequest wsRequest = wsClient.url(config.getString("mailgun.baseUrl") + "/messages");
        wsRequest.setAuth("api", config.getString("mailgun.apiKey"), WSAuthScheme.BASIC);
        wsRequest.setContentType("application/x-www-form-urlencoded");
        final List<NameValuePair> query = new ArrayList<>();
        query.add(new BasicNameValuePair("subject", subject));
        query.add(new BasicNameValuePair("from", addresses.from));
        query.add(new BasicNameValuePair("to", addresses.to));
        query.add(new BasicNameValuePair("bcc", addresses.bcc));
        query.add(new BasicNameValuePair("text", emailContent.text));
        query.add(new BasicNameValuePair("html", emailContent.html));
        query.add(new BasicNameValuePair("o:tag", tag));
        wsRequest.post(URLEncodedUtils.format(query, UTF_8)).thenApply(wsResponse -> {
            if (wsResponse.getStatus() == 200) {
                Logger.info("Message sent with ID " + wsResponse.asJson().get("id"));
            } else {
                try {
                    Logger.warn("Message not sent: " + writer.writeValueAsString(wsResponse));
                } catch (final JsonProcessingException e) {
                    Logger.warn("Message not sent and cannot be logged", e);
                }
            }
            return wsResponse;
        });
    }

    private void sendEmail(final EmailAddresses addresses,
                           final String subject,
                           final Function<EmailContentContext, EmailContent> emailContentProvider) {

        final EmailContent emailContent = emailContentProvider.apply(buildEmailContentContext());
        final AmazonSimpleEmailService client = buildEmailService(buildEmailServerConfig());
        Destination destination;
        if (!hasText(addresses.otherBcc)) {
            destination = new Destination().withToAddresses(addresses.to).withBccAddresses(addresses.bcc);
        } else {
            destination = new Destination().withToAddresses(addresses.to).withBccAddresses(addresses.bcc, addresses.otherBcc);
        }
        final SendEmailRequest request = new SendEmailRequest()
                .withDestination(destination)
                .withMessage(new Message()
                        .withBody(new Body()
                                .withHtml(new Content().withCharset(UTF_8).withData(emailContent.html))
                                .withText(new Content().withCharset(UTF_8).withData(emailContent.text))
                        )
                        .withSubject(new Content().withCharset(UTF_8).withData(subject)))
                .withSource(addresses.from);
        try {
            client.sendEmail(request);
            Logger.info("Successfully sent email to " + addresses.to);
        } catch (final MessageRejectedException | MailFromDomainNotVerifiedException | ConfigurationSetDoesNotExistException e) {
            Logger.error("Email request rejected by AWS :\n" + Json.toJson(request).toString(), e);
        } catch (final RuntimeException e) {
            Logger.error("Unexpected error occurred when sending email request to AWS :\n" + Json.toJson(request).toString(), e);
        }
    }

    private void sendMultipartEmail(final EmailAddresses addresses,
                                    final String subject,
                                    final Function<EmailContentContext, EmailContent> provider) {
        final AmazonSimpleEmailService client = buildEmailService(buildEmailServerConfig());
        final EmailContent emailContent = provider.apply(buildEmailContentContext());

        final HtmlEmail email = new HtmlEmail();
        email.setMailSession(Session.getDefaultInstance(new Properties()));
        try {
            email.addTo(addresses.to);
            if (addresses.bcc != null) {
                email.addBcc(addresses.bcc);
            }
            email.setFrom(addresses.from);
            email.setSubject(subject);
            email.setBoolHasAttachments(isNotEmpty(emailContent.attachments));
            if (isNotBlank(emailContent.text)) {
                email.setMsg(emailContent.text);
            }
            if (isNotBlank(emailContent.html)) {
                email.setHtmlMsg(emailContent.html);
            }
        } catch (final EmailException e) {
            Logger.error(format("Cannot configure email to [%s]", addresses.to), e);
        }

        if (isNotEmpty(emailContent.attachments)) {
            Arrays.stream(emailContent.attachments).forEach(attachment -> {
                final DataSource ds = new ByteArrayDataSource(attachment.data, attachment.type);
                try {
                    email.attach(ds, attachment.name, "", ATTACHMENT);
                } catch (final EmailException e) {
                    Logger.error(format("Cannot add attachment to email to [%s]", addresses.to), e);
                }
            });
        }

        final ByteArrayOutputStream out = writeEmailToStream(email);
        if (out != null) {
            final SendRawEmailRequest request = new SendRawEmailRequest()
                    .withRawMessage(new RawMessage()
                            .withData(ByteBuffer.wrap(out.toByteArray())));
            try {
                client.sendRawEmail(request);
                Logger.info("Successfully sent email to " + addresses.to);
            } catch (final MessageRejectedException | MailFromDomainNotVerifiedException | ConfigurationSetDoesNotExistException e) {
                Logger.error("Email request rejected by AWS :\n" + Json.toJson(request).toString(), e);
            }
        }
    }

    private ByteArrayOutputStream writeEmailToStream(final MultiPartEmail email) {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            email.buildMimeMessage();
            email.getMimeMessage().writeTo(out);
            return out;
        } catch (final IOException | MessagingException | EmailException e) {
            Logger.error(e.getMessage(), e);
            return null;
        }
    }

    private EmailServerConfig buildEmailServerConfig() {
        return new EmailServerConfig(
                config.getString("aws.ses.username"),
                config.getString("aws.ses.password"),
                Regions.US_EAST_1
        );
    }

    private String getAdminEmail() {
        //need to use hardcoded email here instead of from the config file as we
        //dont have a admin@diemlife.com user on prod that some functions need.
        return format(PLATFORM_EMAIL_TEMPLATE, PLATFORM_EMAIL_ADDRESS);
    }

    private EmailAddresses buildEmailAddresses(final String to) {
        final String admin = getAdminEmail();
        return new EmailAddresses.Builder()
                .withFrom(admin)
                .withTo(to)
                .withBCC(admin)
                .build();
    }

    private EmailAddresses buildEmailAddresses(final String to, final String otherBcc) {
        final String admin = getAdminEmail();
        return new EmailAddresses.Builder()
                .withFrom(admin)
                .withTo(to)
                .withBCC(admin)
                .withOtherBCC(otherBcc)
                .build();
    }

    private EmailAddresses buildEmailAddressesWithCC(final String to, final String cc) {
        final String admin = getAdminEmail();
        return new EmailAddresses.Builder()
                .withFrom(admin)
                .withTo(to)
                .withCC(cc)
                .withBCC(admin)
                .build();
    }

    private EmailContentContext buildEmailContentContext() {
        return new EmailContentContext(DeploymentEnvironments.valueOf(config.getString("application.mode")));
    }

    private AmazonSimpleEmailService buildEmailService(final EmailServerConfig config) {
        final BasicAWSCredentials credentials = new BasicAWSCredentials(config.username, config.password);
        return AmazonSimpleEmailServiceClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(config.region)
                .build();
    }

    private static class EmailServerConfig {
        private final String username;
        private final String password;
        private final Regions region;

        private EmailServerConfig(final String username, final String password, final Regions region) {
            this.username = username;
            this.password = password;
            this.region = region;
        }
    }

    private static class EmailAddresses {
        private final String from;
        private final String to;
        private final String bcc;
        private final String otherBcc;
        private final String cc;

        private EmailAddresses(final String from, final String to, final String bcc, final String cc,
                               final String otherBcc) {
            this.from = from;
            this.to = to;
            this.bcc = bcc;
            this.otherBcc = otherBcc;
            this.cc = cc;
        }

        public static final class Builder {
            private String from;
            private String to;
            private String bcc;
            private String otherBcc;
            private String cc;

            Builder withFrom(String from) {
                this.from = from;
                return this;
            }

            Builder withTo(String to) {
                this.to = to;
                return this;
            }

            Builder withBCC(String bcc) {
                this.bcc = bcc;
                return this;
            }

            Builder withOtherBCC(String bcc) {
                this.otherBcc = bcc;
                return this;
            }

            Builder withCC(String cc) {
                this.cc = cc;
                return this;
            }

            public EmailAddresses build() {
                return new EmailAddresses(from,
                        to,
                        bcc,
                        cc,
                        otherBcc);
            }
        }
    }

    private static class EmailContentContext {
        private final DeploymentEnvironments environment;

        private EmailContentContext(final DeploymentEnvironments environment) {
            this.environment = environment;
        }
    }

    private static class EmailContent {
        private final String html;
        private final String text;
        private final EmailAttachment[] attachments;

        private EmailContent(final String html, final String text, final EmailAttachment... attachments) {
            this.html = html;
            this.text = text;
            this.attachments = attachments;
        }
    }

    private static class EmailAttachment {
        private final String name;
        private final String type;
        private final byte[] data;

        private EmailAttachment(final String name, final String type, final byte[] data) {
            this.name = name;
            this.type = type;
            this.data = data;
        }
    }

    private String sortAttributes(List<ExportedProductVariant> productVariant,
                                  final Map<String, Integer> orderItems) {
        List<String> attributes = new ArrayList<>();
        int count = 0;
        for (ExportedProductVariant variant : productVariant) {
            for (Map.Entry<String, Integer> entry : orderItems.entrySet()) {
                if (entry.getKey().equals(variant.id)) {
                    count++;
                }
            }
        }

        attributes.add("Quantity: " + count + "x");
        return attributes.toString().replace("[", "").replace("]", "").replace(",", "").trim();
    }

    private int findTicketQuantity(List<ExportedProductVariant> productVariants,
                                   final Map<String, Integer> orderItems) {
        int count = 0;
        for (ExportedProductVariant variant : productVariants) {
            for (Map.Entry<String, Integer> entry : orderItems.entrySet()) {
                if (entry.getKey().equals(variant.id)) {
                    count++;
                }
            }
        }
        return count;
    }

    private String stampTime() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        return dateFormat.format(date);
    }

    private String buildOrderDetails(List<HappeningParticipant> participants, int count) {
        if (count > 0) {
            int num = 1;
            StringBuilder builder = new StringBuilder();
            for (HappeningParticipant participant : participants) {
                builder.append("Ticket ").append(num).append(" Details");
                builder.append("<br/>");
                builder.append("Burger Temperature: ").append(participant.person.burgerTemp).append("<br/> Cheese: ")
                        .append(participant.person.withCheese)
                        .append("<br/> Special Requests: ")
                        .append(participant.person.specialRequests);
                builder.append("<br/><br/>");
                num++;
            }
            return builder.toString();
        }
        return EMPTY;
    }

    private String buildParticipantDetails(List<HappeningParticipant> participants) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br/>");
        for (HappeningParticipant participant : participants) {
            builder.append(participant.person.firstName).append(" ").append(participant.person.lastName);
            builder.append("<br/>");
        }
        return builder.toString();
    }

    private List<String> getSupportEmailAddresses() {
        return config.getStringList("support.emailAddresses");
    }
}
