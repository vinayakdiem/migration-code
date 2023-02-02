package com.diemlife.models;

import com.bedatadriven.jackson.datatype.jts.serialization.GeometryDeserializer;
import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Point;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by acoleman1 on 6/5/17.
 */
@Entity
@Table(name = "quest_tasks")
public class QuestTasks {

    private Integer id;
    private Integer questId;
    private Integer userId;
    private Long questMapRouteWaypointId;
    private Integer linkedQuestId;
    private String task;
    private String taskCompleted;
    private EmbeddedVideo video;
    private Date taskCompletionDate;
    private Date createdDate;
    private Integer createdBy;
    private Date lastModifiedDate;
    private Integer lastModifiedBy;
    private Integer order;
    private String title;

    @JsonProperty
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private Point point;

    private Float radiusInKm;
    private String pinUrl;
    private String pinCompletedUrl;
    private String imageUrl;
    private String linkUrl;
    private QuestTasksGroup questTasksGroup;

    /*    @ManyToOne
    @JoinColumn(name = "attribute_unit_id")
    private AttributeUnit attributeUnit;*/

    private Integer attributeValueNumeric;

    private String attributeValueString;

    private Date timeHorizonStartDate;

    private Date timeHorizonEndDate;

    private String frequency;

    private Integer activityRecordListId;

    private Boolean required;

    @Column(name = "attribute_value_numeric")
    public Integer getAttributeValueNumeric() {
        return attributeValueNumeric;
    }

    public void setAttributeValueNumeric(Integer attributeValueNumeric) {
        this.attributeValueNumeric = attributeValueNumeric;
    }

    @Column(name = "attribute_value_string")
    public String getAttributeValueString() {
        return attributeValueString;
    }

    public void setAttributeValueString(String attributeValueString) {
        this.attributeValueString = attributeValueString;
    }

    @Column(name = "time_horizon_start_date")
    public Date getTimeHorizonStartDate() {
        return timeHorizonStartDate;
    }

    public void setTimeHorizonStartDate(Date timeHorizonStartDate) {
        this.timeHorizonStartDate = timeHorizonStartDate;
    }

    @Column(name = "time_horizon_end_date")
    public Date getTimeHorizonEndDate() {
        return timeHorizonEndDate;
    }

    public void setTimeHorizonEndDate(Date timeHorizonEndDate) {
        this.timeHorizonEndDate = timeHorizonEndDate;
    }

    @Column(name = "frequency")
    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    @Column(name = "required")
    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Column(name="activity_record_list_id")
    public Integer getActivityRecordListId() {
        return activityRecordListId;
    }

    public void setActivityRecordListId(Integer activityRecordListId) {
        this.activityRecordListId = activityRecordListId;
    }

    /*public AttributeUnit getAttributeUnit() {
        return attributeUnit;
    }
    public void setAttributeUnit(AttributeUnit attributeUnit) {
        this.attributeUnit = attributeUnit;
    }*/

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "quest_id")
    public Integer getQuestId() {
        return questId;
    }

    public void setQuestId(Integer questId) {
        this.questId = questId;
    }

    @Column(name = "user_id")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Column(name = "quest_map_route_waypoint_id")
    public Long getQuestMapRouteWaypointId() {
        return questMapRouteWaypointId;
    }

    public void setQuestMapRouteWaypointId(Long questMapRouteWaypointId) {
        this.questMapRouteWaypointId = questMapRouteWaypointId;
    }

    @Column(name = "linked_quest_id")
    public Integer getLinkedQuestId() {
        return linkedQuestId;
    }

    public void setLinkedQuestId(Integer linkedQuestId) {
        this.linkedQuestId = linkedQuestId;
    }

    @Column(name = "task")
    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    @Column(name = "task_completed")
    public String getTaskCompleted() {
        return taskCompleted;
    }

    public void setTaskCompleted(String taskCompleted) {
        this.taskCompleted = taskCompleted;
    }

    @ManyToOne
    @JoinColumn(name = "video_id", foreignKey = @ForeignKey(name = "quest_tasks_embedded_video_id_fk"))
    public EmbeddedVideo getVideo() {
        return video;
    }

    public void setVideo(EmbeddedVideo video) {
        this.video = video;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_tasks_group_id", foreignKey = @ForeignKey(name = "quest_tasks_quest_tasks_group_id_fk"))
    @JsonIgnore
    public QuestTasksGroup getQuestTasksGroup() {
        return questTasksGroup;
    }

    public void setQuestTasksGroup(QuestTasksGroup questTasksGroup) {
        this.questTasksGroup = questTasksGroup;
    }

    @Column(name = "task_completion_date")
    public Date getTaskCompletionDate() {
        return taskCompletionDate;
    }

    public void setTaskCompletionDate(Date taskCompletionDate) {
        this.taskCompletionDate = taskCompletionDate;
    }

    @Column(name = "created_date")
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(name = "created_by")
    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    @Column(name = "last_modified_date")
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Column(name = "last_modified_by")
    public Integer getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(Integer lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Column(name = "task_order")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Column(name = "geo_point")
    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    @Column(name = "geo_point_radius_km")
    public Float getRadiusInKm() {
        return radiusInKm;
    }

    public void setRadiusInKm(Float radiusInKm) {
        this.radiusInKm = radiusInKm;
    }

    @Column(name = "geo_point_icon")
    public String getPinUrl() {
        return pinUrl;
    }

    public void setPinUrl(String pinUrl) {
        this.pinUrl = pinUrl;
    }

    @Column(name = "geo_point_completed_icon")
    public String getPinCompletedUrl() {
        return pinCompletedUrl;
    }

    public void setPinCompletedUrl(String pinCompletedUrl) {
        this.pinCompletedUrl = pinCompletedUrl;
    }

    @Column(name = "image_url")
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Column(name = "link_url")
    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(final String linkUrl) {
        this.linkUrl = linkUrl;
    }
    
    @Column(name = "title")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
