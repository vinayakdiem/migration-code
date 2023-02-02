package com.diemlife.dao;

import com.diemlife.models.QuestMapRouteWaypoint;

import javax.persistence.EntityManager;
import java.util.List;

public class QuestMapRouteWaypointDAO extends TypedDAO<QuestMapRouteWaypoint> {

    public QuestMapRouteWaypointDAO(EntityManager entityManager) {
        super(entityManager);
    }

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
