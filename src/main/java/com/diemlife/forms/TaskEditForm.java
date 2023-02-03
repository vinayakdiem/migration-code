package com.diemlife.forms;

import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskEditForm extends MilestoneForm {

     private Integer id;
    private Integer questId;
    private Integer userId;
    private Long questMapRouteWaypointId;
    private String taskCompleted;
    private String taskCompletionDate;
   // private Date lastModifiedDate;
   // private Integer lastModifiedBy;
    private Float radiusInKm;
    private String pinUrl;
    private String pinCompletedUrl;
    private Integer attributeValueNumeric;
    private String attributeValueString;
    private String timeHorizonStartDate;
    private String timeHorizonEndDate;
    private String frequency;
    private Boolean required;
    private Integer activityRecordListId;
    private Integer groupIndex;

}
