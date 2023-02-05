package com.diemlife.dto;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.diemlife.constants.Util;
import com.diemlife.models.QuestSEO;
import com.diemlife.models.Quests;
import com.diemlife.models.StripeAccount;
import com.diemlife.utils.URLUtils.QuestSEOSlugs;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestDTO implements QuestSEO, Serializable {

    public int id;
    public String title;
    public String questFeed;
    public String shortDescription;
    public String photo;
    public String privacyLevel;
    public boolean fundraising;
    public boolean realtime;
    public boolean copyAllowed;
    public boolean backingAllowed;
    public boolean hasEvent;
    public int createdBy;
    public Date dateCreated;
    public Date dateModified;
    public UserDTO user;
    public Integer sharedCount;
    public Integer commentCount;
    public Integer views;
    public Integer savedCount;
    public String category;
    public String description;
    public String mode;
    public boolean isUserDoing;
    public boolean startBtnDisabled;
    public boolean backBtnDisabled;
    public boolean geoTriggerEnabled;
    public boolean surveysEnabled;
    public boolean multiSellerEnabled;

    @JsonIgnore
    private QuestActivityDTO activityData;
    @JsonIgnore
    public QuestUserFlagsDTO userFlags;

    @JsonProperty
    public QuestSEOSlugs seoSlugs;

    public Boolean isQuestFundraiser;

    public QuestDTO() {
        super();
    }

    protected QuestDTO(final Quests quest) {
        this.id = quest.getId();
        this.title = quest.getTitle();
        this.questFeed = quest.getQuestFeed();
        this.shortDescription = quest.getShortDescription();
        this.photo = quest.getPhoto();
        this.privacyLevel = quest.getPrivacyLevel() == null ? null : quest.getPrivacyLevel().name().toUpperCase();
        this.fundraising = quest.isFundraising();
        this.realtime = quest.isRealtime();
        this.copyAllowed = quest.isCopyAllowed();
        this.backingAllowed = quest.getUser() != null && quest.getUser().getStripeEntity() instanceof StripeAccount;
        this.createdBy = quest.getCreatedBy() == null ? -1 : quest.getCreatedBy();
        this.dateCreated = quest.getDateCreated();
        this.dateModified = quest.getDateModified();
        this.user = UserDTO.toDTO(quest.getUser());
        this.sharedCount = quest.getSharedCount();
        this.commentCount = quest.getCommentCount();
        this.views = quest.getViews();
        this.savedCount = quest.getSavedCount();
        this.category = quest.getPillar();
        this.description = quest.getQuestFeed();
        this.mode = quest.getMode() == null ? null : quest.getMode().getKey();
        this.multiSellerEnabled = quest.isMultiSellerEnabled();
        this.startBtnDisabled = quest.isStartBtnDisabled();
        this.backBtnDisabled = quest.isBackBtnDisabled();
        this.geoTriggerEnabled = quest.isGeoTriggerEnabled();
        this.surveysEnabled = quest.isSurveysEnabled();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @JsonProperty("status")
    public String getStatus() {
        return activityData == null ? null : activityData.status;
    }

    @JsonProperty("activityMode")
    public String getActivityMode() {
        return activityData == null ? null : activityData.mode;
    }

    @JsonProperty("repeatable")
    public boolean isRepeatable() {
        return activityData != null && activityData.repeatable;
    }

    @JsonProperty("repeatCount")
    public int getRepeatCount() {
        return activityData == null || !activityData.repeatable ? 0 : activityData.repeatCount;
    }

    @JsonProperty("followed")
    public boolean isFollowed() {
        return userFlags != null && userFlags.followed;
    }

    @JsonProperty("saved")
    public boolean isSaved() {
        return userFlags != null && userFlags.saved;
    }

    @JsonProperty("starred")
    public boolean isStarred() {
        return userFlags != null && userFlags.starred;
    }

    public static QuestDTO toDTO(final Quests quest) {
        if (quest == null) {
            return null;
        }
        return new QuestDTO(quest);
    }

    public static List<QuestDTO> listToDTO(final List<Quests> quests) {
        if (Util.isEmpty(quests)) {
            return emptyList();
        }
        return quests.stream().map(QuestDTO::toDTO).filter(Objects::nonNull).collect(toList());
    }

    public QuestDTO withActivityData(final QuestActivityDTO activityData) {
        this.activityData = activityData;
        return this;
    }

    public QuestDTO withUserFlags(final QuestUserFlagsDTO userFlags) {
        this.userFlags = userFlags;
        return this;
    }

    public QuestDTO withSEOSlugs(final QuestSEOSlugs seoSlugs) {
        this.seoSlugs = seoSlugs;
        return this;
    }

    public QuestDTO withUserDoing(final boolean isUserDoing) {
        this.isUserDoing = isUserDoing;
        return this;
    }

    public QuestDTO withHasEvent(final boolean hasEvent) {
        this.hasEvent = hasEvent;
        return this;
    }

    public QuestDTO withIsQuestFundraiser(final boolean isFundraised) {
        this.isQuestFundraiser = isFundraised;
        return this;
    }
}
