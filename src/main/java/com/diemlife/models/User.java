package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import be.objectify.deadbolt.java.models.Permission;
import be.objectify.deadbolt.java.models.Role;
import be.objectify.deadbolt.java.models.Subject;

@Entity
@Indexed
@Table(name = "user")
public class User implements UserEmailDestination, Subject, Serializable {

    public static final String USER_BRAND_FLAG_TRUE = "Y";

    // FIXME: this is a long in the DB!!!
    private Integer id;
    private String email;
    private String name;

    @Field(termVector = TermVector.YES)
    private String firstName;
    @Field(termVector = TermVector.YES)
    private String lastName;
    @Field(termVector = TermVector.YES)
    private String userName;

    private String isUserBrand;
    private Date lastLogin;

    @Field(index = Index.YES)
    private Boolean active;
    private Boolean emailValidated;
    private Date createdOn;

    @Field(index = Index.YES, name = "modificationDate", store = Store.YES)
    //@SortableField(forField = "modificationDate")
    @DateBridge(resolution = Resolution.SECOND)
    private Date updatedOn;

    private Set<SecurityRole> securityRoles = new HashSet<>(0);
    private Set<LinkedAccount> linkedAccounts = new HashSet<>(0);
    private Set<TokenAction> tokenActions = new HashSet<>(0);
    private Set<UserPermission> userPermissions = new HashSet<>(0);
    private String profilePictureURL;
    private String coverPictureURL;
    private String country;
    private String zip;
    private String missionStatement;
    private UserProfile userProfile;
    private String receiveEmail;
    private boolean userNonProfit = false;
    private boolean absorbFees = true;
    private StripeEntity stripeEntity;
    private String profilePictureOriginal;
    private String coverPictureOriginal;
    private String provider;
    private PersonalInfo personalInfo;

    public User() {
        super();
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    @JsonBackReference
    public UserProfile getUserProfile() {
        return this.userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }


    @Column(name = "email")
    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.toLowerCase();
    }

    @Column(name = "name")
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "first_name")
    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column(name = "last_name")
    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Column(name = "user_name")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name = "is_user_brand")
    public String getIsUserBrand() {
        return this.isUserBrand;
    }

    public void setIsUserBrand(String isUserBrand) {
        this.isUserBrand = isUserBrand;
    }

    @JsonIgnore
    @Transient
    public boolean isUserBrand() {
        return USER_BRAND_FLAG_TRUE.equalsIgnoreCase(this.isUserBrand);
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login", length = 19)
    public Date getLastLogin() {
        return this.lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Column(name = "active")
    public Boolean getActive() {
        return this.active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Column(name = "email_validated")
    public Boolean getEmailValidated() {
        return this.emailValidated;
    }

    public void setEmailValidated(Boolean emailValidated) {
        this.emailValidated = emailValidated;
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

    @Column(name = "profile_picture_url")
    public String getProfilePictureURL() {
        return this.profilePictureURL;
    }

    public void setProfilePictureURL(String profilePictureURL) {
        this.profilePictureURL = profilePictureURL;
    }

    @Column(name = "cover_picture_url")
    public String getCoverPictureURL() {
        return this.coverPictureURL;
    }

    public void setCoverPictureURL(String coverPictureURL) {
        this.coverPictureURL = coverPictureURL;
    }

    @Column(name = "country")
    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Column(name = "zip_code")
    public String getZip() {
        return this.zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    @Column(name = "mission_stmt")
    public String getMissionStatement() {
        return missionStatement;
    }

    public void setMissionStatement(String missionStatement) {
        this.missionStatement = missionStatement;
    }

    @Column(name = "receive_email")
    public String getReceiveEmail() {
        return this.receiveEmail;
    }

    public void setReceiveEmail(String receiveEmail) {
        this.receiveEmail = receiveEmail;
    }

    @Column(name = "is_user_non_profit")
    public boolean isUserNonProfit() {
        return userNonProfit;
    }

    public void setUserNonProfit(boolean userNonProfit) {
        this.userNonProfit = userNonProfit;
    }

    @Column(name = "absorbs_fees", nullable = false)
    public boolean isAbsorbFees() {
        return absorbFees;
    }

    public void setAbsorbFees(final boolean absorbFees) {
        this.absorbFees = absorbFees;
    }

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_has_security_role", joinColumns = {@JoinColumn(name = "user_id", nullable = false, updatable = false)}, inverseJoinColumns = {@JoinColumn(name = "security_role_id", nullable = false, updatable = false)})
    @JsonManagedReference
    public Set<SecurityRole> getSecurityRoles() {
        return this.securityRoles;
    }

    public void setSecurityRoles(Set<SecurityRole> securityRoles) {
        this.securityRoles = securityRoles;
    }

    @JsonIgnore
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
    @JsonManagedReference
    public Set<LinkedAccount> getLinkedAccounts() {
        return this.linkedAccounts;
    }

    public void setLinkedAccounts(Set<LinkedAccount> linkedAccounts) {
        this.linkedAccounts = linkedAccounts;
    }

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @JsonManagedReference
    public Set<TokenAction> getTokenActions() {
        return this.tokenActions;
    }

    public void setTokenActions(Set<TokenAction> tokenActions) {
        this.tokenActions = tokenActions;
    }

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_has_user_permission", joinColumns = {@JoinColumn(name = "user_id", nullable = false, updatable = false)}, inverseJoinColumns = {@JoinColumn(name = "user_permission_id", nullable = false, updatable = false)})
    @JsonManagedReference
    public Set<UserPermission> getUserPermissions() {
        return this.userPermissions;
    }

    public void setUserPermissions(Set<UserPermission> userPermissions) {
        this.userPermissions = userPermissions;
    }

    @Transient
    public String getProvider() {
        return provider;
    }

    @Transient
    public void setProvider(final String provider) {
        this.provider = provider;
    }

    @Transient
    public Set<String> getProviders() {
        Set<String> providerKeys = new HashSet<>(this.linkedAccounts.size());
        for (LinkedAccount acc : this.getLinkedAccounts()) {
            providerKeys.add(acc.getProviderKey());
        }
        return providerKeys;
    }

    @Override
    @Transient
    public String getIdentifier() {
        return Integer.toString(this.id);
    }

    @JsonIgnore
    @Override
    @Transient
    public List<? extends Permission> getPermissions() {
        return new ArrayList<>(this.getUserPermissions());
    }

    @JsonIgnore
    @Override
    @Transient
    public List<? extends Role> getRoles() {
    	return new ArrayList<>();
    	//FIXME Raj
    	//return new ArrayList<>(this.getSecurityRoles());
    }

    @JsonManagedReference("UserToStripeEntity")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("stripeEntityId")
    @OneToOne(targetEntity = StripeEntity.class, mappedBy = "user", fetch = FetchType.EAGER)
    public StripeEntity getStripeEntity() {
        return stripeEntity;
    }

    public void setStripeEntity(final StripeEntity stripeEntity) {
        this.stripeEntity = stripeEntity;
    }

    @Column(name = "profile_picture_original")
    public String getProfilePictureOriginal() {
        return profilePictureOriginal;
    }

    public void setProfilePictureOriginal(String profilePictureOriginal) {
        this.profilePictureOriginal = profilePictureOriginal;
    }

    @Column(name = "cover_picture_original")
    public String getCoverPictureOriginal() {
        return coverPictureOriginal;
    }

    public void setCoverPictureOriginal(String coverPictureOriginal) {
        this.coverPictureOriginal = coverPictureOriginal;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_info_id") //nullable
    public PersonalInfo getPersonalInfo() {
        return this.personalInfo;
    }

    public void setPersonalInfo(PersonalInfo personalInfo) {
        this.personalInfo = personalInfo;
    }
}
