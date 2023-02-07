package com.diemlife.dao;

import com.diemlife.models.QuestMapView;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

/**
 * DAO Quest map view
 * Created 03/11/2020
 *
 * @author SYushchenko
 */
@Repository
public class QuestMapViewDAO {
	
	@PersistenceContext
	EntityManager entityManager;

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
