package com.diemlife.forms;

import lombok.Data;
import lombok.NoArgsConstructor;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class MessageAllForm implements Serializable {

    @MaxLength(250)
    private String subject;

    @Required
    @MaxLength(4000)
    private String message;

    @Required
    private boolean plain = false;
    
    private String group;

}
