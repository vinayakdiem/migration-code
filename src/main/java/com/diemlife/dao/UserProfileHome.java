package dao;

// Generated Jul 4, 2015 5:57:00 PM by Hibernate Tools 4.3.1

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import play.Logger;
import models.UserProfile;

/**
 * Home object for domain model class User.
 * @see models.User
 * @author Hibernate Tools
 */
public class UserProfileHome {
	
	public void persist(UserProfile transientInstance, EntityManager entityManager) {
		
		try {
		    entityManager.persist(transientInstance);
		}
		catch (RuntimeException e) {
		    throw e; // or display error message
		}
	}

	public void remove(UserProfile persistentInstance, EntityManager entityManager) {

		try {
		    entityManager.remove(persistentInstance);
		}
		catch (RuntimeException e) {
		    throw e; // or display error message
		}
	}

	public UserProfile merge(UserProfile detachedInstance, EntityManager entityManager) {

		try {
		    UserProfile result = entityManager.merge(detachedInstance);
		    return result;
		}
		catch (RuntimeException e) {
		    throw e; // or display error message
		}
	}

	public UserProfile findById(Integer id, EntityManager entityManager) {
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
