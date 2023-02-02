package com.diemlife.dao;

import com.diemlife.models.UserPaymentFee;
import play.db.jpa.JPAApi;


/**
 * Dao from user_payment_fee
 * Created 23/11/2020
 *
 * @author SYushchenko
 */
public class UserPaymentFeeDAO {

    private final JPAApi jpaApi;

    /**
     * Constructor with parameters
     *
     * @param jpaApi {@link JPAApi}
     */
    public UserPaymentFeeDAO(final JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    /**
     * Find {@link UserPaymentFee} by user id
     * @param userId user id
     * @return {@link UserPaymentFee}
     */
    public UserPaymentFee getUserPaymentFeeByUserId(final long userId) {
        return jpaApi.em().find(UserPaymentFee.class, userId);
    }
}
