package dao;

// Generated Jul 4, 2015 5:57:00 PM by Hibernate Tools 4.3.1

import com.feth.play.module.pa.user.AuthUser;
import models.LinkedAccount;
import models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Home object for domain model class LinkedAccount.
 * @see models.LinkedAccount
 * @author Hibernate Tools
 */
public class LinkedAccountHome {

	public void persist(LinkedAccount transientInstance, EntityManager entityManager) {

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

	public void update(LinkedAccount linkedAccount, AuthUser authUser, EntityManager entityManager) {

		linkedAccount.setProviderKey(authUser.getProvider());
		linkedAccount.setProviderUserId(authUser.getId());

		this.merge(linkedAccount, entityManager);
	}
}