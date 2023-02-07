package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.Happening;
import com.diemlife.models.ReferredQuest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReferredQuestDAO extends TypedDAO<ReferredQuest> {

	@PersistenceContext
	EntityManager entityManager;

    public List<ReferredQuest> findReferredQuestForHappening(final Happening event) {
        if (event == null) {
            throw new RequiredParameterMissingException("event");
        }
        return entityManager.createQuery("SELECT rq FROM ReferredQuests rq WHERE rq.event.id = :eventId", ReferredQuest.class)
                .setParameter("eventId", event.id)
                .getResultList();
    }

}
