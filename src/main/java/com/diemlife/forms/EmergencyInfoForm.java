package com.diemlife.forms;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;

public class EmergencyInfoForm {

    @Required
    private String name;
    @Required
    private String number;
    @Email
    private String email;

    public EmergencyInfoForm() {
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(final String number) {
        this.number = number;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

}
