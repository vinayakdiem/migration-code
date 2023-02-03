package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class TasksGroupManageEditForm implements Serializable {

    private Integer id;
	private String groupName;
    private Integer groupOrder;
    private Integer groupOwnerId;
}
