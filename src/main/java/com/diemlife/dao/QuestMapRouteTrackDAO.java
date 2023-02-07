package com.diemlife.dao;

import com.diemlife.models.QuestMapRouteTrack;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

/**
 * QuestMapRouteTrackDAO
 * Created 30/11/2002
 *
 * @author SYushchenko
 */
@Repository
public class QuestMapRouteTrackDAO extends TypedDAO<QuestMapRouteTrack>{

	@PersistenceContext
	EntityManager entityManager;
}
