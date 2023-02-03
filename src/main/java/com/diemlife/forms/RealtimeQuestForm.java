package com.diemlife.forms;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class RealtimeQuestForm {

    private String image;

    private String comment;

    private List<AttributeValueForm> attributes;

    private boolean completeMilestones;

    private QuestActionPointForm coordinates;

}
