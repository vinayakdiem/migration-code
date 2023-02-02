package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Date;

import static javax.persistence.FetchType.EAGER;
import static javax.persistence.TemporalType.TIMESTAMP;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "QuestBackings")
@Table(name = "quest_backing")
public class QuestBacking extends IdentifiedEntity {

    @Temporal(TIMESTAMP)
    @Column(name = "backing_date", nullable = false)
    private Date backingDate;

    @Column(name = "amount_in_cents", nullable = false, updatable = false)
    private Integer amountInCents;

    @Column(name = "currency", nullable = false, updatable = false)
    private String currency;

    @Column(name = "message", updatable = false)
    private String message;

    @Column(name = "backer_first_name", updatable = false)
    private String backerFirstName;

    @Column(name = "backer_last_name", updatable = false)
    private String backerLastName;

    @OneToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false, updatable = false)
    private PaymentTransaction paymentTransaction;

    @OneToOne(fetch = EAGER, optional = false, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "billing_personal_info_id", nullable = false, updatable = false)
    private PersonalInfo billingPersonalInfo;

    @OneToOne(fetch = EAGER, optional = false, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "billing_address_id", nullable = false, updatable = false)
    private Address billingAddress;

    @Column(name = "offline_mode", updatable = false)
    private Boolean offlineMode;

    @Column(name = "backed_on_behalf", updatable = false)
    private Boolean backedOnBehalf = false;

    @Column(name = "tip", updatable = false)
    private Long tip;
}
