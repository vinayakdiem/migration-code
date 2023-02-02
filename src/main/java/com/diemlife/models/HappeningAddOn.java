package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import static javax.persistence.FetchType.LAZY;

@Entity(name = "HappeningAddOns")
@Table(name = "event_add_ons")
public class HappeningAddOn extends IdentifiedEntity {

    @JsonIgnore
    @ManyToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    public Happening event;

    @JsonProperty("addOnType")
    @Enumerated(EnumType.STRING)
    @Column(name = "add_on_type", nullable = false)
    public HappeningAddOnType addOnType;

    @JsonIgnore
    @Column(name = "stripe_product_id")
    public String stripeProductId;

}
