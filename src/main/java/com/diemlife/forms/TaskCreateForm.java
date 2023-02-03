package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskCreateForm extends MilestoneForm {

    /**
     * Order in group
     */
    private Integer order;

    /**
     * Index of task group
     */
    private Integer groupIndex;

    public TaskCreateForm(final String task) {
        super(task);
    }

}
