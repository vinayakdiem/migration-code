package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class QuestBackingForm extends PaymentsEnabledForm {

    @Required
    @Min(1)
    private Long amount;
    @Required
    private String currency;
    private Boolean recurrent;
    private String message;
    private Boolean fundraising;
    private Boolean anonymous;
    private Boolean absorbFees;
    private Boolean payNow;
    private BackerInfoForm billingInfo;
    private Boolean signUp;
    private Boolean mailing;
    private Double tip;
    private Integer brandUserId;

    private String backerDisplayFirstName;
    private String backerDisplayLastName;
    private String backerDisplayComment;

}
