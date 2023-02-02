package com.diemlife.dto;

import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;

import java.io.Serializable;
import java.util.Date;

import com.diemlife.models.Notification;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

public class NotificationDTO implements Serializable {

    public int id;
    public int userId;
    public String type;
    public boolean isRead;
    public Date createdDate;
    public String message;
    public UserDTO user;
    public QuestDTO quest;
    public CommentsDTO comment;

    public static NotificationDTO toDTO(final Notification notification,
                                        final User user,
                                        final Quests quest,
                                        final String environmentUrl) {
        if (notification == null) {
            return null;
        }
        final NotificationDTO dto = new NotificationDTO();
        dto.id = notification.getId();
        dto.userId = notification.getUserId();
        dto.type = notification.getType();
        dto.isRead = notification.isRead();
        dto.createdDate = notification.getCreatedDate();
        dto.message = notification.getMessage();
        dto.user = UserDTO.toDTO(user);
        dto.quest = QuestDTO.toDTO(quest);

        if (quest != null && user != null) {
        	com.diemlife.dto.quest.seoSlugs = publicQuestSEOSlugs(dto.quest, dto.user, environmentUrl);
        }

        return dto;
    }

    public NotificationDTO withComment(final CommentsDTO comment) {
        this.comment = comment;
        return this;
    }

}
