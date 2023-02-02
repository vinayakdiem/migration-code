package com.diemlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StripeAddressDTO extends StripeDTO {
    public String line1;
    public String line2;
    public String city;
    public String state;
    public String country;
    public String postalCode;
}
