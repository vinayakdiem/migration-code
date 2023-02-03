package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.StripeCustomer;
import com.diemlife.models.StripeEntity;
import play.Logger;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class StripeCustomerDAO extends TypedDAO<StripeEntity> {

    public StripeCustomerDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public <T extends StripeEntity> T getByUserId(final Integer userId, final Class<T> type) {
        if (userId == null) {
            throw new RequiredParameterMissingException("userId");
        }
        if (type == null) {
            throw new RequiredParameterMissingException("type");
        }
        try {
            return entityManager
                    .createQuery("SELECT su FROM " + type.getAnnotation(Entity.class).name() + " su JOIN su.user u WHERE u.id = :userId", type)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } catch (final NoResultException e) {
            Logger.warn(format("StripeCustomerDAO::getByUserId : No StripeEntity found for userId %s and type '%s'", userId, type));
            return null;
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("StripeCustomerDAO::getByUserId : Unable to retrieve StripeCustomer for userId %s", userId), e);
            throw e;
        }
    }

    public <T extends StripeEntity> T changeStripeEntityType(final Long id, final Class<T> type) {
        if (id == null) {
            throw new RequiredParameterMissingException("userId");
        }
        if (type == null) {
            throw new RequiredParameterMissingException("type");
        }
        entityManager.createNamedQuery("StripeEntity.changeUserType")
                .setParameter("type", type.getAnnotation(DiscriminatorValue.class).value())
                .setParameter("id", id)
                .executeUpdate();
        entityManager.flush();
        entityManager.detach(entityManager.find(StripeEntity.class, id));

        return entityManager.find(type, id);
    }

    public StripeCustomer loadStripeCustomer(final String stripeCustomerId) {
        if (isBlank(stripeCustomerId)) {
            return null;
        }
        return entityManager.createQuery("SELECT sc FROM StripeCustomers sc WHERE sc.stripeCustomerId = :id", StripeCustomer.class)
                .setParameter("id", stripeCustomerId)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

}
