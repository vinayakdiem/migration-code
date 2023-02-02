package dao;

import exceptions.RequiredParameterMissingException;
import models.QuestInvite;
import models.Quests;
import models.User;

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
