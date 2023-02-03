package com.diemlife.forms;

import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class PayoutForm implements Serializable {
    @Required
    private Long amount;
    @Required
    private String currency;

    public PayoutForm() {
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(final Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }
}
