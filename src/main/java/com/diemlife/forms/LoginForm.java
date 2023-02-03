package com.diemlife.forms;

import org.hibernate.validator.constraints.Email;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class LoginForm implements Serializable {

    @Required
    @Email
    private String email;
    @Required
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

}
