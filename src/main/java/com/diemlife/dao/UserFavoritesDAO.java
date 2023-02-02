package com.diemlife.dao;

import com.diemlife.models.User;
import com.diemlife.models.UserFavorites;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by andrew on 5/8/17.
 */
public class UserFavoritesDAO {

    public void persist(UserFavorites transientInstance, EntityManager entityManager) {

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

    public void remove(UserFavorites persistentInstance, EntityManager entityManager) {

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

    public static List<String> getUserFavorites(User user, EntityManager em) {

        try {
            Query query = em.createQuery("SELECT uf.favorite from UserFavorites uf WHERE uf.user_id = :userId");
            query.setParameter("userId", user.getId());
            List<String> userFavorites = (List<String>)query.getResultList();
            return userFavorites;
        } catch (Exception ex) {
            Logger.error("UserFavoritesDAO :: getUserFavorites : could not get favorites for user - " + user.getId() + " => " + ex,ex);
            return null;
        }
    }
}
