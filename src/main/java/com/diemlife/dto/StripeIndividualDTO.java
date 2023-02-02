package com.diemlife.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class StripeIndividualDTO extends StripeDTO {

    public String firstName;
    public String lastName;
    public Gender gender;
    public String email;
    public String phone;
    public StripeAddressDTO address;
    public DateOfBirthDTO dob;
    public String idNumber;
    @JsonProperty("ssn_last_4")
    public String ssnLast4;

    @Getter
    @Setter
    @NoArgsConstructor
    public static final class DateOfBirthDTO implements Serializable {
        public int day;
        public int month;
        public int year;
    }

    public enum Gender {
        male, female
    }
}
