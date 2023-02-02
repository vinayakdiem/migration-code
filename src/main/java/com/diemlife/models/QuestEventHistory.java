package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.bedatadriven.jackson.datatype.jts.serialization.GeometryDeserializer;
import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Point;

@Entity
@Table(name = "quest_event_history", schema = "diemlife")
public class QuestEventHistory {

    private Integer id;
    private Integer questId;
    private Integer userId;
    private QuestEvents eventDesc;
    private Integer origQuestId;
    private Date addedDate;
    private Date lastModifiedDate;

    @JsonProperty
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private Point point;

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

    @Column(name = "event_description")
    @Enumerated(EnumType.STRING)
    public QuestEvents getEventDesc() {
        return eventDesc;
    }

    public void setEventDesc(QuestEvents eventDesc) {
        this.eventDesc = eventDesc;
    }

    @Column(name = "orig_quest_id")
    public Integer getOrigQuestId() {
        return origQuestId;
    }

    public void setOrigQuestId(Integer origQuestId) {
        this.origQuestId = origQuestId;
    }

    @Column(name = "added_date")
    public Date getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Date addedDate) {
        this.addedDate = addedDate;
    }

    @Column(name = "last_modified_date")
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Column(name = "geo_point")
    public Point getPoint() {
        return point;
    }

    public void setPoint(final Point point) {
        this.point = point;
    }

}
