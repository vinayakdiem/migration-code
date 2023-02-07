package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.User;
import com.diemlife.models.UserActivationPinCode;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import java.util.Calendar;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Repository
public class UserActivationPinCodeDAO extends TypedDAO<UserActivationPinCode> {

	@PersistenceContext
	private EntityManager entityManager;

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
