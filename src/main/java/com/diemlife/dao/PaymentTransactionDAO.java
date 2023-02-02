package com.diemlife.dao;

import com.diemlife.dto.TransactionExportDTO;
import exceptions.RequiredParameterMissingException;
import models.PaymentTransaction;
import models.RecurringQuestBackingTransaction;
import models.TicketPurchaseTransaction;
import play.Logger;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class PaymentTransactionDAO extends TypedDAO<PaymentTransaction> {

    public PaymentTransactionDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public List<PaymentTransaction> getLastTransactions(final Integer userId, final boolean all) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to retrieve transactions information");
        }
        try {
            final TypedQuery<PaymentTransaction> query = entityManager.createQuery("SELECT pt " +
                    "FROM PaymentTransactions pt " +
                    "INNER JOIN FundraisingTransactions ft ON ft.id = pt.id " +
                    "WHERE (pt.to.user.id = :userId OR pt.from.user.id = :userId OR ft.intermediary.id = :userId) " +
                    (all ? "" : "AND pt.createdOn > :monthAgo ") +
                    "AND pt.valid = TRUE " +
                    "ORDER BY pt.createdOn DESC", PaymentTransaction.class);
            query.setParameter("userId", userId);
            if (!all) {
                final LocalDate now = LocalDate.now();
                final LocalDateTime monthAgo = now.minus(1, ChronoUnit.MONTHS).atStartOfDay();
                final Timestamp monthAgoTs = Timestamp.from(monthAgo.atZone(ZoneId.systemDefault()).toInstant());

                query.setParameter("monthAgo", monthAgoTs);
            }
            return query.getResultList();
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("Unable to retrieve PaymentTransaction for userId %s", userId), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all the transactions related to a Quest ID
     *
     * @param questId ID of the quest from which you want to download the transactions
     * @return Collection {@link TransactionExportDTO}
     */
    public List<PaymentTransaction> getTransactionsPerQuest(final Integer questId) {

        try {
            final Query query = entityManager.createNativeQuery("SELECT * " +
                    "FROM payment_transaction pt " +
                    "WHERE pt.valid = TRUE " +
                    "AND (pt.stripe_transaction_target = 'Q' OR pt.stripe_transaction_target = 'S' OR pt.stripe_transaction_target = 'F') " +
                    "AND pt.quest_id = :questId " +
                    "ORDER BY pt.created_on DESC", PaymentTransaction.class);
            query.setParameter("questId", questId);

            return (List<PaymentTransaction>) query.getResultList();
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error("Unable to retrieve PaymentTransaction for questId {}", questId, e);
            return Collections.emptyList();
        }
    }

    public Long getOutgoingTransactionsCount(final Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to retrieve outgoing transactions count");
        }
        try {
            return entityManager
                    .createQuery("select count(pt) " +
                                    "from PaymentTransactions pt " +
                                    "where pt.from.user.id = :userId",
                            Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } catch (final NoResultException nre) {
            Logger.warn(format("No PaymentTransaction found for userId %s", userId), nre);
            return 0L;
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("Unable to retrieve PaymentTransaction for userId %s", userId), e);
            throw e;
        }
    }

    public Long getHappeningTransactionsCountForQuest(final Integer questId) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        try {
            return entityManager
                    .createQuery("select count(t) from TicketPurchaseTransactions t  where t.event.quest.id = :questId", Long.class)
                    .setParameter("questId", questId)
                    .getSingleResult();
        } catch (final NoResultException nre) {
            Logger.warn(format("No TicketPurchaseTransactions found for questId %s", questId), nre);
            return 0L;
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("Unable to retrieve TicketPurchaseTransactions for questId %s", questId), e);
            throw e;
        }
    }

    public List<TicketPurchaseTransaction> getHappeningTransactionsForQuest(final Integer questId) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        try {
            return entityManager
                    .createQuery("SELECT t " +
                            "FROM TicketPurchaseTransactions t " +
                            "WHERE t.event.quest.id = :questId " +
                            "ORDER BY t.createdOn", TicketPurchaseTransaction.class)
                    .setParameter("questId", questId)
                    .getResultList();
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("Unable to retrieve TicketPurchaseTransactions for questId %s", questId), e);
            throw e;
        }
    }

    public Long getQuestBackingTransactionsCountForQuest(final Integer questId) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        try {
            return entityManager
                    .createQuery("select count(t) from QuestBackingTransactions t where t.quest.id = :questId", Long.class)
                    .setParameter("questId", questId)
                    .getSingleResult();
        } catch (final NoResultException nre) {
            Logger.warn(format("No QuestBackingTransactions found for questId %s", questId), nre);
            return 0L;
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("Unable to retrieve QuestBackingTransactions for questId %s", questId), e);
            throw e;
        }
    }

    public List<RecurringQuestBackingTransaction> getSubscriptionsForQuestAndBeneficiary(final Integer questId,
                                                                                         final Integer beneficiaryId) {
        if (questId == null) {
            throw new IllegalArgumentException("Subscribed Quest ID is required");
        }
        if (beneficiaryId == null) {
            throw new IllegalArgumentException("Subscription beneficiary ID is required");
        }
        try {
            return entityManager
                    .createQuery(
                            "SELECT t FROM RecurringQuestBackingTransactions t " +
                                    "WHERE t.quest.id = :questId AND t.to.user.id = :beneficiaryId",
                            RecurringQuestBackingTransaction.class)
                    .setParameter("questId", questId)
                    .setParameter("beneficiaryId", beneficiaryId)
                    .getResultList();
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format(
                    "Unable to retrieve RecurringQuestBackingTransactions for Quest ID [%s] and beneficiary ID [%s]",
                    questId,
                    beneficiaryId
            ), e);
            throw e;
        }
    }

    public List<RecurringQuestBackingTransaction> getSubscriptionsForCustomer(final Integer customerId) {
        if (customerId == null) {
            throw new RequiredParameterMissingException("customerId");
        }
        try {
            return entityManager
                    .createQuery(
                            "SELECT t FROM RecurringQuestBackingTransactions t WHERE t.from.user.id = :customerId",
                            RecurringQuestBackingTransaction.class)
                    .setParameter("customerId", customerId)
                    .getResultList();
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format(
                    "Unable to retrieve RecurringQuestBackingTransactions for customer ID [%s]",
                    customerId), e);
            throw e;
        }
    }

}
