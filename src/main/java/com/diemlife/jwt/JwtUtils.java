package com.diemlife.jwt;

import play.mvc.Http;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public abstract class JwtUtils {

    public static final String CLAIM_KEY_USER_ID = "userId";
    public static final String CLAIM_KEY_USER_EMAIL = "userEmail";
    public static final String CLAIM_KEY_USER_PROVIDER = "userProvider";

    private static final String BEARER_PREFIX = "Bearer ";

    private JwtUtils() {
        super();
    }

    public static String extractToken(final Http.RequestHeader requestHeaders) {
        final String authHeader = requestHeaders.getHeader(Http.HeaderNames.AUTHORIZATION);
        if (isNotBlank(authHeader) && startsWith(authHeader, BEARER_PREFIX)) {
            return trimToEmpty(removeStart(authHeader, BEARER_PREFIX));
        } else {
            return EMPTY;
        }
    }

}
