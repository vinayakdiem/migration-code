package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class CommentAddForm implements Serializable {

    @Required
    private Integer questId;
    @Required
    private String comments;

    private Integer inReplyToId;
	private Integer imageId;
}
