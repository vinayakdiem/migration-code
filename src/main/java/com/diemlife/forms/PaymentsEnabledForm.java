package com.diemlife.forms;

import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class PaymentsEnabledForm implements Serializable {

    @Required
    private String paymentMode;
    private String lastFour;
    private String token;
    @Required
    private boolean save = false;

    public PaymentsEnabledForm() {
        super();
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(final String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getLastFour() {
        return lastFour;
    }

    public void setLastFour(final String lastFour) {
        this.lastFour = lastFour;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public boolean isSave() {
        return save;
    }

    public void setSave(final boolean save) {
        this.save = save;
    }

}
