package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "QuestRecommendations")
@Table(name = "quest_recommendation")
public class QuestRecommendation implements Serializable {

    @Id
    public Integer id;

    @Column(name = "active")
    public Boolean active;

    @Column(name = "created_on")
    public Date createdOn;

    @OneToOne(targetEntity = Quests.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quest_id")
    public Quests quest;

}
