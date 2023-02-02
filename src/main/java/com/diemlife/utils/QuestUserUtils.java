/*package com.diemlife.utils;

import java.util.Optional;

import javax.persistence.EntityManager;

import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestTeamMember;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

import dao.QuestActivityHome;
import dao.QuestTeamDAO;

public abstract class QuestUserUtils {

    private QuestUserUtils() {
        super();
    }

    public static Optional<User> getPrincipleQuestUser(final Quests quest, final User currentUser, final EntityManager entityManager) {
        if (quest == null) {
            return Optional.empty();
        }
        final QuestActivity activity = currentUser == null
                ? null
                : QuestActivityHome.getQuestActivityForQuestIdAndUser(quest, currentUser, entityManager);
        if (activity == null) {
            switch (quest.getMode()) {
                case PACE_YOURSELF:
                case SUPPORT_ONLY:
                    return Optional.of(quest.getUser());
                case TEAM:
                    final QuestTeamMember member = new QuestTeamDAO(entityManager).getTeamMember(quest, currentUser, false);
                    return member == null ? Optional.of(quest.getUser()) : Optional.of(member.getTeam().getCreator());
                default:
                    return Optional.empty();
            }
        } else {
            switch (quest.getMode()) {
                case PACE_YOURSELF:
                    return Optional.of(currentUser);
                case TEAM:
                    final QuestTeamMember member = new QuestTeamDAO(entityManager).getTeamMember(quest, currentUser);
                    return member == null ? Optional.of(quest.getUser()) : Optional.of(member.getTeam().getCreator());
                case SUPPORT_ONLY:
                    return Optional.of(quest.getUser());
                default:
                    return Optional.empty();
            }
        }
    }

}
*/