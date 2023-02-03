package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TasksGroupForm {

    private Integer groupId;

    private String groupName;

    private List<Integer> groupTasks;

}
