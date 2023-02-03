package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Required;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class QuestCreateForm extends QuestForm {

    /**
     * One of {@link constants.QuestMode} keys
     */
    @Required
    @MaxLength(250)
    private String mode;

    private List<TasksGroupManageForm> tasksGroups;

    private List<TaskCreateForm> questTasks;

    @MaxLength(250)
    private String questTasksGroupName;

}
