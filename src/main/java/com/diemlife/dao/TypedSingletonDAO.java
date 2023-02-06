package com.diemlife.dao;

import com.diemlife.models.IdentifiedEntity;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Repository;

@Repository
public class TypedSingletonDAO<T extends IdentifiedEntity> {

	@PersistenceContext
	private EntityManager em;

    public <ST extends T> ST save(final ST entity, final Class<ST> type) {
        if (entity.getId() == null) {
            try {
                em.persist(entity);
                em.flush();
            } catch (final PersistenceException | IllegalArgumentException e) {
                Logger.error("Unable to persist " + type.getSimpleName(), e);
                throw e;
            }
            return em.find(type, entity.getId());
        } else {
            return em.merge(entity);
        }
    }

    public <ST extends T> ST load(final Long id, final Class<ST> type) {
        return em.find(type, id);
    }

    public <ST extends T> void delete(final ST entity) {
        em.remove(entity);
    }

}
