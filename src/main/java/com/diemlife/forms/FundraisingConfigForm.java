package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.MaxLength;

import java.io.Serializable;

import static play.data.validation.Constraints.Min;
import static play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class FundraisingConfigForm implements Serializable {

    @Required
    @Min(1)
    private Long targetAmount;
    @Required
    @MaxLength(15)
    private String currency;
    @MaxLength(255)
    private String campaignName;
    @MaxLength(2047)
    private String coverImageUrl;

}
