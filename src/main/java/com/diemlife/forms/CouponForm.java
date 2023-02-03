package com.diemlife.forms;

import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class CouponForm implements Serializable {
    @Required
    private String couponCode;

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(final String couponCode) {
        this.couponCode = couponCode;
    }
}
