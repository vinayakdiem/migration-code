package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

import static javax.persistence.FetchType.EAGER;

@Entity(name = "Happenings")
@Table(name = "event")
public class Happening extends IdentifiedEntity {

    @JsonProperty("active")
    @Column(name = "active", nullable = false)
    public boolean active;

    @JsonProperty("waiver")
    @Column(name = "waiver", length = Integer.MAX_VALUE)
    public String waiver;

    @JsonIgnore
    @Column(name = "stripe_product_id", nullable = false)
    public String stripeProductId;

    @JsonIgnore
    @Column(name = "event_email", length = Integer.MAX_VALUE)
    public String eventEmail;

    @JsonProperty("registrationTemplate")
    @Column(name = "registration_template")
    public String registrationTemplate;

    @JsonProperty("showDiscounts")
    @Column(name = "show_discounts")
    public boolean showDiscounts;

    @JsonProperty("showWaiver")
    @Column(name = "show_waiver")
    public boolean showWaiver;

    @JsonProperty("happeningDate")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "event_date")
    public Date happeningDate;

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "register_start_date")
    public Date registerStartDate;

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "register_end_date")
    public Date registerEndDate;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(name = "quest_id", nullable = false, foreignKey = @ForeignKey(name = "fk_happening_quest_id"))
    public Quests quest;

    @JsonIgnore
    @OneToMany(mappedBy = "event", fetch = EAGER)
    public List<HappeningAddOn> addOns;

    @JsonManagedReference("event_participant_reference")
    @OneToOne(mappedBy = "event", fetch = EAGER)
    public HappeningParticipantFields participantFields;

    @JsonIgnore
    @PrePersist
    void prePersist() {
        active = true;
    }

}
