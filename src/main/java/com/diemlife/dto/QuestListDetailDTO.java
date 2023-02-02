package com.diemlife.dto;

import java.io.Serializable;

import com.diemlife.constants.QuestMode;

public class QuestListDetailDTO implements Serializable {

    public final int questId;
    public final QuestMode activityMode;
    public final long completeTasks;
    public final long totalTasks;
    public final long creatorCompleteTasks;
    public final long creatorTotalTasks;

    public QuestListDetailDTO(final int questId,
                              final QuestMode activityMode,
                              final long completeTasks,
                              final long totalTasks,
                              final long creatorCompleteTasks,
                              final long creatorTotalTasks) {
        this.questId = questId;
        this.activityMode = activityMode;
        this.completeTasks = completeTasks;
        this.totalTasks = totalTasks;
        this.creatorCompleteTasks = creatorCompleteTasks;
        this.creatorTotalTasks = creatorTotalTasks;
    }

}
