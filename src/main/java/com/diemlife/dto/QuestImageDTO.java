package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;

import com.diemlife.models.QuestImage;
import com.diemlife.models.User;

public class QuestImageDTO implements Serializable {

    public Integer id;
    public String questImageUrl;
    public String caption;
    public Date createdDate;
    public Date lastModifiedDate;
    public UserDTO creator;

    public static QuestImageDTO toDTO(final QuestImage image) {
        final QuestImageDTO dto = new QuestImageDTO();
        dto.id = image.getId();
        dto.questImageUrl = image.getQuestImageUrl();
        dto.caption = image.getCaption();
        dto.createdDate = image.getCreatedDate();
        dto.lastModifiedDate = image.getLastModifiedDate();
        return dto;
    }

    public QuestImageDTO withCreator(final User user) {
        this.creator = UserDTO.toDTO(user);
        return this;
    }

}
