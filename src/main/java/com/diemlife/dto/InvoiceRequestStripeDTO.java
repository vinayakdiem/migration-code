package com.diemlife.dto;

public class InvoiceRequestStripeDTO extends CollectionRequestDTO {

    public final String subscription;

    public InvoiceRequestStripeDTO(final String subscription, final String... expand) {
        super(expand);
        this.subscription = subscription;
    }

}
