package com.diemlife.dto;

import java.io.Serializable;

import com.diemlife.models.Quests;
import com.diemlife.models.User;

public class QuestPermissionsDTO implements Serializable {

    public boolean editable;

    public QuestPermissionsDTO(final Quests quest, final User currentUser) {
        this.editable = quest != null
                && currentUser != null
                && (quest.getCreatedBy().equals(currentUser.getId()) || quest.getAdmins().stream().anyMatch(admin -> admin.getEmail().equalsIgnoreCase(currentUser.getEmail()))
        );
    }

}
