package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

import static play.data.validation.Constraints.Required;

@Getter
@Setter
@NoArgsConstructor
public class TaskMoveForm implements Serializable {

    @Required
    private Integer taskId;

    @Required
    private Integer groupId;

    private Integer taskOrder;

}
