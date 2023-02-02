package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.diemlife.models.Notification;

@Entity
public class GroupedNotificationDTO implements Serializable {

    public Long notificationsCount;
    @Id
    public Integer lastUserId;
    public String lastUserName;
    public Date createdDate;
    @Id
    public String notificationsType;
    @Id
    public Integer fromQuestId;
    public String fromQuestTitle;
    public Boolean isRead;
    public Long countFromUsers;
    public Integer fromComment;

    public GroupedNotificationDTO() {
    }

    public static Notification toNotification (GroupedNotificationDTO dto) {
        Notification notification = new Notification();
        notification.setFromQuest(dto.fromQuestId);
        notification.setRead(dto.isRead);
        notification.setUserId(dto.lastUserId);
        notification.setCreatedDate(dto.createdDate);
        notification.setType(dto.notificationsType);
        notification.setFromComment(dto.fromComment);

        return notification;
    }

}
