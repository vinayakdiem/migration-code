package com.diemlife.dto;

import java.io.Serializable;

public class WhoIAmDTO implements Serializable {
    public Integer id;
    public String email;

    public WhoIAmDTO(final Integer id, final String email) {
        this.id = id;
        this.email = email;
    }
}
