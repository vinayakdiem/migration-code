package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class QuestTeamInfoForm implements Serializable {

    private Long questTeamId;

    private String questTeamName;

    private String questTeamLogoUrl;

    private String questTeamCoverUrl;

    private TeamAction questTeamAction;

    public enum TeamAction {
        Create, Join, Update
    }

}
