package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Required;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public abstract class MilestoneForm implements Serializable {

    @Required
    @MaxLength(500)
    private String task;
    private EmbeddedVideoForm video;
    private Integer linkedQuestId;
    private QuestActionPointForm point;
    private Float radiusInKm;
    private Integer groupId;
    private String imageUrl;
    private String linkUrl;
    private Integer questOwnerId;
    private List<Integer> activitiesIds;
    private String title;

    public MilestoneForm(final String task) {
        this.task = task;
    }

}
