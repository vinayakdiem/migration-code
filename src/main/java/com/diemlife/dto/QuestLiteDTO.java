package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotNull;

import com.diemlife.models.Quests;
import com.diemlife.utils.URLUtils.QuestSEOSlugs;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestLiteDTO implements Serializable {

    @JsonProperty
    public final Integer id;
    @JsonProperty
    public final String pillar;
    @JsonProperty
    public final String questFeed;
    @JsonProperty
    public final String title;
    @JsonProperty
    public Date dateCreated;
    @JsonProperty
    public String shortDescription;

    @JsonProperty
    public String photo = null;
    @JsonProperty
    public Integer views = null;
    @JsonProperty
    public UserAvatarDTO user = null;
    @JsonProperty
    public QuestSEOSlugs seoSlugs = null;

    @JsonCreator
    public QuestLiteDTO(final @NotNull Integer id,
                        final @NotNull String pillar,
                        final @NotNull String questFeed,
                        final @NotNull String title,
                        final @NotNull Date dateCreated,
                        final @NotNull String shortDescription) {
        this.id = id;
        this.pillar = pillar;
        this.questFeed = questFeed;
        this.title = title;
        this.dateCreated = dateCreated;
        this.shortDescription = shortDescription;
    }

    public QuestLiteDTO withPhoto(final String photo) {
        this.photo = photo;
        return this;
    }

    public QuestLiteDTO withViews(final Integer views) {
        this.views = views;
        return this;
    }

    public QuestLiteDTO withUser(final UserAvatarDTO user) {
        this.user = user;
        return this;
    }

    public QuestLiteDTO withSeoSlugs(final QuestSEOSlugs seoSlugs) {
        this.seoSlugs = seoSlugs;
        return this;
    }

    public static QuestLiteDTO toDTO(final Quests quest) {
        return new QuestLiteDTO(quest.getId(), quest.getPillar(), quest.getQuestFeed(), quest.getTitle(), quest.getDateCreated(), quest.getShortDescription())
                .withPhoto(quest.getPhoto())
                .withViews(quest.getViews())
                .withUser(new UserAvatarDTO(quest.getUser().getId(), quest.getUser().getFirstName(), quest.getUser().getLastName())
                        .withPhoto(quest.getUser().getProfilePictureURL()));
    }

}
