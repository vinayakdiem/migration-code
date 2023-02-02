package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity(name = "QuestCommentLikes")
@Table(name = "quest_comment_like")
public class QuestCommentLike extends IdentifiedEntity {

    @JsonBackReference("questCommentReference")
    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_comment_id", nullable = false, updatable = false)
    public QuestComments comment;

    @JsonIdentityReference(alwaysAsId = true)
    @JsonIdentityInfo(generator = PropertyGenerator.class, property = "id")
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    public User liker;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on")
    public Date createdOn;

}
