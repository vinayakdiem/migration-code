package com.diemlife.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.math.BigInteger;

@Data
public class BrandDTO implements Serializable {
    private AddressDTO address;
    private String name;
    private String orgType;
    private String website;
    //ToDo: Add phone number
    private String phone;
}
