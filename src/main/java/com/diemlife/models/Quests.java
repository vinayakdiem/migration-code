package com.diemlife.models;

import static javax.persistence.EnumType.STRING;
import static javax.persistence.FetchType.EAGER;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

import com.bedatadriven.jackson.datatype.jts.serialization.GeometryDeserializer;
import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.diemlife.constants.DirectionsType;
import com.diemlife.constants.PrivacyLevel;
import com.diemlife.constants.QuestMode;
import com.diemlife.dto.LeaderboardAttributeDTO;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Point;

/**
 * Created by andrewcoleman on 3/3/16.
 */
@Entity
@Table(name = "quest_feed")
@Indexed
@NamedQueries({
        @NamedQuery(name = "findImageById", query = "SELECT q FROM Quests q where q.id = ?1"),
        @NamedQuery(name = "findByCategory", query = "SELECT q FROM Quests  q where q.pillar = :categoryCodeIn")
})
public class Quests implements QuestSEO, Serializable {

    private PrivacyLevel privacyLevel;

    // FIXME: this is a long in the db!
    private Integer id;
    private String shortDescription;
    private String questFeed;
    private String pillar;
    private Integer sharedCount;
    private Integer savedCount;
    private Integer commentCount;
    private Integer views;
    private Float weight;
    private Integer status;
    private Integer createdBy;
    private Integer origCreatedBy;
    private Date dateCreated;
    private Integer modifiedBy;
    private String distanceAttributeName;

    @Field(index = Index.YES, name = "modificationDate", store = Store.YES)
    @DateBridge(resolution = Resolution.SECOND)
    private Date dateModified;

    private String title;
    private String photo;
    private EmbeddedVideo video;
    private String type;
    private User user;
    private List<User> admins;
    private List<LeaderboardAttribute> leaderboardAttributes = new LinkedList<LeaderboardAttribute>();
    private Integer version;
    private boolean fundraising = false;
    private boolean fundraisingBarHidden = false;
    private boolean showBackingAmount = false;
    private boolean startBtnDisabled;
    private boolean backBtnDisabled = true;
    private boolean milestoneControlsDisabled = false;
    private boolean copyAllowed = false;
    private boolean editableMilestones = false;
    private boolean leaderboardEnabled = false;
    private boolean surveysEnabled = false;
    private boolean geoTriggerEnabled = false;
    private boolean taskViewDisabled = false;
    private boolean multiSellerEnabled = false;
    private boolean tippingEnabled = true;
    private boolean monthlyDonationEnabled = false;
    private boolean multiTeamsEnabled = false;
    private QuestMode mode;
    public String place;

    @JsonProperty
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private Point point;

    private DirectionsType directionsType;
    private Integer questMapViewId;
    private String category;
    private boolean realtime;

    @Enumerated(STRING)
    @Column(name = "privacy")
    public PrivacyLevel getPrivacyLevel() {
        return privacyLevel;
    }

    @Column(name = "fundraising")
    public boolean isFundraising() {
        return fundraising;
    }

    @Column(name = "fundraising_bar_hidden")
    public boolean isFundraisingBarHidden() {
        return fundraisingBarHidden;
    }

    @Column(name = "show_backing_amount")
    public boolean isShowBackingAmount() {
        return showBackingAmount;
    }

    @Column(name = "start_btn_disabled")
    public boolean isStartBtnDisabled() {
        return startBtnDisabled;
    }

    public void setStartBtnDisabled(boolean startBtnDisabled) {
        this.startBtnDisabled = startBtnDisabled;
    }

    @Column(name = "copy_allowed", nullable = false)
    public boolean isCopyAllowed() {
        return copyAllowed;
    }

    @Column(name = "editable_milestones", nullable = false)
    public boolean isEditableMilestones() {
        return editableMilestones;
    }

    @Column(name = "leaderboard_enabled", nullable = false)
    public boolean isLeaderboardEnabled() {
        return leaderboardEnabled;
    }

    @Column(name = "surveys_enabled", nullable = false)
    public boolean isSurveysEnabled() {
        return surveysEnabled;
    }

    @Column(name = "geo_trigger_enabled", nullable = false)
    public boolean isGeoTriggerEnabled() {
        return geoTriggerEnabled;
    }

    @JsonSerialize(using = QuestMode.QuestModeKeySerializer.class)
    @Enumerated(STRING)
    @Column(name = "mode")
    public QuestMode getMode() {
        return mode;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    public Integer getId() {
        return this.id;
    }

    @Field(termVector = TermVector.YES)
    @Column(name = "quest_feed")
    public String getQuestFeed() {
        return this.questFeed;
    }

    @Field(termVector = TermVector.YES)
    @Column(name = "short_description")
    public String getShortDescription() {
        return shortDescription;
    }

    @Field(termVector = TermVector.YES)
    @Column(name = "pillar")
    public String getPillar() {
        return this.pillar;
    }

    @Column(name = "shared_count")
    public Integer getSharedCount() {
        return this.sharedCount;
    }

    @Column(name = "saved_count")
    public Integer getSavedCount() {
        return this.savedCount;
    }

    @Column(name = "comment_count")
    public Integer getCommentCount() {
        return this.commentCount;
    }

    @Column(name = "views")
    public Integer getViews() {
        return views;
    }

    @Column(name = "weight")
    public Float getWeight() {
        return weight;
    }

    @Column(name = "status")
    public Integer getStatus() {
        return this.status;
    }

    @Column(name = "created_by")
    public Integer getCreatedBy() {
        return this.createdBy;
    }

    @Column(name = "orig_created_by")
    public Integer getOrigCreatedBy() {
        return this.origCreatedBy;
    }

    @Column(name = "created_date", columnDefinition = "datetime")
    public Date getDateCreated() {
        return this.dateCreated;
    }

    @Column(name = "last_modified_by")
    public Integer getModifiedBy() {
        return this.modifiedBy;
    }

    @Column(name = "distance_attribute_name")
    public String getDistanceAttributeName() {
        return distanceAttributeName;
    }

    public void setDistanceAttributeName(String distanceAttributeName) {
        this.distanceAttributeName = distanceAttributeName;
    }

    @Column(name = "last_modified_date", columnDefinition = "datetime")
    public Date getDateModified() {
        return this.dateModified;
    }

    @Field(termVector = TermVector.YES)
    @Column(name = "title")
    public String getTitle() {
        return this.title;
    }

    @Column(name = "photo")
    public String getPhoto() {
        return this.photo;
    }

    @JsonProperty("questVideoUrl")
    @JsonSerialize(using = EmbeddedVideo.VideoUrlSerializer.class)
    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "embedded_video_id", foreignKey = @ForeignKey(name = "quest_feed_embedded_video_id_fk"))
    public EmbeddedVideo getVideo() {
        return video;
    }

    public void setVideo(final EmbeddedVideo video) {
        this.video = video;
    }

    @Column(name = "version")
    public Integer getVersion() {
        return version;
    }

    public void setPrivacyLevel(PrivacyLevel privacyLevel) {
        this.privacyLevel = privacyLevel;
    }

    @Column(name = "type")
    public String getType() {
        return this.type;
    }

    @OneToOne
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.setCreatedBy(user.getId());
        }
    }

    @JsonSerialize(as = List.class, contentUsing = UserToEmailSerializer.class)
    @ManyToMany(fetch = LAZY)
    @JoinTable(name = "quest_admins", joinColumns = {
            @JoinColumn(name = "quest_id", nullable = false, foreignKey = @ForeignKey(name = "quest_admins_quest_id_fk"))
    }, inverseJoinColumns = {
            @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "quest_admins_user_id_fk"))
    })
    public List<User> getAdmins() {
        return admins;
    }

    @JsonIgnore
    public void setAdmins(final List<User> admins) {
        this.admins = admins;
    }

    @Transient
    @JsonGetter
    @JsonSerialize(as = List.class, contentUsing = LeaderboardAttributeToDtoSerializer.class)
    public List<LeaderboardAttribute> getLeaderboardAttributes() {
        return leaderboardAttributes;
    }

    @Column(name = "place")
    public String getPlace() {
        return place;
    }

    @Column(name = "geo_point")
    public Point getPoint() {
        return point;
    }

    @Column(name = "geo_directions_type")
    @Enumerated(STRING)
    public DirectionsType getDirectionsType() {
        return directionsType;
    }

    @Column(name = "map_view_id")
    public Integer getQuestMapViewId() {
        return questMapViewId;
    }

    public void setQuestMapViewId(Integer questMapViewId) {
        this.questMapViewId = questMapViewId;
    }

    @Column(name = "category")
    public String getCategory() {
        return category;
    }

    @Column(name = "realtime")
    public boolean isRealtime() {
        return realtime;
    }

    @JsonIgnore
    public void setLeaderboardAttributes(final List<LeaderboardAttribute> leaderboardAttributes) {
        this.leaderboardAttributes = leaderboardAttributes;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setQuestFeed(String questFeed) {
        this.questFeed = questFeed;
    }

    public void setShortDescription(final String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setPillar(String pillar) {
        this.pillar = pillar;
    }

    public void setSharedCount(Integer sharedCount) {
        this.sharedCount = sharedCount;
    }

    public void setSavedCount(Integer savedCount) {
        this.savedCount = savedCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public void setOrigCreatedBy(Integer origCreatedBy) {
        this.origCreatedBy = origCreatedBy;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setModifiedBy(Integer modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setFundraising(boolean fundraising) {
        this.fundraising = fundraising;
    }

    public void setFundraisingBarHidden(final boolean fundraisingBarHidden) {
        this.fundraisingBarHidden = fundraisingBarHidden;
    }

    public void setShowBackingAmount(final boolean showBackingAmount) {
        this.showBackingAmount = showBackingAmount;
    }

    @Column(name = "back_btn_disabled")
    public boolean isBackBtnDisabled() {
        return backBtnDisabled;
    }

    public void setBackBtnDisabled(boolean backBtnDisabled) {
        this.backBtnDisabled = backBtnDisabled;
    }

    @Column(name = "milestone_ctrl_disabled")
    public boolean isMilestoneControlsDisabled() {
        return milestoneControlsDisabled;
    }

    @Column(name = "task_view_disabled")
    public boolean isTaskViewDisabled() {
        return taskViewDisabled;
    }

    @Column(name = "multi_seller_enabled")
    public boolean isMultiSellerEnabled() {
        return multiSellerEnabled;
    }

    @Column(name = "tipping_enabled")
    public boolean isTippingEnabled() {
        return tippingEnabled;
    }

    @Column(name = "monthly_donation_enabled")
    public boolean isMonthlyDonationEnabled() {
        return monthlyDonationEnabled;
    }

    @Column(name = "multi_teams_enabled")
    public boolean isMultiTeamsEnabled() {
        return multiTeamsEnabled;
    }

    public void setMilestoneControlsDisabled(boolean milestoneControlsDisabled) {
        this.milestoneControlsDisabled = milestoneControlsDisabled;
    }

    public void setMultiSellerEnabled(final boolean multiSellerEnabled) {
        this.multiSellerEnabled = multiSellerEnabled;
    }

    public void setTippingEnabled(boolean tippingEnabled) {
        this.tippingEnabled = tippingEnabled;
    }

    public void setMonthlyDonationEnabled(boolean monthlyDonationEnabled) {
        this.monthlyDonationEnabled = monthlyDonationEnabled;
    }

    public void setMultiTeamsEnabled(boolean multiTeamsEnabled) {
        this.multiTeamsEnabled = multiTeamsEnabled;
    }

    public void setTaskViewDisabled(boolean taskViewDisabled) {
        this.taskViewDisabled = taskViewDisabled;
    }

    public void setCopyAllowed(boolean copyAllowed) {
        this.copyAllowed = copyAllowed;
    }

    public void setEditableMilestones(boolean editableMilestones) {
        this.editableMilestones = editableMilestones;
    }

    public void setLeaderboardEnabled(boolean leaderboardEnabled) {
        this.leaderboardEnabled = leaderboardEnabled;
    }

    public void setSurveysEnabled(boolean surveysEnabled) {
        this.surveysEnabled = surveysEnabled;
    }

    public void setGeoTriggerEnabled(boolean geoTriggerEnabled) {
        this.geoTriggerEnabled = geoTriggerEnabled;
    }

    public void setMode(QuestMode mode) {
        this.mode = mode;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public void setDirectionsType(final DirectionsType directionsType) {
        this.directionsType = directionsType;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setRealtime(boolean realtime) {
        this.realtime = realtime;
    }

    static class UserToEmailSerializer extends JsonSerializer<User> {
        @Override
        public void serialize(final User user,
                              final JsonGenerator generator,
                              final SerializerProvider provider) throws IOException {
            generator.writeString(user == null ? null : user.getEmail());
        }
    }

    static class LeaderboardAttributeToDtoSerializer extends JsonSerializer<LeaderboardAttribute> {
        @Override
        public void serialize(final LeaderboardAttribute attribute,
                              final JsonGenerator generator,
                              final SerializerProvider serializers) throws IOException {
            generator.writeObject(LeaderboardAttributeDTO.builder()
                    .id(attribute.getId())
                    .name(attribute.getName())
                    .unit(attribute.getUnit())
                    .asc(attribute.isAsc())
                    .build()
            );
        }
    }

    @Transient
    public boolean isCopyProtectionEnabled() {
        return !isCopyAllowed();
    }
}
