package dao;

import models.QuestMapRouteTrack;

import javax.persistence.EntityManager;

/**
 * QuestMapRouteTrackDAO
 * Created 30/11/2002
 *
 * @author SYushchenko
 */
public class QuestMapRouteTrackDAO extends TypedDAO<QuestMapRouteTrack>{

    public QuestMapRouteTrackDAO(EntityManager entityManager) {
        super(entityManager);
    }
}
