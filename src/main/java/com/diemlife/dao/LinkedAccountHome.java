package com.diemlife.dao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import com.diemlife.models.LinkedAccount;
import com.diemlife.models.User;

// Generated Jul 4, 2015 5:57:00 PM by Hibernate Tools 4.3.1

//FIXME Raj
//import com.feth.play.module.pa.user.AuthUser;

import play.Logger;

/**
 * Home object for domain model class LinkedAccount.
 * @see models.LinkedAccount
 * @author Hibernate Tools
 */
@Repository
public class LinkedAccountHome {
	
	@PersistenceContext
	EntityManager entityManager;

	public void persist(LinkedAccount transientInstance) {

		try {
			entityManager.persist(transientInstance);
		}
		catch (RuntimeException e) {
			throw e; // or display error message
		}
	}

	public void remove(LinkedAccount persistentInstance, EntityManager entityManager) {

		try {
			entityManager.remove(persistentInstance);
		}
		catch (RuntimeException e) {
			throw e; // or display error message
		}
	}

	public LinkedAccount merge(LinkedAccount detachedInstance, EntityManager entityManager) {

		try {
			LinkedAccount result = entityManager.merge(detachedInstance);
			return result;
		} catch (Exception e) {
			throw e; // or display error message
		}
	}

	public LinkedAccount findById(Integer id, EntityManager entityManager) {
		Logger.debug("getting LinkedAccount instance with id: " + id);
		try {
			LinkedAccount instance = entityManager.find(LinkedAccount.class, id);
			Logger.debug("get successful");
			return instance;
		} catch (RuntimeException re) {
			Logger.error("get failed", re);
			throw re;
		}
	}

	public LinkedAccount findByProviderKey(User user, String key, EntityManager entityManager) {

		try {
			Query query = entityManager.createQuery("SELECT l FROM LinkedAccount l WHERE lower(l.providerKey) = :pKey AND l.user = :user");
			query.setParameter("pKey", key.toLowerCase());
			query.setParameter("user", user);

			LinkedAccount linkedAccount = (LinkedAccount) query.getSingleResult();

			return linkedAccount;
		}catch (NoResultException e) {
			Logger.info("account not found");
			return null;
		}
		catch (Exception re) {
			Logger.error("error: ", re);
			throw re;
		}
	}

	public LinkedAccount create(User userAccount, String providerKey, String providerUserId, EntityManager entityManager) {

		LinkedAccount linkedAccount = null;

		try {
			Query query = entityManager.createQuery("SELECT l FROM LinkedAccount l WHERE lower(l.providerKey) = :pKey AND l.user = :user");
			query.setParameter("pKey", providerKey.toLowerCase());
			query.setParameter("user", userAccount);

			linkedAccount = (LinkedAccount) query.getSingleResult();

			//return linkedAccount;
		}catch (NoResultException e) {
			Logger.info("account not found");
		}
		catch (Exception re) {
			Logger.error("error: ", re);
			throw re;
		}

		if(linkedAccount ==  null){
			linkedAccount = new LinkedAccount();
			linkedAccount.setUser(userAccount);
			linkedAccount.setProviderKey(providerKey);
			linkedAccount.setProviderUserId(providerUserId);
		}else{
			linkedAccount.setProviderUserId(providerUserId);
		}

		return this.merge(linkedAccount, entityManager);
	}

	//FIXME Raj
	/*public void update(LinkedAccount linkedAccount, AuthUser authUser, EntityManager entityManager) {

		linkedAccount.setProviderKey(authUser.getProvider());
		linkedAccount.setProviderUserId(authUser.getId());

		this.merge(linkedAccount, entityManager);
	}
*/
}