package com.diemlife.models;

import com.diemlife.constants.ActivityEventType;
import com.diemlife.constants.ActivityUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ActivityRaw {
    private long ts;
    private String idType;
    private int sequence;
    private ActivityEventType eventType;
    private String msg;
    private String username;
    private Long teamId;
    private Long questId;
    private Long taskId;
    private Double lat;
    private Double lon;
    private String comment;
    private String commentImgUrl;
    private Double quantity;
    private ActivityUnit unit;
    private String tag;
}
