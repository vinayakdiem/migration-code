package com.diemlife.utils;

import com.diemlife.dao.PaymentTransactionDAO;
import com.diemlife.dao.UserActivationPinCodeDAO;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

@Singleton
public class DAOProvider {

    public UserActivationPinCodeDAO userActivationPinCodeDAO(final EntityManager em) {
        return new UserActivationPinCodeDAO(em);
    }

    public PaymentTransactionDAO paymentTransactionDAO(final EntityManager em) {
        return new PaymentTransactionDAO(em);
    }

}
