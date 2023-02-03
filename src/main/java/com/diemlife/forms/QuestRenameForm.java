package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class QuestRenameForm {

    @Required
    private String questName;

}
