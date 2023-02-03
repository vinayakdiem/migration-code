package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.Happening;
import com.diemlife.models.ReferredQuest;

import javax.persistence.EntityManager;
import java.util.List;

public class ReferredQuestDAO extends TypedDAO<ReferredQuest> {

    public ReferredQuestDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public List<ReferredQuest> findReferredQuestForHappening(final Happening event) {
        if (event == null) {
            throw new RequiredParameterMissingException("event");
        }
        return entityManager.createQuery("SELECT rq FROM ReferredQuests rq WHERE rq.event.id = :eventId", ReferredQuest.class)
                .setParameter("eventId", event.id)
                .getResultList();
    }

}
