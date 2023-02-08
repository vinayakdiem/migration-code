package com.diemlife.services;

import com.diemlife.dao.UserPaymentFeeDAO;
import com.diemlife.models.User;
import com.diemlife.models.UserPaymentFee;
import com.typesafe.config.Config;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service user payment fee service
 * Created 23/11/2020
 *
 * @author SYushchenko
 */
@Service
public class UserPaymentFeeService {

    private static final String IS_USER_BRAND = "Y";
    
    @Autowired
    private UserPaymentFeeDAO userPaymentFeeDAO;
    
    @Autowired
    private Config config;

    public double getFeeByUser(final User user) {
        if (user == null) {
            return config.getDouble("application.fee");
        }
        UserPaymentFee userPaymentFee = userPaymentFeeDAO.getUserPaymentFeeByUserId(user.getId());
        if (userPaymentFee != null) {
            return userPaymentFee.getFee();
        }
        return getFeeWhenUserPaymentFeeTableEmpty(user);
    }

    private double getFeeWhenUserPaymentFeeTableEmpty(final User user) {
        if (IS_USER_BRAND.equals(user.getIsUserBrand())) {
            return config.getDouble("application.brandFee");
        }
        return config.getDouble("application.fee");
    }
}
