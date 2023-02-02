package com.diemlife.dto;

public class PaymentSourceListRequestDTO extends CollectionRequestDTO {

    public PaymentSourceListRequestDTO(final String object, final String... expand) {
        super(expand);
        this.object = object;
    }

}
