package com.diemlife.services;

import play.db.jpa.JPAApi;
import com.diemlife.services.StripeConnectService;

public interface StripeAccountManager {

    StripeConnectService stripeConnectService();

    JPAApi jpaApi();

}
