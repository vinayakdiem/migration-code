package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Date;
import java.util.List;


import static javax.persistence.FetchType.EAGER;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity(name = "FundraisingLinks")
@Table(name = "fundraising_link")
public class FundraisingLink extends IdentifiedEntity {

    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(
            name = "fundraiser_user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fundraising_link_user_id_fk")
    )
    public User fundraiser;

    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(
            name = "fundraising_quest_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fundraising_link_quest_id_fk")
    )
    public Quests quest;

    @Temporal(TIMESTAMP)
    @Column(name = "created_on", nullable = false, updatable = false)
    public Date createdOn;

    @Column(name = "campaign_name")
    public String campaignName;

    public String getCampaignName() {
        return campaignName;
    }

    @Column(name = "cover_image_url", length = 2047)
    public String coverImageUrl;

    @Column(name = "target_amount_cents", nullable = false)
    public Long targetAmountCents;

    @Column(name = "target_amount_currency", length = 15, nullable = false, updatable = false)
    public String targetAmountCurrency;

    @Column(name = "active", nullable = false)
    public boolean active;

    @Column(name = "display_fundraise_btn", nullable = false)
    public boolean displayBtn;

    @OneToMany(fetch = EAGER)
    @JoinTable(
            name = "fundraising_link_transactions",
            joinColumns = @JoinColumn(
                    name = "fundraising_link_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fundraising_transactions_link_id_fk")),
            inverseJoinColumns = @JoinColumn(
                    name = "payment_transaction_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fundraising_transactions_payment_id_fk")))
    public List<PaymentTransaction> transactions;

    @Column(name = "fundraising_end_date")
    public Date endDate;

    @ManyToOne(fetch = EAGER)
    @JoinColumn(
            name = "brand_config_id",
            updatable = false,
            foreignKey = @ForeignKey(name = "fundraising_link_brand_config_id_fk")
    )
    public BrandConfig brand;

    @ManyToOne(fetch = EAGER)
    @JoinColumn(
            name = "secondary_brand_config_id",
            updatable = false,
            foreignKey = @ForeignKey(name = "fundraising_link_brand_config_id_fk")
    )
    public BrandConfig secondaryBrand;
}
