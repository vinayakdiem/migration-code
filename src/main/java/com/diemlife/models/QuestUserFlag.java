package com.diemlife.models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.diemlife.constants.QuestUserFlagKey;

@Entity(name = "QuestUserFlags")
@Table(name = "quest_user_flags")
public class QuestUserFlag extends IdentifiedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false, updatable = false)
    public Quests quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    public User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_key", nullable = false, updatable = false)
    public QuestUserFlagKey flagKey;

    @Column(name = "flag_value", nullable = false)
    public boolean flagValue;

    @Temporal(TemporalType.DATE)
    @Column(name = "creation_date", nullable = false)
    public Date creationDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "modification_date")
    public Date modificationDate;

}
