package com.diemlife.dto;

public class ChargeListRequestDTO extends CollectionRequestDTO {

    public String transferGroup;

    public ChargeListRequestDTO(final String transferGroup, final String... expand) {
        super(expand);
        this.transferGroup = transferGroup;
    }

}
