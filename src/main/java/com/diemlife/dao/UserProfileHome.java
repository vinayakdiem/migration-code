package com.diemlife.dao;

// Generated Jul 4, 2015 5:57:00 PM by Hibernate Tools 4.3.1

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import play.Logger;
import com.diemlife.models.UserProfile;

/**
 * Home object for domain model class User.
 * @see models.User
 * @author Hibernate Tools
 */
@Repository
public class UserProfileHome {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public void persist(UserProfile transientInstance) {
		
		try {
		    entityManager.persist(transientInstance);
		}
		catch (RuntimeException e) {
		    throw e; // or display error message
		}
	}

	public void remove(UserProfile persistentInstance) {

		try {
		    entityManager.remove(persistentInstance);
		}
		catch (RuntimeException e) {
		    throw e; // or display error message
		}
	}

	public UserProfile merge(UserProfile detachedInstance) {

		try {
		    UserProfile result = entityManager.merge(detachedInstance);
		    return result;
		}
		catch (RuntimeException e) {
		    throw e; // or display error message
		}
	}

	public UserProfile findById(Integer id) {
		Logger.debug("getting User instance with id: " + id);
		try {
			UserProfile instance = entityManager.find(UserProfile.class, id);
			Logger.debug("get successful");
			return instance;
		} catch (RuntimeException re) {
			Logger.error("get failed", re);
			throw re;
		}
	}
}
