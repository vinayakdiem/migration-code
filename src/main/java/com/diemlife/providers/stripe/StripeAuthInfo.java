package com.diemlife.providers.stripe;

import com.feth.play.module.pa.providers.oauth2.OAuth2AuthInfo;

/**
 * Created by andrewcoleman on 11/29/16.
 * Handles authorization for Stripe
 */
public class StripeAuthInfo extends OAuth2AuthInfo {

    //The token is the access_token provided by stripe api.
    public StripeAuthInfo(String token) {
        super(token);
    }
}
