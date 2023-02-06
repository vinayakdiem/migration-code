package com.diemlife.models;

import com.diemlife.constants.ActivityEventType;
import com.diemlife.constants.ActivityUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Activity {
    private String uid;
    private long ts;
    private ActivityEventType eventType;
    private String idType;
    private int sequence;
    private String username;
    private Long teamId;
    private Long questId;
    private Long taskId;
    private Double lat;
    private Double lon;
    private String msg;
    private String comment;
    private String commentImgUrl;
    private String targetActivityUid;
    private Long cheerCount;
    private String postalCode;
    private Boolean deleted;
    private Double quantity;
    private ActivityUnit unit;
    private String tag;

    // Returns true if this activity can be cheered
    public boolean isCheerable() {
        // Can't cheer a cheer ... but all else is ok?
        switch (eventType) {
            case USER_CREATED:
            case QUEST_CREATED:
            case QUEST_STARTED:
            case QUEST_JOINED:
            case QUEST_SUPPORTED:
            case QUEST_COMPLETED:
            case QUEST_CANCELED:
            case TASK_COMPLETED:
            case TEAM_CREATED:
            case TEAM_JOINED:
            case COMMENT:
            case REPLY:
                return true;
            case CHEER:
            default:
                return false;
        }
    }

    // Returns true if this activity can be replied to
    public boolean isCommentable() {
        // reply is a recursive thing than can be applied to almost all activities
        switch (eventType) {
            case COMMENT:
            case REPLY:
            case USER_CREATED:
            case QUEST_CREATED:
            case QUEST_STARTED:
            case QUEST_JOINED:
            case QUEST_SUPPORTED:
            case QUEST_COMPLETED:
            case QUEST_CANCELED:
            case TASK_COMPLETED:
            case TEAM_CREATED:
            case TEAM_JOINED:
                return true;
            case CHEER:
            default:
                return false;
        }
    }

    public boolean isCheer() {
        return ActivityEventType.CHEER.equals(eventType);
    }

    public boolean isEvent() {
       switch (eventType) {
            case USER_CREATED:
            case QUEST_CREATED:
            case QUEST_STARTED:
            case QUEST_JOINED:
            case QUEST_SUPPORTED:
            case QUEST_COMPLETED:
            case QUEST_CANCELED:
            case TASK_COMPLETED:
            case TEAM_CREATED:
            case TEAM_JOINED:
                return true;
            case COMMENT:
            case CHEER:
            case REPLY:
            default:
                return false;
        } 
    }

    public boolean isComment() {
        return ActivityEventType.COMMENT.equals(eventType);
    }

    public boolean isReply() {
        return ActivityEventType.COMMENT.equals(eventType);
    }

    public boolean isMetaActivity() {
        switch (eventType) {
            case CHEER:
            case REPLY:
                return true;
            case USER_CREATED:
            case QUEST_CREATED:
            case QUEST_STARTED:
            case QUEST_JOINED:
            case QUEST_SUPPORTED:
            case QUEST_COMPLETED:
            case QUEST_CANCELED:
            case TASK_COMPLETED:
            case TEAM_CREATED:
            case TEAM_JOINED:
            case COMMENT:
            default:
                return false;
        }  
    }
}
