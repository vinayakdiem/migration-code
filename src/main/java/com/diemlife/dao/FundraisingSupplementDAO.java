package com.diemlife.dao;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Repository;

import com.diemlife.models.FundraisingSupplement;

import play.Logger;

@Repository
public class FundraisingSupplementDAO {
	
	@PersistenceContext
	private EntityManager entityManager;

    public List<FundraisingSupplement> getFundraisingSupplement(final Integer questId, final Integer userId) {
        if (entityManager == null) {
            return emptyList();
        }
        try {
            return entityManager.createQuery("SELECT f FROM FundraisingSupplement f WHERE f.questId = :questId AND f.userId = :userId", FundraisingSupplement.class)
                    .setParameter("questId", questId)
                    .setParameter("userId", userId)
                    .getResultList();
        } catch (final PersistenceException e) {
            Logger.error(format("error getting fundraising supplement for quest [%s] and user [%s]", questId, userId));
        }
        return emptyList();
    }
}
