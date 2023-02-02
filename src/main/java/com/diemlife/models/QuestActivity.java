package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Timestamp;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.diemlife.constants.QuestActivityStatus;
import com.diemlife.constants.QuestMode;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Created by andrewcoleman on 8/22/16.
 */
@Entity
@Table(name = "quest_activity", schema = "diemlife")
public class QuestActivity {
    private int id;
    private int userId;
    private int questId;
    private QuestActivityStatus status;
    private QuestMode mode;
    private int cyclesCounter;
    private String group;
    private Timestamp addedDate;
    private Timestamp lastModifiedDate;
    private Quests quests;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "user_id")
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Basic
    @Column(name = "quest_id")
    public int getQuestId() {
        return questId;
    }

    public void setQuestId(int questId) {
        this.questId = questId;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public QuestActivityStatus getStatus() {
        return status;
    }

    public void setStatus(final QuestActivityStatus status) {
        this.status = status;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "mode")
    public QuestMode getMode() {
        return mode;
    }

    public void setMode(final QuestMode mode) {
        this.mode = mode;
    }

    @Basic
    @Column(name = "cycles_counter", nullable = false)
    public int getCyclesCounter() {
        return cyclesCounter;
    }

    public void setCyclesCounter(int cyclesCounter) {
        this.cyclesCounter = cyclesCounter;
    }

    @Column(name = "group_name")
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Basic
    @Column(name = "added_date")
    public Timestamp getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Timestamp addedDate) {
        this.addedDate = addedDate;
    }

    @Basic
    @Column(name = "last_modified_date")
    public Timestamp getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Timestamp lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @OneToOne
    @JoinColumn(name = "quest_id", insertable = false, updatable = false)
    @Fetch(FetchMode.JOIN)
    @JsonBackReference
    public Quests getQuests() {
        return this.quests;
    }

    public void setQuests(Quests quests) {
        this.quests = quests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuestActivity that = (QuestActivity) o;

        if (id != that.id) return false;
        if (userId != that.userId) return false;
        if (questId != that.questId) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (mode != null ? !mode.equals(that.mode) : that.mode != null) return false;
        if (addedDate != null ? !addedDate.equals(that.addedDate) : that.addedDate != null) return false;
        return lastModifiedDate != null ? lastModifiedDate.equals(that.lastModifiedDate) : that.lastModifiedDate == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + userId;
        result = 31 * result + questId;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        result = 31 * result + (addedDate != null ? addedDate.hashCode() : 0);
        result = 31 * result + (lastModifiedDate != null ? lastModifiedDate.hashCode() : 0);
        return result;
    }
}
