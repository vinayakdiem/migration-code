package com.diemlife.forms;

import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class ExternalAccountForm implements Serializable {
    @Required
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }
}
