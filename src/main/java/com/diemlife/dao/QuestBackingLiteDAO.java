package com.diemlife.dao;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.dto.FundraisingTotalDTO;

@Repository
public class QuestBackingLiteDAO {

	 @PersistenceContext
	 private EntityManager entityManager;

    public List<FundraisingTotalDTO> getFundraisingTotals(final int questId, final int userId) {
        return Stream.of(entityManager.createQuery("SELECT qb.amountInCents, pt.stripeTransactionId" +
                " FROM FundraisingTransactions pt " +
                " JOIN QuestBackings qb ON pt.id = qb.paymentTransaction.id" +
                " WHERE pt.quest.id = :questId AND pt.intermediary.id = :userId")
                .setParameter("questId", questId)
                .setParameter("userId", userId)
                .getResultList().toArray())
                .map(Object[].class::cast)
                .map(FundraisingTotalDTO::from)
                .collect(Collectors.toList());
    }
}
