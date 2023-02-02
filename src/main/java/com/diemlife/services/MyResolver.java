package com.diemlife.services;

/**
 * Created by andrewcoleman on 4/15/17.
 */

import com.feth.play.module.pa.Resolver;
import controllers.routes;
import play.mvc.Call;

/**
 * Concrete resolver implementation.
 */
public class MyResolver extends Resolver {
    @Override
    public Call login() {
        // Your login page
        return routes.Signup.toLogin();
    }

    @Override
    public Call afterAuth() {
        // The user will be redirected to this page after authentication
        // if no original URL was saved
        return routes.Application.index();
    }

    @Override
    public Call afterLogout() {
        return routes.Application.index();
    }

    @Override
    public Call auth(final String provider) {
        // You can provide your own authentication implementation,
        // however the default should be sufficient for most cases
        return com.feth.play.module.pa.controllers.routes.Authenticate.authenticate(provider);
    }

    @Override
    public Call askMerge() {
        return routes.Application.index();
    }

    @Override
    public Call askLink() {
        return routes.Application.index();
    }

}
