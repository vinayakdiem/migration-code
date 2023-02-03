package com.diemlife.forms;

import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class BankAccountVerificationForm implements Serializable {
    @Required
    @Min(1)
    private Integer firstDebit;
    @Required
    @Min(1)
    private Integer secondDebit;

    public Integer getFirstDebit() {
        return firstDebit;
    }

    public void setFirstDebit(final Integer firstDebit) {
        this.firstDebit = firstDebit;
    }

    public Integer getSecondDebit() {
        return secondDebit;
    }

    public void setSecondDebit(final Integer secondDebit) {
        this.secondDebit = secondDebit;
    }
}
