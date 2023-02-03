package com.diemlife.forms;

import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class TasksGroupRenameForm implements Serializable {

    @Required
    private Integer groupId;
    @Required
    private String groupName;

}
