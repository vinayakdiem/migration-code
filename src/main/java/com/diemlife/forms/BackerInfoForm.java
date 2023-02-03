package com.diemlife.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class BackerInfoForm implements Serializable {

    @Required
    private PersonalInfoForm personalInfo;
    @Required
    private CountryZipForm address;

}
