package com.diemlife.dto;

import lombok.Data;

@Data
public class AddressDTO {
    public String country;
    public String lineOne;
    public String lineTwo;
    public String city;
    public String state;
    public String zip;
}
