package com.diemlife.utils;

//import static org.apache.commons.lang.StringUtils.lowerCase;

import java.io.Serializable;
import java.util.function.Function;

import com.diemlife.models.FundraisingTransaction;
import com.diemlife.models.PaymentTransaction;
import com.diemlife.models.QuestBacking;
import com.diemlife.models.QuestBackingTransaction;
import com.diemlife.models.Quests;
import com.diemlife.models.TicketPurchaseTransaction;
import com.diemlife.models.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.BankAccount;
import com.stripe.model.Charge;
import com.stripe.model.Dispute;
import com.stripe.model.Payout;
import com.typesafe.config.ConfigFactory;

public abstract class TransactionResponse<T extends TransactionResponse.TransactionParty> implements Serializable {

    public String type;
    public T from;
    public T to;
    public String displayName;
    public Long amount;
    public Long fee;
    public String currency;
    public Long created;
    public String description;
    public Integer questId;
    public String status;
    public String lastFour;
    public boolean mailing;

    public static class ExportTransactionResponse extends TransactionResponse<ExportTransactionParty> {
        public Long id;
        public Long net;
        public Long refunded;
        public String coupon;
        public boolean disputed;
        public String isRefunded;
        public Long disputeAmount;
        public String disputeStatus;
        public String disputeReason;
        public Long disputeCreated;
        public Long disputeEvidence;
    }

    public static class WebTransactionResponse extends TransactionResponse<WebTransactionParty> {
        public boolean incoming;
        public boolean outgoing;
        public boolean isAnonymous;
        public boolean absorbFees;
    }

    public static TransactionResponse fromCharge(final Charge charge, final PaymentTransaction transaction, final User currentUser, final boolean export, final QuestBacking questBacking) throws StripeException {

        if (charge == null || transaction == null) {
            return null;
        }

        final long totalAmount = charge.getAmount() == null ? 0L : charge.getAmount();
        final long totalFees;
        final long netAmount;
        final long refundAmount = charge.getAmountRefunded();
        final long tip = questBacking != null && questBacking.getTip() != null ? questBacking.getTip() : 0;

        if (charge.getTransferObject() != null) {
            netAmount = charge.getTransferObject().getAmount();
            totalFees = totalAmount - netAmount - tip;
        } else if (charge.getBalanceTransactionObject() != null) {
            netAmount = charge.getBalanceTransactionObject().getNet();
            totalFees = charge.getBalanceTransactionObject().getFee();
        } else {
            return null;
        }

        final TransactionResponse<?> response = export ? new ExportTransactionResponse() : new WebTransactionResponse();
        if (export) {
            response.amount = totalAmount - tip;
            response.fee = totalFees;
            ExportTransactionResponse.class.cast(response).net = netAmount;
            ExportTransactionResponse.class.cast(response).refunded = refundAmount;
            ExportTransactionResponse.class.cast(response).coupon = transaction.couponUsed;
            ExportTransactionResponse.class.cast(response).disputed = charge.getDisputed();
            ExportTransactionResponse.class.cast(response).isRefunded = "" + charge.getRefunded();
            if (!charge.getRefunded() && refundAmount != 0) {
                ExportTransactionResponse.class.cast(response).isRefunded = "PARTIAL REFUND";
            }
            if (charge.getDisputed() && charge.getDispute() != null) {
                if (ConfigFactory.load().getString("play.env").equals("LOCAL")) {
                    Stripe.apiKey = ConfigFactory.load().getString("stripe.api.key");
                } else if (ConfigFactory.load().getString("play.env").equals("DEV")) {
                    Stripe.apiKey = ConfigFactory.load().getString("stripe.api.key");
                } else {
                    Stripe.apiKey = ConfigFactory.load().getConfig("play-authenticate").getConfig("stripe").getString("clientSecret");
                }
                Dispute dispute = Dispute.retrieve(charge.getDispute());
                if (dispute != null) {
                    ExportTransactionResponse.class.cast(response).disputeAmount = dispute.getAmount();
                    ExportTransactionResponse.class.cast(response).disputeStatus = dispute.getStatus();
                    ExportTransactionResponse.class.cast(response).disputeReason = dispute.getReason();
                    ExportTransactionResponse.class.cast(response).disputeCreated = dispute.getCreated() * 1000;
                    ExportTransactionResponse.class.cast(response).disputeEvidence = dispute.getEvidenceDetails() == null ?
                            null : dispute.getEvidenceDetails().getDueBy() * 1000;
                }
            }
        } else if (currentUser.getId().equals(transaction.from.user.getId())) {
            response.amount = totalAmount;
            response.fee = totalFees;
            WebTransactionResponse.class.cast(response).outgoing = true;
            WebTransactionResponse.class.cast(response).incoming = false;
        } else {
            response.amount = netAmount;
            response.fee = null;
            WebTransactionResponse.class.cast(response).outgoing = false;
            WebTransactionResponse.class.cast(response).incoming = true;
        }
        response.currency = charge.getCurrency();
        response.created = charge.getCreated() * 1000;
        response.status = charge.getStatus();
        response.mailing = transaction.isMailing;
        if (questBacking != null ) {
            response.displayName = BackerDisplayNameUtils.getBackerDisplayName(questBacking);
        }

        new TransactionDataFiller<>(transaction, currentUser, export)
                .fillTransactionData(response)
                .fillTransactionDescription(response)
                .fillTransactionQuestInfo(response);

        return response;
    }

    public static TransactionResponse fromPayout(final Payout payout, final PaymentTransaction transaction, final User currentUser, final boolean export, final QuestBacking questBacking) {
        if (payout == null || transaction == null) {
            return null;
        }
        final TransactionResponse response = export ? new ExportTransactionResponse() : new WebTransactionResponse();
        response.amount = payout.getAmount();
        response.currency = payout.getCurrency();
        response.created = payout.getCreated() * 1000;
        response.status = payout.getStatus();
        response.lastFour = payout.getDestinationObject() instanceof BankAccount
                ? BankAccount.class.cast(payout.getDestinationObject()).getLast4()
                : null;
        response.mailing = transaction.isMailing;
        if (questBacking != null ) {
            response.displayName = BackerDisplayNameUtils.getBackerDisplayName(questBacking);
        }
        if (response instanceof WebTransactionResponse) {
            WebTransactionResponse.class.cast(response).description = payout.getStatementDescriptor();
            WebTransactionResponse.class.cast(response).outgoing = true;
            WebTransactionResponse.class.cast(response).incoming = false;
        }

        new TransactionDataFiller<>(transaction, currentUser, export)
                .fillTransactionData(response);

        return response;
    }

    public static TransactionResponse fromFreeTicket(final String currency, final PaymentTransaction transaction, final User currentUser, final boolean export, final QuestBacking questBacking) {
        if (transaction == null) {
            return null;
        }
        final TransactionResponse response = export ? new ExportTransactionResponse() : new WebTransactionResponse();
        response.amount = 0L;
        response.currency = currency;
        response.created = transaction.createdOn == null ? null : transaction.createdOn.getTime();
        response.status = (transaction.stripeTransactionId).toLowerCase();
        response.mailing = transaction.isMailing;
        if (questBacking != null ) {
            response.displayName = BackerDisplayNameUtils.getBackerDisplayName(questBacking);
        }
        if (response instanceof WebTransactionResponse) {
            WebTransactionResponse.class.cast(response).outgoing = true;
            WebTransactionResponse.class.cast(response).incoming = false;
        }

        new TransactionDataFiller<>(transaction, currentUser, export)
                .fillTransactionData(response)
                .fillTransactionDescription(response)
                .fillTransactionQuestInfo(response)
                .done();

        return response;
    }

    private static class TransactionDataFiller<T extends PaymentTransaction> {

        private final T transaction;
        private final Quests quest;
        private final TransactionPartyConverter partyConverter;
        private final boolean export;

        private TransactionDataFiller(final T transaction, final User currentUser, final boolean export) {
            this.transaction = transaction;
            this.quest = getQuest(transaction);
            this.partyConverter = new TransactionPartyConverter(currentUser, export);
            this.export = export;
        }

        private TransactionDataFiller fillTransactionData(final TransactionResponse response) {
            response.type = transaction.getStripeTransactionType().name();
            response.from = partyConverter.apply(transaction.from.user);
            response.to = transaction instanceof FundraisingTransaction
                    ? partyConverter.apply(FundraisingTransaction.class.cast(transaction).intermediary)
                    : partyConverter.apply(transaction.to.user);
            if (export) {
                ExportTransactionResponse.class.cast(response).id = transaction.id;
            } else {
                if (transaction.isAnonymous) {
                    response.from.firstName = null;
                    response.from.lastName = null;
                    response.from.userId = null;
                }
                WebTransactionResponse.class.cast(response).isAnonymous = transaction.isAnonymous;
                WebTransactionResponse.class.cast(response).absorbFees = !(transaction instanceof TicketPurchaseTransaction)
                        && transaction.to.user.isAbsorbFees();
            }
            return this;
        }

        private TransactionDataFiller fillTransactionDescription(final TransactionResponse response) {
            if (quest != null) {
                response.description = quest.getTitle();
            }
            return this;
        }

        private TransactionDataFiller fillTransactionQuestInfo(final TransactionResponse response) {
            if (quest != null) {
                response.questId = quest.getId();
            }
            return this;
        }

        private static Quests getQuest(final PaymentTransaction transaction) {
            if (transaction instanceof QuestBackingTransaction) {
                return ((QuestBackingTransaction) transaction).quest;
            } else if (transaction instanceof TicketPurchaseTransaction
                    && ((TicketPurchaseTransaction) transaction).event != null) {
                return ((TicketPurchaseTransaction) transaction).event.quest;
            }
            return null;
        }

        private void done() {
        }
    }

    public static abstract class TransactionParty implements Serializable {
        public Integer userId;
        public String firstName;
        public String lastName;
    }

    public static class ExportTransactionParty extends TransactionParty {
        public String email;
    }

    public static class WebTransactionParty extends TransactionParty {
        public String username;
        public String profilePhotoURL;
        public boolean isMyself;
    }

    private static class TransactionPartyConverter implements Function<User, TransactionParty> {

        private final User currentUser;
        private final boolean export;

        private TransactionPartyConverter(final User currentUser, final boolean export) {
            this.currentUser = currentUser;
            this.export = export;
        }

        @Override
        public TransactionParty apply(final User user) {
            final TransactionParty result = export ? new ExportTransactionParty() : new WebTransactionParty();
            result.userId = user.getId();
            result.firstName = user.getFirstName();
            result.lastName = user.getLastName();
            if (export) {
                ExportTransactionParty.class.cast(result).email = user.getEmail();
            } else {
                WebTransactionParty.class.cast(result).username = user.getUserName();
                WebTransactionParty.class.cast(result).profilePhotoURL = user.getProfilePictureURL();
                WebTransactionParty.class.cast(result).isMyself = currentUser.getId().equals(user.getId());
            }
            return result;
        }
    }

}