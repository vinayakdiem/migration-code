package com.diemlife.dao;

import java.math.BigInteger;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.diemlife.models.HappeningParticipant;

@Repository
public class HappeningParticipantDAO extends TypedDAO<HappeningParticipant> {

    public List<HappeningParticipant> findByHappeningId(final Long eventId) {
        return entityManager.createQuery("" +
                "SELECT hp FROM HappeningParticipants hp " +
                "WHERE hp.event.id = :eventId " +
                "ORDER BY hp.registrationDate DESC", HappeningParticipant.class)
                .setParameter("eventId", eventId)
                .getResultList();
    }

    // sql statement to match quest id and user id to check if ticket purchased. returns count, thus bigint and > 0 are set
    public boolean hasEventRegistered(final Integer questId, final Integer userId) {
        final BigInteger count = (BigInteger) entityManager.createNativeQuery("SELECT count(pt.id) "+
            "FROM payment_transaction pt "+
            "INNER JOIN stripe_customer sc ON sc.id = pt.from_customer_id "+
            "INNER JOIN user u ON u.Id = sc.user_id "+
            "LEFT OUTER JOIN event e ON e.id = pt.event_id "+
            "LEFT OUTER JOIN quest_feed qf ON qf.id = e.quest_id "+
            "WHERE qf.id = :questId "+
            "AND sc.user_id = :userId "+
            "AND pt.stripe_transaction_target = 'E'")
            .setParameter("questId", questId)
            .setParameter("userId", userId)
            .getSingleResult();
        return count.longValue() > 0;
    }
}
