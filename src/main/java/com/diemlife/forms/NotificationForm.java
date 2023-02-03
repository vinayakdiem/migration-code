package com.diemlife.forms;

import play.data.validation.Constraints;
import play.data.validation.Constraints.Required;

public class NotificationForm {

    private Integer questId;
    @Required
    private String notificationType;

    public Integer getQuestId() {
        return questId;
    }

    public void setQuestId(Integer questId) {
        this.questId = questId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
}
