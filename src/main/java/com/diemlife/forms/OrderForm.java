package com.diemlife.forms;

import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class OrderForm implements Serializable {

    private String password;
    @Required
    private OrderItemForm[] orderItems;
    private String couponCode;
    private Long referredQuestToBack;
    @Required
    private OrderBillingForm billingInfo;
    @Required
    private OrderParticipantForm[] participants;
    private Boolean signUp;
    private Double tip;
    private Boolean checkPassword;

    public OrderForm() {
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public OrderItemForm[] getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(final OrderItemForm[] orderItems) {
        this.orderItems = orderItems;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(final String couponCode) {
        this.couponCode = couponCode;
    }

    public Long getReferredQuestToBack() {
        return referredQuestToBack;
    }

    public void setReferredQuestToBack(final Long referredQuestToBack) {
        this.referredQuestToBack = referredQuestToBack;
    }

    public OrderBillingForm getBillingInfo() {
        return billingInfo;
    }

    public void setBillingInfo(final OrderBillingForm billingInfo) {
        this.billingInfo = billingInfo;
    }

    public OrderParticipantForm[] getParticipants() {
        return participants;
    }

    public void setParticipants(final OrderParticipantForm[] participants) {
        this.participants = participants;
    }

    public Boolean getSignUp() {
        return signUp;
    }

    public void setSignUp(final Boolean signUp) {
        this.signUp = signUp;
    }

    public Double getTip() {
        return tip;
    }

    public void setTip(Double tip) {
        this.tip = tip;
    }

    public Boolean getCheckPassword() {
        return checkPassword;
    }

    public void setCheckPassword(final Boolean checkPassword) {
        this.checkPassword = checkPassword;
    }    
}
