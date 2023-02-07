package com.diemlife.dao;

// Generated Jul 4, 2015 5:57:00 PM by Hibernate Tools 4.3.1

import com.diemlife.models.TokenAction;
import com.diemlife.models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

import static java.lang.String.format;

/**
 * Home object for domain model class TokenAction.
 * @see models.TokenAction
 * @author Hibernate Tools
 */
@Repository
public class TokenActionHome {

	@PersistenceContext
	EntityManager entityManager;
	
	private final static long VERIFICATION_TIME = 7L * 24 * 3600;

	public void persist(TokenAction transientInstance) {

		try {
			Logger.info("ABOUT TO PERSIST");
			Logger.info("TransientInstance = " + transientInstance.getToken() + " : " + transientInstance.getUser().getId());
			entityManager.persist(transientInstance);
		}
		catch (Exception e) {
			Logger.info("TokenActionHome:: persist : Error persisting token => " + e,e);
		}
	}

	public void remove(TokenAction persistentInstance) {

		EntityTransaction tx = null;
		try {
			entityManager.remove(persistentInstance);
		}
		catch (RuntimeException e) {
			Logger.info("TokenActionHome:: remove : Error removing token => " + e,e);
		}
	}

	public TokenAction merge(TokenAction detachedInstance) {

		EntityTransaction tx = null;
		try {
			tx = entityManager.getTransaction();
			tx.begin();

			TokenAction result = entityManager.merge(detachedInstance);

			tx.commit();

			return result;
		}
		catch (RuntimeException e) {
			if ( tx != null && tx.isActive() ) tx.rollback();
			throw e; // or display error message
		}
	}

	public TokenAction findById(Integer id) {
		Logger.debug("getting TokenAction instance with id: " + id);
		try {
			TokenAction instance = entityManager.find(TokenAction.class, id);
			Logger.debug("get successful");
			return instance;
		} catch (RuntimeException re) {
			Logger.error("get failed", re);
			throw re;
		}
	}

	public TokenAction create(String type, String token, User targetUser) {

		Logger.info("targetUser.id = " + targetUser.getId());
		//FIXME Vinayak
//		User user = UserHome.findById(targetUser.getId(), entityManager);

		//FIXME Vinayak
//		if (user == null) {
//			user = UserHome.findByIdReference(targetUser.getId(), entityManager);
//			Logger.info("GOT BY REF = " + user.getId());
//		}

		TokenAction ua = new TokenAction();
		//FIXME Vinayak
//		ua.setUser(user);
		ua.setToken(token);
		ua.setType(type);
		Date created = new Date();
		ua.setCreatedOn(created);
		ua.setExpiresOn(new Date(created.getTime() + VERIFICATION_TIME * 1000));

		persist(ua);
		Logger.info("PERSISTED = " + ua.getToken() + "user " + ua.getUser().getId());
		return ua;
	}

	public TokenAction findByToken(final String token, final String type) {
		try {
			return entityManager
                    .createQuery("SELECT t FROM TokenAction t WHERE lower(t.token) = :token AND lower(t.type) = :type", TokenAction.class)
                    .setParameter("token", token.toLowerCase())
                    .setParameter("type", type.toLowerCase())
                    .getSingleResult();
		} catch (final NoResultException e) {
			Logger.warn(format("Token action not found of type '%s' and token string [%s]", type, token));
			return null;
		} catch (final NonUniqueResultException e) {
			Logger.warn(format("Duplicate found for token action of type '%s' and token string [%s]", type, token), e);
            throw e;
		} catch (final PersistenceException e) {
			Logger.warn(format("Failed retrieving token action of type '%s' and token string [%s]", type, token), e);
			throw e;
		}
	}

	public TokenAction findByUserId(Integer userId, String type) {
		try {
			Query query = entityManager.createQuery("SELECT t FROM TokenAction t WHERE t.user.id = :userId AND lower(t.type) = :type");
			query.setParameter("userId", userId);
			query.setParameter("type", type.toLowerCase());
			return (TokenAction) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.info("token not found");
			return null;
		} catch (Exception re) {
			Logger.error("get failed", re);
			throw re;
		}
	}

	public void deleteByUser(User u, String type, EntityManager entityManager) {

		try {
			Query query = entityManager.createQuery("SELECT t FROM TokenAction t JOIN t.user u WHERE u = :user AND lower(t.type) = :type");
			query.setParameter("user", u);
			query.setParameter("type", type.toLowerCase());

			List<TokenAction> tokens = (List<TokenAction>) query.getResultList();

			for(TokenAction token: tokens)
			{
				this.remove(token);
			}

		}catch (NoResultException e){
			Logger.info("token not found");
		}
		catch (Exception re) {
			Logger.error("get failed", re);
			throw re;
		}
	}
}