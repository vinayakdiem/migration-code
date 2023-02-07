package com.diemlife.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.UserPaymentFee;



/**
 * Dao from user_payment_fee
 * Created 23/11/2020
 *
 * @author SYushchenko
 */

@Repository
public class UserPaymentFeeDAO {

  
	@PersistenceContext
	private EntityManager em;
	
    /**
     * Find {@link UserPaymentFee} by user id
     * @param userId user id
     * @return {@link UserPaymentFee}
     */
    public UserPaymentFee getUserPaymentFeeByUserId(final long userId) {
        return em.find(UserPaymentFee.class, userId);
    }
}
