/*package com.diemlife.dto;

import static constants.QuestMemberStatus.Unknown;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.upperCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.diemlife.constants.QuestMemberStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.Geometry;

import forms.QuestActionPointForm;

public class QuestMemberDTO implements Serializable {
    public Integer questId;
    public Integer userId;
    public String userName;
    public String userFullName;
    public String profilePictureURL;
    public String firstName;
    public String lastName;
    public String backerDisplayName;
    public String zipCode;
    public QuestActionPointForm geopoint;
    public String location;
    public boolean isUserBrand;
    public boolean isAnonymous;
    public boolean isOffline;
    public Number completeTasksCount;
    public Number totalTasksCount;
    public Number repeatsCount;
    public List<Number> amountBacked = new ArrayList<>();
    public Set<QuestMemberStatus> memberStatus = new LinkedHashSet<>();
    public Number teamId;

    public QuestMemberDTO(final Integer questId,
                          final Integer userId,
                          final String userName,
                          final String userFullName,
                          final String profilePictureURL,
                          final String firstName,
                          final String lastName,
                          final String backerDisplayName,
                          final String zipCode,
                          final String city,
                          final String state,
                          final Geometry geometry,
                          final String isUserBrand,
                          final Boolean isAnonymous,
                          final Boolean isOffline,
                          final Number completeTasksCount,
                          final Number totalTasksCount,
                          final Number repeatsCount,
                          final Number amountBacked,
                          final String memberStatus,
                          final Number teamId) {
        this.questId = questId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.backerDisplayName = isNotBlank(backerDisplayName) ? backerDisplayName.trim() : null;
        this.isUserBrand = toBoolean(upperCase(isUserBrand), "Y", "N");
        this.userName = userName;
        this.userFullName = userFullName;
        this.zipCode = zipCode;
        this.location = isNotBlank(state) ? (city + ", " + state) : city;
        if (geometry != null && geometry.getCoordinate() != null) {
            this.geopoint = new QuestActionPointForm(((Double) geometry.getCoordinate().x).floatValue(), ((Double) geometry.getCoordinate().y).floatValue());
        }
        this.profilePictureURL = profilePictureURL;
        this.isAnonymous = isAnonymous;
        this.isOffline = isOffline;
        this.completeTasksCount = completeTasksCount;
        this.totalTasksCount = totalTasksCount;
        this.repeatsCount = repeatsCount;
        this.amountBacked.add(amountBacked);
        this.memberStatus.add(Stream.of(QuestMemberStatus.values())
                .filter(status -> status.name().equals(memberStatus))
                .findFirst()
                .orElse(Unknown));
        this.teamId = teamId;
    }

    public QuestMemberDTO(final Integer questId,
                          final Integer userId,
                          final Number amountBacked,
                          final String memberStatus,
                          Number teamId) {
        this(questId, userId, null, null, null, null, null, null, null, null, null, null, "N", false, false, 0, 0, 0, amountBacked, memberStatus, teamId);
    }

    @JsonIgnore
    public void addStatus(final QuestMemberStatus status) {
        this.memberStatus.add(status);
    }

    @JsonIgnore
    public void addBacking(final Number amount) {
        this.amountBacked.add(amount);
    }

    public int getSortingScore() {
        return this.memberStatus.stream().mapToInt(QuestMemberStatus::getScore).sum() - (isAnonymous ? 1 : 0);
    }

    public Integer getUserId() {
        return this.userId;
    }

}
*/