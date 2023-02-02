package com.diemlife.utils;

import static org.apache.commons.lang3.StringUtils.upperCase;

import java.io.Serializable;

import com.diemlife.models.Brand;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

public class SearchResponse implements Serializable {

    public String type;
    public Integer userId;
    public String userFullName;
    public String userName;
    public String goals;
    public String profilePictureURL;
    //Quest Related
    public Integer questId;
    public String questName;
    public String description;
    public String questImageURL;
    public String pillar;
    public String status;

    public Float score;
    public String path;

    public Brand brand;
    
    public static SearchResponse fromQuest(final Quests quest) {
        final SearchResponse searchResponse = new SearchResponse();
        searchResponse.type = "quest";
        searchResponse.questId = quest.getId();
        searchResponse.questName = quest.getTitle();
        searchResponse.description = quest.getQuestFeed();
        searchResponse.questImageURL = quest.getPhoto();
        searchResponse.userId = quest.getCreatedBy();
        searchResponse.userName = quest.getUser().getName();
        searchResponse.pillar = upperCase(quest.getPillar());
        return searchResponse;
    }

    public static SearchResponse fromUser(final User user) {
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.type = "user";
        searchResponse.userId = user.getId();
        searchResponse.userFullName = user.getFirstName() + " " + user.getLastName();
        searchResponse.userName = user.getUserName();
        searchResponse.goals = user.getMissionStatement();
        searchResponse.profilePictureURL = user.getProfilePictureURL();
        return searchResponse;
    }

    public SearchResponse withScore(final Float score) {
        this.score = score;
        return this;
    }

    public SearchResponse withPath(final String path) {
        this.path = path;
        return this;
    }

	public Brand getBrand() {
		return brand;
	}

	public void setBrand(Brand brand) {
		this.brand = brand;
	}

}
