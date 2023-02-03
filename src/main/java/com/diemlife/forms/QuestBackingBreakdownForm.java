package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class QuestBackingBreakdownForm implements Serializable {

    @Required
    private Long amount;
    @Required
    private String paymentMode;
    private Boolean backerAbsorbsFees;
    private Double tip;
    private Integer brandUserId;

}
