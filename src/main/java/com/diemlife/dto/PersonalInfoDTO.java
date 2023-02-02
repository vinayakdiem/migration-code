package com.diemlife.dto;

import lombok.Data;

@Data
public class PersonalInfoDTO {

    private final String address;
    private final String last4;
    private final String dob;
    private final String personalId;
    private final String phone;
    private final String url;
}
