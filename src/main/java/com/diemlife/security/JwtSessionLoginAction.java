package com.diemlife.security;

import jwt.JwtSessionUser;
import play.Logger;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static jwt.JwtSessionUser.JWT_HASH;
import static jwt.JwtSessionUser.JWT_USER;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class JwtSessionLoginAction extends Action<JwtSessionLogin> {

    @Override
    public CompletionStage<Result> call(final Http.Context context) {
        final String userJson = context.request().tags().get(JWT_USER);
        final String tokenHash = context.request().tags().get(JWT_HASH);
        final JwtSessionUser user = isBlank(userJson) ? null : Json.fromJson(Json.parse(userJson), JwtSessionUser.class);
        if (configuration.required()) {
            Logger.debug("JWT authentication required for request [" + context.request().path() + "]");

            if (user == null) {
                Logger.debug("Rejecting call for missing JWT for request [" + context.request().path() + "]");

                return completedFuture(unauthorized());
            } else if (user.isAnonymous()) {
                Logger.debug("Rejecting call for anonymous JWT for request [" + context.request().path() + "]");

                return completedFuture(forbidden());
            } else {
                user.storeInSession(context.session(), tokenHash);

                return delegate.call(context);
            }
        } else if (configuration.skip()) {
            Logger.debug("Skipping JWT authentication required for request [" + context.request().path() + "]");

            return delegate.call(context);
        } else {
            if (user != null) {
                user.storeInSession(context.session(), tokenHash);
            }

            return delegate.call(context);
        }
    }

}
