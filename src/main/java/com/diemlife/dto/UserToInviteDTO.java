package com.diemlife.dto;

import java.io.Serializable;

public class UserToInviteDTO implements Serializable {

    public final String email;
    public final String name;

    public UserToInviteDTO(final String email, final String name) {
        this.email = email;
        this.name = name;
    }
}
