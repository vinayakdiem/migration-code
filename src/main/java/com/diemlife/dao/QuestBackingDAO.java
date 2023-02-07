package com.diemlife.dao;

import com.diemlife.models.QuestBacking;
import com.diemlife.models.User;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Repository
public class QuestBackingDAO extends TypedDAO<QuestBacking> {

	 @PersistenceContext
	 private EntityManager entityManager;
	 
    public List<QuestBacking> getQuestBackingsByQuestBackingIds(final List<Long> questBackingIds) {
        return questBackingIds.isEmpty() ? Collections.emptyList() :
                this.entityManager.createQuery("SELECT qb FROM QuestBackings qb " +
                        " WHERE qb.id IN (:questBackingIds) ", QuestBacking.class)
                        .setParameter("questBackingIds", questBackingIds)
                        .getResultList();
    }

    public List<QuestBacking> getQuestBackingsByPaymentTransactions(final List<Long> paymentTransactions) {
        return paymentTransactions.isEmpty() ? Collections.emptyList() :
                this.entityManager.createQuery("SELECT qb FROM QuestBackings qb " +
                        " WHERE qb.paymentTransaction.id IN (:paymentTransactionId) ", QuestBacking.class)
                        .setParameter("paymentTransactionId", paymentTransactions)
                        .getResultList();
    }

    public List<QuestBacking> getBackingsForUser(final User user) {
        return user == null || user.getId() == null ? emptyList() : this.entityManager
                .createQuery("SELECT qb FROM QuestBackings qb WHERE qb.beneficiary.id = :userId", QuestBacking.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    public List<String> getBackersEmails(final Integer questId) {
        return Stream.of(this.entityManager.createNativeQuery("" +
                "SELECT pi.email FROM personal_info pi " +
                "      INNER JOIN quest_backing qb ON qb.billing_personal_info_id = pi.id " +
                "      INNER JOIN payment_transaction pt ON pt.id = qb.payment_transaction_id " +
                "      INNER JOIN quest_feed qf ON qf.id = pt.quest_id " +
                "WHERE qf.id = :questId")
                .setParameter("questId", questId)
                .getResultList()
                .toArray())
                .map(Object::toString)
                .collect(toList());
    }

}
