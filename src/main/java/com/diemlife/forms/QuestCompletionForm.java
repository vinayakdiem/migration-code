package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class QuestCompletionForm implements Serializable {

    @Required
    private Integer questId;

    private Boolean completeMilestones;

    private QuestActionPointForm point;

}
