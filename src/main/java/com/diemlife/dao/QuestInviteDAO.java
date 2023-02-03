package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.QuestInvite;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

import javax.persistence.EntityManager;
import java.util.List;

public class QuestInviteDAO extends TypedDAO<QuestInvite> {

    public QuestInviteDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public List<QuestInvite> getInvitesForQuest(final Quests quest) {
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        return entityManager
                .createQuery("SELECT qi FROM QuestInvites qi WHERE qi.quest.id = :questId", QuestInvite.class)
                .setParameter("questId", quest.getId())
                .getResultList();
    }

}
