package com.diemlife.forms;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;

import java.io.Serializable;
import java.util.List;

public class ShareQuestForm implements Serializable {

    @Required
    private Integer questId;
    @Email
    private String email;
    private List<String> emails;
    private String message;

    public ShareQuestForm() {
    }

    public Integer getQuestId() {
        return questId;
    }

    public void setQuestId(final Integer questId) {
        this.questId = questId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(final List<String> emails) {
        this.emails = emails;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

}
