package com.diemlife.dao;

import com.diemlife.models.QuestMapRouteSegment;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

/**
 * DAO QuestMapRouteSegmentDAO
 * Created 30/11/2020
 *
 * @author SYushchenko
 */
@Repository
public class QuestMapRouteSegmentDAO extends TypedDAO<QuestMapRouteSegment> {
	@PersistenceContext
	EntityManager entityManager;
}
