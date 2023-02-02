package com.diemlife.dao;

import exceptions.RequiredParameterMissingException;
import models.User;
import models.UserActivationPinCode;

import javax.persistence.EntityManager;
import java.util.Calendar;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class UserActivationPinCodeDAO extends TypedDAO<UserActivationPinCode> {

    public UserActivationPinCodeDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public UserActivationPinCode getUserActivationPinCode(final User user, final String code) {
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
        if (isBlank(code)) {
            throw new RequiredParameterMissingException("code");
        }
        return entityManager
                .createQuery("SELECT uac FROM UserActivationCodes uac WHERE uac.user.id = :userId AND pinCode = :code", UserActivationPinCode.class)
                .setParameter("userId", user.getId())
                .setParameter("code", code)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public UserActivationPinCode consume(final UserActivationPinCode activationCode) {
        if (activationCode == null) {
            throw new RequiredParameterMissingException("activationCode");
        }
        activationCode.consumed = true;
        activationCode.consumedOn = Calendar.getInstance().getTime();
        return entityManager.merge(activationCode);
    }

}
