package com.diemlife.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import play.data.validation.Constraints.Min;

import java.io.Serializable;

import static play.data.validation.Constraints.MaxLength;
import static play.data.validation.Constraints.MinLength;
import static play.data.validation.Constraints.Required;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class CountryZipForm implements Serializable {

    @Required
    @MinLength(2)
    @MaxLength(2)
    private String country;

    @Required
    @Min(0)
    private String zip;

}
