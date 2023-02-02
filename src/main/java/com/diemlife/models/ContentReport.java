package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;

import java.util.Date;

import static javax.persistence.FetchType.EAGER;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity(name = "ContentReports")
@Table(name = "content_reports")
public class ContentReport extends IdentifiedEntity {

    @Temporal(TIMESTAMP)
    @Column(name = "report_date", nullable = false)
    public Date reportedOn;

    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "reporter_user_id", nullable = false, foreignKey = @ForeignKey(name = "content_reports_reporter_user_fk"))
    public User reporter;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "accused_quest_id", foreignKey = @ForeignKey(name = "content_reports_accused_quest_fk"))
    public Quests accusedQuest;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "accused_comment_id", foreignKey = @ForeignKey(name = "content_reports_accused_comment_fk"))
    public QuestComments accusedComment;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "accused_user_id", foreignKey = @ForeignKey(name = "content_reports_accused_user_fk"))
    public User accusedUser;

    @Column(name = "flag_hidden")
    public boolean hiddenFlag = false;

    @Column(name = "flag_reported")
    public boolean reportedFlag = false;

}
