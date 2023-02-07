package com.diemlife.dao;

import com.diemlife.models.User;
import com.diemlife.models.UserFavorites;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by andrew on 5/8/17.
 */
@Repository
public class UserFavoritesDAO {

	@PersistenceContext
	private EntityManager entityManager;
	
    public void persist(UserFavorites transientInstance) {

        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            tx.begin();

            entityManager.persist(transientInstance);

            tx.commit();
        }
        catch (RuntimeException e) {
            if ( tx != null && tx.isActive() ) tx.rollback();
            throw e; // or display error message
        }
    }

    public void remove(UserFavorites persistentInstance) {

        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            tx.begin();

            entityManager.remove(persistentInstance);

            tx.commit();
        }
        catch (RuntimeException e) {
            if ( tx != null && tx.isActive() ) tx.rollback();
            throw e; // or display error message
        }
    }

    public List<String> getUserFavorites(User user) {

        try {
            Query query = entityManager.createQuery("SELECT uf.favorite from UserFavorites uf WHERE uf.user_id = :userId");
            query.setParameter("userId", user.getId());
            List<String> userFavorites = (List<String>)query.getResultList();
            return userFavorites;
        } catch (Exception ex) {
            Logger.error("UserFavoritesDAO :: getUserFavorites : could not get favorites for user - " + user.getId() + " => " + ex,ex);
            return null;
        }
    }
}
