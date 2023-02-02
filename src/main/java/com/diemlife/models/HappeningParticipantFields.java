package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import static javax.persistence.FetchType.LAZY;

@Entity(name = "HappeningParticipantFields")
@Table(name = "event_participant_fields")
public class HappeningParticipantFields extends IdentifiedEntity {

    @JsonBackReference("event_participant_reference")
    @OneToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    public Happening event;

    @JsonProperty("hasName")
    @Column(name = "has_name", nullable = false)
    public boolean hasName;

    @JsonProperty("hasEmail")
    @Column(name = "has_email", nullable = false)
    public boolean hasEmail;

    @JsonProperty("hasPhone")
    @Column(name = "has_phone", nullable = false)
    public boolean hasPhone;

    @JsonProperty("hasGender")
    @Column(name = "has_gender", nullable = false)
    public boolean hasGender;

    @JsonProperty("hasBirthDate")
    @Column(name = "has_birth_date", nullable = false)
    public boolean hasBirthDate;

    @JsonProperty("hasAge")
    @Column(name = "has_age", nullable = false)
    public boolean hasAge;

    @JsonProperty("hasAddress")
    @Column(name = "has_address", nullable = false)
    public boolean hasAddress;

    @JsonProperty("hasZipOnly")
    @Column(name = "has_zip_only", nullable = false)
    public boolean hasZipOnly;

    @JsonProperty("hasEmergency")
    @Column(name = "has_emergency", nullable = false)
    public boolean hasEmergency;

}
