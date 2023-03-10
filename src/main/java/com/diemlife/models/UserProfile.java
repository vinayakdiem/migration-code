package com.diemlife.models;

// Generated Jun 5, 2015 4:35:44 PM by Hibernate Tools 4.3.1

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * UserProfile generated by hbm2java
 */
@Entity
@Table(name = "user_profile")
public class UserProfile implements Serializable {
    private static final long serialVersionUID = 2204436828844586782L;

    private Integer id;
    private String address1;
    private String address2;
    private String city;
    private String county;
    private String postcode;
    private String country;
    private String mobileNumber;
    private String homeNumber;
    private String gender;
    private Date dob;
    private BigDecimal lat;
    private BigDecimal lng;
    private String salutation;
    private String TiUserId;
    private String DeviceToken;
    private String DeviceOs;
    private Date createdOn;
    private Date updatedOn;
    private Set<User> users = new HashSet<User>(0);

    public UserProfile() {
    }

    public UserProfile(String address1, String address2, String city,
                       String county, String postcode, String country,
                       String mobileNumber, String homeNumber, String gender, Date dob,
                       BigDecimal lat, BigDecimal lng, String salutation) {
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.county = county;
        this.postcode = postcode;
        this.country = country;
        this.mobileNumber = mobileNumber;
        this.homeNumber = homeNumber;
        this.gender = gender;
        this.dob = dob;
        this.lat = lat;
        this.lng = lng;
        this.salutation = salutation;
    }

    public UserProfile(String address1, String address2, String city,
                       String county, String postcode, String country,
                       String mobileNumber, String homeNumber, String gender, Date dob,
                       BigDecimal lat, BigDecimal lng, String salutation, Date createdOn,
                       Date updatedOn, Set<User> users) {
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.county = county;
        this.postcode = postcode;
        this.country = country;
        this.mobileNumber = mobileNumber;
        this.homeNumber = homeNumber;
        this.gender = gender;
        this.dob = dob;
        this.lat = lat;
        this.lng = lng;
        this.salutation = salutation;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.users = users;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "address1")
    public String getAddress1() {
        return this.address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    @Column(name = "address2")
    public String getAddress2() {
        return this.address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    @Column(name = "city", length = 150)
    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Column(name = "county", length = 80)
    public String getCounty() {
        return this.county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    @Column(name = "postcode", length = 8)
    public String getPostcode() {
        return this.postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    @Column(name = "country", length = 150)
    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Column(name = "mobile_number", length = 45)
    public String getMobileNumber() {
        return this.mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    @Column(name = "home_number", length = 45)
    public String getHomeNumber() {
        return this.homeNumber;
    }

    public void setHomeNumber(String homeNumber) {
        this.homeNumber = homeNumber;
    }

    @Column(name = "gender", length = 6)
    public String getGender() {
        return this.gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Temporal(TemporalType.DATE)
    @Column(name = "dob", length = 10)
    public Date getDob() {
        return this.dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

//    @Column(name = "lat", precision = 10, scale = 8)
//    public BigDecimal getLat() {
//        return this.lat;
//    }
//
//    public void setLat(BigDecimal lat) {
//        this.lat = lat;
//    }
//
//    @Column(name = "lng", precision = 11, scale = 8)
//    public BigDecimal getLng() {
//        return this.lng;
//    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }

//    @Column(name = "salutation", length = 45)
//    public String getSalutation() {
//        return this.salutation;
//    }
//
//    public void setSalutation(String salutation) {
//        this.salutation = salutation;
//    }

    @Column(name = "ti_user_id", length = 80)
    public String getTiUserId() {
        return TiUserId;
    }

    public void setTiUserId(String tiUserId) {
        TiUserId = tiUserId;
    }

    @Column(name = "device_token", length = 255)
    public String getDeviceToken() {
        return DeviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        DeviceToken = deviceToken;
    }

    @Column(name = "device_os", length = 80)
    public String getDeviceOs() {
        return DeviceOs;
    }

    public void setDeviceOs(String deviceOs) {
        DeviceOs = deviceOs;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on", length = 19)
    public Date getCreatedOn() {
        return this.createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_on", length = 19)
    public Date getUpdatedOn() {
        return this.updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "userProfile")
    @JsonManagedReference
    public Set<User> getUsers() {
        return this.users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    @PrePersist
    void onCreate() {
        this.setCreatedOn(new Date());
        this.setUpdatedOn(new Date());
    }

    @PreUpdate
    void onPersist() {
        this.setUpdatedOn(new Date());
    }

}