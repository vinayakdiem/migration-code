package com.diemlife.forms;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class QuestEditForm extends QuestForm {

    @Required
    private Integer questId;
    private String stayInOldQuest;
    private List<TasksGroupManageEditForm> tasksGroups;
    private List<TaskEditForm> questTasks;
}
