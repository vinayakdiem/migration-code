package com.diemlife.utils;

import java.util.Date;

/**
 * Created by andrew on 5/14/17.
 */
public class QuestAndUserResponse {
        public Integer id;
        public String questFeed;
        public String pillar;
        public Integer sharedCount;
        public Integer savedCount;
        public Integer commentCount;
        public Integer createdBy;
        public Date dateCreated;
        public Integer modifiedBy;
        public Date dateModified;
        public String title;
        public String metaTags;
        public String notes;
        public String photo;
        //UserRelated
        public Integer userId;
        public String email;
        public String name;
        public String firstName;
        public String lastName;
        public String userName;
        public Date createdOn;
        public Date updatedOn;
        public String profilePictureURL;
        public String missionStatement;
        public String type;

    public QuestAndUserResponse(Integer id, String questFeed, String pillar, Integer sharedCount, Integer savedCount, Integer commentCount, Integer createdBy, Date dateCreated,
                                Integer modifiedBy, Date dateModified, String title, String metaTags, String notes, String photo, String type, Integer userId, String email, String name, String firstName,
                                String lastName, String userName, Date createdOn, Date updatedOn, String profilePictureURL, String missionStatement) {
        this.id = id;
        this.questFeed = questFeed;
        this.pillar = pillar;
        this.sharedCount = sharedCount;
        this.savedCount = savedCount;
        this.commentCount = commentCount;
        this.createdBy = createdBy;
        this.dateCreated = dateCreated;
        this.modifiedBy = modifiedBy;
        this.dateModified = dateModified;
        this.title = title;
        this.metaTags = metaTags;
        this.notes = notes;
        this.photo = photo;
        this.type = type;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.profilePictureURL = profilePictureURL;
        this.missionStatement = missionStatement;
    }
}
