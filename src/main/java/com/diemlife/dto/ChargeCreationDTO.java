package com.diemlife.dto;

public class ChargeCreationDTO extends StripeDTO {
    public Long amount;
    public String currency;
    public String customer;
    public String source;
    public String description;
    public String statementDescriptor;
    public String statementDescriptorSuffix;
    public String transferGroup;
    public StripeChargeDestinationDTO transferData;
}
