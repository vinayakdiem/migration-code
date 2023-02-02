package com.diemlife.dao;

import com.diemlife.models.IdentifiedEntity;
import play.Logger;
import play.db.jpa.JPAApi;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

public abstract class TypedSingletonDAO<T extends IdentifiedEntity> {

    protected final JPAApi jpaApi;

    protected TypedSingletonDAO(final JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public <ST extends T> ST save(final ST entity, final Class<ST> type) {
        final EntityManager em = jpaApi.em();
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
        final EntityManager em = jpaApi.em();
        return em.find(type, id);
    }

    public <ST extends T> void delete(final ST entity) {
        final EntityManager em = jpaApi.em();
        em.remove(entity);
    }

}
