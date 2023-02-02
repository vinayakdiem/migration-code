package com.diemlife.security;

import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class UsernamePasswordAuth implements Serializable {

    public static final String PROVIDER_KEY = "password";

    private final PasswordHasher hasher;

    private final String email;
    private final String password;

    public UsernamePasswordAuth(final String email, final String password) {
        this.email = email;
        this.password = password;
        this.hasher = new PasswordHasher(password);
    }

    public String getEmail() {
        return email;
    }

    public boolean checkPassword(final String hashed) {
        if (isBlank(hashed)) {
            return false;
        }

        final String[] hashSalt = hashed.split("#");
        final String storedSha256hex = hashSalt[0];
        final String storedSalt = hashSalt[1];

        return hasher.checkPassword(storedSha256hex, storedSalt, password);
    }

}
