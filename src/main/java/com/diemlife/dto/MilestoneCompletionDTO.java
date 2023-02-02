package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;

import com.diemlife.models.QuestTaskCompletionHistory;

import forms.QuestActionPointForm;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MilestoneCompletionDTO implements Serializable {

    private Integer userTriggeredId;
    private Date dateTriggered;
    private boolean completed;
    private boolean geoPointInArea;
    private QuestActionPointForm geoPoint;

    public static MilestoneCompletionDTO toDto(final QuestTaskCompletionHistory completion) {
        final MilestoneCompletionDTO dto = new MilestoneCompletionDTO();
        dto.setUserTriggeredId(completion.getUserTriggeredId());
        dto.setDateTriggered(completion.getDateTriggered());
        dto.setCompleted(completion.isCompleted());
        dto.setGeoPointInArea(completion.isGeoPointInArea());

        if (completion.getPoint() != null) {
            dto.setGeoPoint(new QuestActionPointForm(
                    Double.valueOf(completion.getPoint().getCoordinate().x).floatValue(),
                    Double.valueOf(completion.getPoint().getCoordinate().y).floatValue()
            ));
        }

        return dto;
    }

}
