package com.diemlife.dao;

import com.diemlife.models.QuestMapView;

import javax.persistence.EntityManager;

/**
 * DAO Quest map view
 * Created 03/11/2020
 *
 * @author SYushchenko
 */
public class QuestMapViewDAO {

    private final EntityManager entityManager;

    /**
     * Constructor with parameters
     *
     * @param entityManager {@link EntityManager}
     */
    public QuestMapViewDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Find quest map view by questMapViewId
     *
     * @param questMapViewId id
     * @return {@link QuestMapView}
     */
    public QuestMapView findQuestMapViewById(final Integer questMapViewId) {
        return entityManager.find(QuestMapView.class, questMapViewId);
    }
}
