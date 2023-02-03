package com.diemlife.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class AddressForm extends CountryZipForm {

    @Required
    private String streetNo;
    private String streetNoAdditional;
    @Required
    private String city;
    @Required
    @MinLength(2)
    @MaxLength(2)
    private String state;

}
