package com.diemlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StripeAccountCreationDTO extends StripeAccountBaseDTO {

    public StripeAccountType type = StripeAccountType.custom;
    public String country;

    public enum StripeAccountType {
        custom
    }

}
