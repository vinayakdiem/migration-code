package com.diemlife.dao;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Repository;

import com.diemlife.models.IdentifiedEntity;

import play.Logger;

@Repository
public abstract class TypedDAO<T extends IdentifiedEntity> {

	@PersistenceContext
	EntityManager entityManager;

    public <ST extends T> ST save(final ST entity, final Class<ST> type) {
        if (entity.getId() == null) {
            try {
                entityManager.persist(entity);
                entityManager.flush();
            } catch (final PersistenceException | IllegalArgumentException e) {
                Logger.error("Unable to persist " + type.getSimpleName(), e);
                throw e;
            }
            return entityManager.find(type, entity.getId());
        } else {
            return entityManager.merge(entity);
        }
    }

    public <ST extends T> void saveAll(final Collection<ST> entities) {
        final AtomicInteger index = new AtomicInteger(1);
        entities.forEach(l -> {
            if (index.getAndIncrement() % 1000 == 0) {
                entityManager.flush();
            }
            if (l.getId() == null) {
                entityManager.persist(l);
            } else {
                entityManager.merge(l);
            }
        });
    }

    public <ST extends T> ST load(final Long id, final Class<ST> type) {
        return entityManager.find(type, id);
    }

    public <ST extends T> void delete(final ST entity) {
        entityManager.remove(entity);
    }
}
