package com.diemlife.models;

import com.bedatadriven.jackson.datatype.jts.serialization.GeometryDeserializer;
import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Point;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.sql.Timestamp;

import static javax.persistence.FetchType.LAZY;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "QuestTaskCompletionHistory")
@Table(name = "quest_task_completion_history", indexes = {
        @Index(name = "quest_task_completion_history_fk_idx", columnList = "milestone_id,user_triggered_id"),
        @Index(name = "quest_task_completion_history_date_idx", columnList = "date_triggered")
})
public class QuestTaskCompletionHistory extends IdentifiedEntity {

    @Column(name = "milestone_id")
    private Integer milestoneId;

    @Column(name = "user_triggered_id")
    private Integer userTriggeredId;

    @Column(name = "date_triggered")
    private Timestamp dateTriggered;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "geo_point")
    @JsonProperty
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private Point point;

    @Column(name = "geo_point_in_area")
    private boolean geoPointInArea;

    @ManyToOne(targetEntity = QuestTasks.class, fetch = LAZY, optional = false)
    @JoinColumn(name = "milestone_id", insertable = false, updatable = false, nullable = false)
    private QuestTasks milestone;

    @ManyToOne(targetEntity = User.class, fetch = LAZY, optional = false)
    @JoinColumn(name = "user_triggered_id", insertable = false, updatable = false, nullable = false)
    private User userTriggered;

}
