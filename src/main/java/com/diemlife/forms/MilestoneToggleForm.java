package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class MilestoneToggleForm implements Serializable {

    @Required
    private Integer taskId;

    private QuestActionPointForm point;

}
