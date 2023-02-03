package com.diemlife.forms;

import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class OrderBreakdownForm implements Serializable {

    private String paymentMode;
    private String couponCode;
    @Required
    private OrderItemForm[] orderItems;

    public OrderBreakdownForm() {
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(final String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(final String couponCode) {
        this.couponCode = couponCode;
    }

    public OrderItemForm[] getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(final OrderItemForm[] orderItems) {
        this.orderItems = orderItems;
    }

}
