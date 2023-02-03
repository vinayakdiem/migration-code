package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class LeaderboardScoreForm implements Serializable {

    @Required
    private Long memberId;

    @Required
    private Integer value;

    private String status;

}
