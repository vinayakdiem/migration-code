package dao;

import acl.QuestsListWithACL;
import dto.QuestMemberDTO;
import models.QuestSaved;
import models.Quests;
import models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

import static acl.QuestsListWithACL.emptyListWithACL;
import static java.util.Collections.emptyList;

/**
 * Created by acoleman1 on 6/15/17.
 */
public class QuestSavedDAO {

    public static void remove(QuestSaved persistentInstance, EntityManager em) {

        try {
            em.remove(persistentInstance);
        } catch (Exception e) {
            Logger.error("QuestTasksDAO :: remove : error removing quest task => " + e, e);
        }
    }

    public static void saveQuestForUser(Quests quest, User user, EntityManager em) {

        Date date = new Date();

        if (quest != null && user != null) {
            try {
                QuestSaved questSaved = new QuestSaved();
                questSaved.setUserId(user.getId());
                questSaved.setQuestId(quest.getId());
                questSaved.setAddedDate(date);
                questSaved.setLastModifiedDate(date);

                em.persist(questSaved);
            } catch (Exception ex) {
                Logger.error("QuestSavedDAO :: saveQuestForUser : error persisting quest saved => " + ex, ex);
            }

            try {
                if (quest.getSavedCount() != null) {
                    quest.setSavedCount(quest.getSavedCount() + 1);
                    em.merge(quest);
                }
            } catch (Exception ex) {
                Logger.error("QuestActivityHome :: startQuestForUser : error incrementing saved count => " + ex, ex);
            }
        }
    }

    public static void removeQuestForUser(Integer questId, Integer userId, EntityManager em) {
        if (questId == null || userId == null) {
            Logger.warn("QuestSavedDAO :: removeQuestForUser : empty arguments passed");
        }
        try {
            em.createQuery("SELECT qs " +
                    "FROM QuestSaved qs " +
                    "WHERE qs.questId = :questId AND qs.userId = :userId", QuestSaved.class)
                    .setParameter("questId", questId)
                    .setParameter("userId", userId)
                    .getResultList()
                    .forEach(em::remove);
        } catch (final PersistenceException e) {
            Logger.info("QuestSavedDAO :: removeQuestForUer : error removing quest for user => " + e.getMessage(), e);
        }
    }

    public static boolean doesQuestSavedExistForUser(Integer questId, Integer userId, EntityManager em) {
        if (questId == null || userId == null) {
            return false;
        }
        try {
            return em.createQuery("SELECT COUNT(qs) " +
                    "FROM QuestSaved qs " +
                    "WHERE qs.questId = :questId AND qs.userId = :userId", Long.class)
                    .setParameter("questId", questId)
                    .setParameter("userId", userId)
                    .getSingleResult() > 0;
        } catch (final PersistenceException e) {
            Logger.warn("QuestSavedDAO :: doesQuestSavedExistForUser : Error finding if saved quest exists for user => " + e.getMessage(), e);
            return false;
        }
    }

    public static QuestsListWithACL getSavedQuestsForUser(final User user, final EntityManager em) {
        if (user == null) {
            return emptyListWithACL();
        }
        try {
            final TypedQuery<Quests> query = em.createQuery("SELECT distinct q " +
                            "from Quests q, QuestSaved qs " +
                            "where qs.questId = q.id " +
                            "and qs.userId = :userId",
                    Quests.class);
            query.setParameter("userId", user.getId());
            return new QuestsListWithACL(query::getResultList, em);
        } catch (final PersistenceException e) {
            Logger.error("Error finding saved quests ::  Exception => " + e, e);
            return emptyListWithACL();
        }
    }

    public static List<Integer> getSavedQuestIdsForUser(final User user, final EntityManager em) {
        if (user == null) {
            return emptyList();
        }
        return em.createQuery("SELECT DISTINCT q.id FROM Quests q " +
                "INNER JOIN QuestSaved qs ON qs.questId = q.id " +
                "WHERE qs.userId = :userId", Integer.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    public static List<QuestMemberDTO> getSavedMembersForQuest(final Quests quest, final EntityManager em) {
        return getSavedMembersForQuest(quest.getId(), em);
    }

    public static List<QuestMemberDTO> getSavedMembersForQuest(final Integer questId, final EntityManager em) {
        if (questId == null) {
            return emptyList();
        }
        try {
            // Note: hibernate doesn't like null here so 0 is used in place of null.  Since there is no corresponding records with id = 0,
            // this is probably ok
            return em.createQuery("SELECT " +
                    "NEW dto.QuestMemberDTO(" +
                    "  qs.questId, " +
                    "  u.id, " +
                    "  u.userName, " +
                    "  u.name, " +
                    "  u.profilePictureURL, " +
                    "  u.firstName, " +
                    "  u.lastName, " +
                    "  '', " +
                    "  u.zip, " +
                    "  pc.city, " +
                    "  pc.state, " +
                    "  pc.point, " +
                    "  u.isUserBrand, " +
                    "  FALSE, " +
                    "  FALSE, " +
                    "  0, " +
                    "  0, " +
                    "  0, " +
                    "  0, " +
                    "  'Interested', " +
                    "  0" +
                    ") FROM QuestSaved qs " +
                    "INNER JOIN User u ON u.id = qs.userId " +
                    "LEFT OUTER JOIN PostalCodeUs pc ON pc.zip = u.zip " +
                    "WHERE qs.questId = :questId", QuestMemberDTO.class)
                    .setParameter("questId", questId)
                    .getResultList();
        } catch (final PersistenceException e) {
            Logger.error("Error getting saved members for Quest " + questId, e);
            return emptyList();
        }
    }

}
