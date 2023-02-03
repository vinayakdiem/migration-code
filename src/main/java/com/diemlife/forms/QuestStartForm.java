package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class QuestStartForm extends QuestTeamInfoForm {

    @Required
    private Integer questId;

    private Integer referrerId;

    private String questMode;

    private QuestActionPointForm point;

}
