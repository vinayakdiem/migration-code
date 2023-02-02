package com.diemlife.dto;

public class SubscriptionCreationDTO extends StripeDTO {

    private static final String BILLING_TYPE_CHARGE_AUTOMATICALLY = "charge_automatically";

    public String customer;
    public String plan;
    public Long quantity;
    public Double applicationFeePercent;
    public String collection_method = BILLING_TYPE_CHARGE_AUTOMATICALLY;
    public Long billingCycleAnchor;
    public Boolean prorate;

}
