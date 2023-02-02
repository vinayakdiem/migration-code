package com.diemlife.models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Date;

import static javax.persistence.TemporalType.TIMESTAMP;

@Entity(name = "HappeningParticipants")
@Table(name = "event_participant")
public class HappeningParticipant extends IdentifiedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    public Happening event;

    @ManyToOne
    @JoinColumn(name = "order_id")
    public PaymentTransaction order;

    @ManyToOne(optional = false, cascade = {CascadeType.ALL})
    @JoinColumn(name = "address_id", nullable = false)
    public Address address;

    @ManyToOne(optional = false, cascade = {CascadeType.ALL})
    @JoinColumn(name = "person_id", nullable = false)
    public PersonalInfo person;

    @ManyToOne(optional = false, cascade = {CascadeType.ALL})
    @JoinColumn(name = "emergency_id")
    public EmergencyContact contact;

    @Column(name = "stripe_sku_id")
    public String stripeSkuId;

    @Column(name = "sku_fee")
    public Integer skuFee;

    @Column(name = "sku_price")
    public Integer skuPrice;

    @Temporal(TIMESTAMP)
    @Column(name = "registration_date")
    public Date registrationDate;

}
