package com.diemlife.dto;

public class StripeOrderDTO extends StripeDTO {
    public String currency;
    public String email;
    public StripeOrderItemDTO[] items;
    public StripeShippingDTO shipping;
}
