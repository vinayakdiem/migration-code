package com.diemlife.dao;

import com.diemlife.models.QuestMapRouteSegment;

import javax.persistence.EntityManager;

/**
 * DAO QuestMapRouteSegmentDAO
 * Created 30/11/2020
 *
 * @author SYushchenko
 */
public class QuestMapRouteSegmentDAO extends TypedDAO<QuestMapRouteSegment> {

    public QuestMapRouteSegmentDAO(EntityManager entityManager) {
        super(entityManager);
    }
}
