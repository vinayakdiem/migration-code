package com.diemlife.dto;

import com.diemlife.models.UserEmailPersonal;

import lombok.Data;

@Data
public class EmailPersonalDTO implements UserEmailPersonal {

    private final String firstName;
    private final String lastName;
    private final String email;

}
