package com.diemlife.dto;

public class StripePayoutDTO extends StripeDTO {
    public Long amount;
    public String currency;
    public String statementDescriptor;
    public String method;
    public String sourceType;
    public String destination;
}
