package com.diemlife.utils;

import static com.diemlife.constants.QuestActivityStatus.IN_PROGRESS;
import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.TEAM;
import static java.util.Arrays.asList;

import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

public class QuestSecurityUtils {

    public static boolean canCheckMilestone(final QuestTasks milestone, final User user) {
        return user != null && user.getId().equals(milestone.getUserId());
    }

    public static boolean canEditQuest(final Quests quest, final User user) {
        return user != null && (user.getId().equals(quest.getCreatedBy())
                || quest.getAdmins().stream().anyMatch(admin -> admin.getId().equals(user.getId())));
    }

    public static boolean canManageTasksInQuest(final Quests quest, final User user, final QuestActivity activity) {
        if (canEditQuest(quest, user)) {
            return true;
        }
        return editableMilestonesRuleAppliesTo(quest, activity);
    }

    private static boolean editableMilestonesRuleAppliesTo(final Quests quest, final QuestActivity activity) {
        return activity != null
                && IN_PROGRESS.equals(activity.getStatus())
                && quest.isEditableMilestones()
                && asList(PACE_YOURSELF, TEAM).contains(activity.getMode());
    }

}
