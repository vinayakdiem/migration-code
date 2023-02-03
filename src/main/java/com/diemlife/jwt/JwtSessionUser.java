package com.diemlife.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import play.libs.Json;
import play.mvc.Http;
import com.diemlife.security.UsernamePasswordAuth;

import java.io.Serializable;
import java.util.Date;

import static com.diemlife.jwt.JwtUtils.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class JwtSessionUser implements Serializable {

    private static final String ANONYMOUS_EMAIL = "anonymous@diem.life";
    private static final String SESSION_USER_ID_KEY = "sessionUserId";
    private static final String SESSION_USER_EMAIL_KEY = "sessionUserEmail";
    private static final String SESSION_LOGIN_EXPIRES_KEY = "sessionLoginExpires";
    private static final String SESSION_USER_PROVIDER = "sessionUserProvider";
    private static final String SESSION_USER_TOKEN_HASH = "sessionUserTokenHash";

    private static final JwtSessionUser ANONYMOUS = new JwtSessionUser(0, ANONYMOUS_EMAIL, UsernamePasswordAuth.PROVIDER_KEY, new Date(0));
    public static final String JWT_USER = "Jwt-User";
    public static final String JWT_HASH = "Jwt-Hash";

    public final Integer userId;
    public final String userEmail;
    public final Date loginExpires;
    public final String userProvider;

    @JsonCreator
    private JwtSessionUser(final @JsonProperty("userId") Integer userId,
                           final @JsonProperty("userEmail") String userEmail,
                           final @JsonProperty("userProvider") String userProvider,
                           final @JsonProperty("loginExpires") Date loginExpires) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.loginExpires = loginExpires;
        this.userProvider = userProvider;
    }

    public JwtSessionUser(final DecodedJWT jwt) {
        this(jwt.getClaim(CLAIM_KEY_USER_ID).asInt(), jwt.getClaim(CLAIM_KEY_USER_EMAIL).asString(), jwt.getClaim(CLAIM_KEY_USER_PROVIDER).asString(), jwt.getExpiresAt());
    }

    public void storeInSession(final Http.Session session, final String tokenHash) {
        if (session == null) {
            throw new IllegalStateException("Cannot store JWT session user - no HTTP session bound to thread !");
        }
        session.put(SESSION_USER_ID_KEY, Integer.toString(userId));
        session.put(SESSION_USER_EMAIL_KEY, userEmail);
        session.put(SESSION_LOGIN_EXPIRES_KEY, Long.toString(loginExpires.getTime()));
        session.put(SESSION_USER_PROVIDER, userProvider);
        session.put(SESSION_USER_TOKEN_HASH, trimToNull(tokenHash));
    }

    public static String userEmailFromSession(final Http.Session session) {
        if (session == null) {
            throw new IllegalStateException("Cannot get JWT session user Email - no HTTP session bound to thread !");
        }
        final String email = trimToNull(session.get(SESSION_USER_EMAIL_KEY));
        if (isBlank(email) || ANONYMOUS_EMAIL.equalsIgnoreCase(email)) {
            return null;
        } else {
            return email;
        }
    }

    public static String userProviderFromSession(final Http.Session session) {
        if (session == null) {
            throw new IllegalStateException("Cannot get JWT session user provider - no HTTP session bound to thread !");
        }
        return trimToNull(session.get(SESSION_USER_PROVIDER));
    }

    public static String userTokenHashFromSession(final Http.Session session) {
        if (session == null) {
            throw new IllegalStateException("Cannot get JWT hash - no HTTP session bound to thread !");
        }
        return trimToNull(session.get(SESSION_USER_TOKEN_HASH));
    }

    public static String clearInSession(final Http.Session session) {
        session.remove(SESSION_USER_ID_KEY);
        session.remove(SESSION_LOGIN_EXPIRES_KEY);
        session.remove(SESSION_USER_PROVIDER);
        session.remove(SESSION_USER_TOKEN_HASH);
        return session.remove(SESSION_USER_EMAIL_KEY);
    }

    public static JwtSessionUser anonymous() {
        return ANONYMOUS;
    }

    public boolean isAnonymous() {
        return ANONYMOUS_EMAIL.equalsIgnoreCase(userEmail) && Integer.valueOf(0).equals(userId);
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

}
