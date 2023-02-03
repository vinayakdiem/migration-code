package com.diemlife.filters;

import akka.stream.Materializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jwt.JwtSessionUser;
import play.Logger;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;
import com.diemlife.providers.RedisAuthProvider;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static jwt.JwtSessionUser.JWT_HASH;
import static jwt.JwtSessionUser.JWT_USER;
import static jwt.JwtUtils.extractToken;
import static org.apache.commons.lang.StringUtils.isBlank;
import static play.libs.Json.toJson;

public class JwtFilter extends Filter {

    private final RedisAuthProvider redisAuthProvider;

    private final ObjectMapper objectMapper;

    @Inject
    public JwtFilter(final Materializer materializer,
                     final RedisAuthProvider redisAuthProvider) {
        super(materializer);
        this.redisAuthProvider = redisAuthProvider;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CompletionStage<Result> apply(final Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
                                         final Http.RequestHeader requestHeader) {
        final String token = extractToken(requestHeader);

        final String userJson = redisAuthProvider.get(token);
        if (isBlank(userJson)) {
            return nextFilter.apply(withJwtSessionUser(requestHeader, JwtSessionUser.anonymous(), token));
        }
        final JwtSessionUser jwtSessionUser = getJwtSessionUserByJson(userJson);
        if (jwtSessionUser != null) {
            return nextFilter.apply(withJwtSessionUser(requestHeader, jwtSessionUser, token));
        } else {
            throw new IllegalStateException("JWT session user is null");
        }
    }

    private static Http.RequestHeader withJwtSessionUser(final Http.RequestHeader requestHeader, final JwtSessionUser user, final String token) {
        if (user == null) {
            return requestHeader;
        } else {
            return new Http.RequestImpl(requestHeader._underlyingHeader()
                    .withTag(JWT_HASH, token)
                    .withTag(JWT_USER, toJson(user).toString()));
        }
    }

    private JwtSessionUser getJwtSessionUserByJson(final String userJson) {
        try {
            return objectMapper.readValue(userJson, JwtSessionUser.class);
        } catch (final IOException exception) {
            Logger.error("Failed to read JwtSessionUser, json: " + userJson);
        }
        return null;
    }

}
