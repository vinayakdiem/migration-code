package com.diemlife.dao;

import com.diemlife.models.QuestMapRouteWaypoint;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestMapRouteWaypointDAO extends TypedDAO<QuestMapRouteWaypoint> {

	@PersistenceContext
	EntityManager entityManager;

    public List<QuestMapRouteWaypoint> findAllQuestMapRouteWaypoint(final Long questMapRouteId) {
        return entityManager.createQuery("SELECT qmrw FROM QuestMapRouteWaypoint qmrw " +
                "WHERE qmrw.questMapRouteId = :questMapRouteId ", QuestMapRouteWaypoint.class)
                .setParameter("questMapRouteId", questMapRouteId)
                .getResultList();
    }

    public List<QuestMapRouteWaypoint> findAllQuestMapRouteWaypointsByQuestId(final Integer questId) {
        return entityManager.createQuery("" +
                "SELECT qmrw FROM QuestMapRouteWaypoint qmrw " +
                "JOIN QuestMapRoute qmr ON qmrw.questMapRouteId = qmr.id " +
                "WHERE qmr.questId = :questId AND qmr.active = TRUE " +
                "ORDER BY qmrw.sequence ASC", QuestMapRouteWaypoint.class )
                .setParameter("questId", questId)
                .getResultList();
    }
}
