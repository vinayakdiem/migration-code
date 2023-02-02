package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import static javax.persistence.FetchType.EAGER;

@Entity(name = "ReferredQuests")
@Table(name = "event_referred_quests")
public class ReferredQuest extends IdentifiedEntity {

    @Column(name = "message")
    public String message;

    @Column(name = "amount_in_cents")
    public Long amount;

    @Column(name = "default_on")
    public Boolean defaultOn;

    @JsonManagedReference("ReferredQuestsToEvent")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("eventId")
    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_referred_event_id"))
    public Happening event;

    @JsonManagedReference("ReferredQuestsToQuests")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("questId")
    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "quest_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_referred_quest_id"))
    public Quests quest;

}
