package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class FundraisingForm extends FundraisingConfigForm {

    @Required
    private Integer doerId;
    @Required
    private Integer questId;

    private Integer brandUserId;
    private Integer secondaryBrandUserId;

}
