package com.diemlife.forms;

import com.diemlife.models.UserEmailPersonal;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;

public class PersonalInfoForm implements UserEmailPersonal {

    @Required
    private String firstName;
    @Required
    private String lastName;
    @Required
    @Email
    private String email;

    public PersonalInfoForm() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

}
