package com.diemlife.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class ActivityDTO implements Serializable {
    private String uid;
    private String activityType;
    private long ts;

    // TODO: change to location
    private Double lat;
    private Double lon;
    private String postalCode;

    private String username;
    private String firstname;
    private String lastname;

    private String profilePictureURL;

    private Long questId;
    private String questTitle;

    private Long taskId;
    private String task;

    private Long teamId;
    private String teamName;

    private Long cheers;

    private String comment;

    private String commentImgUrl;

    private String targetActivity;

    private Boolean deleted;

    private Boolean cheeredByUser;

    private Double quantity;
    private String unit;
    private String tag;
}
