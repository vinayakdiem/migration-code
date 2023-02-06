package com.diemlife.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Repository;

import com.diemlife.models.ExplorePlaces;

import play.Logger;

@Repository
public class ExplorePlacesDAO {

	@PersistenceContext
	EntityManager em;
	
    public boolean doesPlaceExist(String place, EntityManager em) {
        try {
            Long explorePlace = em.createQuery("SELECT COUNT (ep) FROM ExplorePlaces ep WHERE ep.place=:place", Long.class)
                    .setParameter("place", place)
                    .getSingleResult();
            return explorePlace > 0;
        } catch (PersistenceException | IllegalArgumentException e) {
            Logger.info("Cannot insert value in ExplorePlaces", e);
        }
        return false;
    }

    public List<ExplorePlaces> findAllExplorePlaces(EntityManager em) {
        List<ExplorePlaces> explorePlaces = null;
        try {
            explorePlaces = em.createQuery("SELECT ep FROM ExplorePlaces ep WHERE ep.included = true ORDER BY ep.order", ExplorePlaces.class)
                    .getResultList();
        } catch (PersistenceException | IllegalArgumentException e) {
            Logger.info("ExplorePlacesDAO :: findAllExplorePlaces() :: Cannot get values from ExplorePlaces");
        }
        return explorePlaces;
    }
}
