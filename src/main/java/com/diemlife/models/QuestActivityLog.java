package com.diemlife.models;

import static javax.persistence.EnumType.STRING;
import static javax.persistence.FetchType.LAZY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.diemlife.constants.QuestActivityStatus;

@Entity(name = "QuestActivityLogs")
@Table(name = "quest_activity", schema = "diemlife")
public class QuestActivityLog extends IdentifiedEntity {

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added_date", nullable = false)
    public Date createdOn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_activity_log_user_id"))
    public User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_id", nullable = false, foreignKey = @ForeignKey(name = "fk_activity_log_quest_id"))
    public Quests quest;

    @Enumerated(STRING)
    @Column(name = "status", nullable = false)
    public QuestActivityStatus status;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "previous_id", foreignKey = @ForeignKey(name = "fk_activity_log_previous_id"))
    public QuestActivityLog previousActivity;

}
