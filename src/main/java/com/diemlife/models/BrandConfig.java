package com.diemlife.models;

import static javax.persistence.FetchType.EAGER;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "BrandConfig")
@Table(name = "brand_config")
@Getter
@Setter
@NoArgsConstructor
public class BrandConfig implements Serializable {

    @Id
    @Column(name = "user_id", unique = true, nullable = false)
    private Integer userId;

    @ManyToOne(fetch = EAGER, targetEntity = User.class)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false, unique = true)
    private User user;

    @Column(name = "brand_full_name")
    private String fullName;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "site_url")
    private String siteUrl;

    @Column(name = "non_profit", nullable = false)
    private boolean nonProfit;

    @Column(name = "on_landing", nullable = false)
    private boolean onLanding;

    @Column(name = "landing_order", nullable = false)
    private int landingOrder;

}
