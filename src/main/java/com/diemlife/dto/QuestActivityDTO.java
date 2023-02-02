package com.diemlife.dto;

import java.io.Serializable;

import com.diemlife.constants.QuestActivityStatus;
import com.diemlife.constants.QuestMode;

import lombok.ToString;

@ToString
public class QuestActivityDTO implements Serializable {

    public final int questId;
    public final int userId;
    public final String status;
    public final String mode;
    public final boolean repeatable;
    public final int repeatCount;

    public QuestActivityDTO(final int questId,
                            final int userId,
                            final QuestActivityStatus status,
                            final QuestMode mode,
                            final boolean repeatable,
                            final int repeatCount) {
        this.questId = questId;
        this.userId = userId;
        this.status = status == null ? null : status.name();
        this.mode = mode == null ? null : mode.getKey();
        this.repeatable = repeatable;
        this.repeatCount = repeatCount;
    }

}
