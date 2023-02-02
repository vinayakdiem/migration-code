package com.diemlife.dao;

import acl.QuestsListWithACL;
import constants.QuestActivityStatus;
import constants.QuestMode;
import dto.QuestActivityDTO;
import models.QuestActivity;
import models.Quests;
import models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static acl.QuestsListWithACL.emptyListWithACL;
import static constants.QuestActivityStatus.COMPLETE;
import static constants.QuestActivityStatus.IN_PROGRESS;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Created by andrewcoleman on 8/22/16.
 */
public class QuestActivityHome {

    public static void startQuestForUser(final Integer questId, final Integer userId, final QuestMode mode, final EntityManager em) {

        final Date date = new Date();
        final QuestActivity questActivity = new QuestActivity();
        questActivity.setQuestId(questId);
        questActivity.setUserId(userId);
        questActivity.setAddedDate(new Timestamp(date.getTime()));
        questActivity.setLastModifiedDate(new Timestamp(date.getTime()));
        questActivity.setStatus(QuestActivityStatus.IN_PROGRESS);
        questActivity.setMode(mode);
        //Persist the new Quest Activity record to the DB
        try {
            em.persist(questActivity);
        } catch (final PersistenceException e) {
            Logger.error("QuestActivityHome :: startQuestForUser : error persisting quest activity => " + e, e);
        }
    }

    public static void completeQuestForUser(Quests quest, User user, EntityManager em) {
        final Date date = new Date();
        try {
            final QuestActivity questActivity = getQuestActivityForQuestIdAndUser(quest, user, em);

            if (questActivity != null) {
                questActivity.setStatus(COMPLETE);
                questActivity.setCyclesCounter(questActivity.getCyclesCounter() + 1);
                questActivity.setLastModifiedDate(new Timestamp(date.getTime()));
                em.merge(questActivity);

                quest.setDateModified(date);
                em.merge(quest);
            }
        } catch (final PersistenceException ex) {
            Logger.error(format("QuestActivityHome:: completeQuestForUsers : error completing quest [%s] for user [%s]", quest.getId(), user.getId()), ex);
        }
    }

    public static boolean repeatQuestForUser(final Quests quest, final User user, final EntityManager em) {
        final Date date = new Date();
        try {
            final QuestActivity questActivity = getQuestActivityForQuestIdAndUser(quest, user, em);

            if (questActivity == null) {
                Logger.warn(format("Quest [%s] cannot be repeated for user [%s] for no activity", quest.getId(), user.getId()));

                return false;
            } else {
                if (COMPLETE.equals(questActivity.getStatus())) {
                    questActivity.setStatus(IN_PROGRESS);
                    questActivity.setLastModifiedDate(new Timestamp(date.getTime()));
                    em.merge(questActivity);

                    quest.setDateModified(date);
                    em.merge(quest);

                    return true;
                } else {
                    Logger.warn(format("Quest [%s] cannot be repeated for user [%s] as it's not completed", quest.getId(), user.getId()));

                    return false;
                }
            }
        } catch (final PersistenceException ex) {
            Logger.error(format("QuestActivityHome:: completeQuestForUsers : error completing quest [%s] for user [%s]", quest.getId(), user.getId()), ex);

            return false;
        }
    }

    public static List<QuestActivityDTO> getRepeatableInfoForDoer(final User doer, final EntityManager em) {
        if (doer == null || doer.getId() == null) {
            return emptyList();
        }
        return em.createQuery("SELECT " +
                "NEW dto.QuestActivityDTO(" +
                "  qa.questId," +
                "  qa.userId," +
                "  qa.status," +
                "  qa.mode," +
                "  CASE WHEN sum(coalesce(e.id, 0)) = 0 THEN TRUE ELSE FALSE END, " +
                "  qa.cyclesCounter" +
                ") " +
                "FROM QuestActivity qa " +
                "LEFT OUTER JOIN Happenings e ON e.quest.id = qa.questId " +
                "WHERE qa.userId = :doerId " +
                "GROUP BY qa.questId " +
                "ORDER BY qa.lastModifiedDate DESC", QuestActivityDTO.class)
                .setParameter("doerId", doer.getId())
                .getResultList();
    }

    public static QuestActivityDTO getRepeatableInfoForQuestAndDoer(final Quests quest, final User doer, final EntityManager em) {
        if (quest == null || quest.getId() == null || doer == null || doer.getId() == null) {
            return null;
        }
        return em.createQuery("SELECT " +
                "NEW dto.QuestActivityDTO(" +
                "  qa.questId," +
                "  qa.userId," +
                "  qa.status," +
                "  qa.mode," +
                "  CASE WHEN sum(coalesce(e.id, 0)) = 0 THEN TRUE ELSE FALSE END, " +
                "  qa.cyclesCounter" +
                ") " +
                "FROM QuestActivity qa " +
                "LEFT OUTER JOIN Happenings e ON e.quest.id = qa.questId " +
                "WHERE qa.userId = :doerId AND qa.questId = :questId " +
                "GROUP BY qa.questId " +
                "ORDER BY qa.lastModifiedDate DESC", QuestActivityDTO.class)
                .setParameter("questId", quest.getId())
                .setParameter("doerId", doer.getId())
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static QuestsListWithACL getInProgressQuestsForUser(User user, EntityManager em, Integer pageNumber, Integer pageSize) {
        if (user == null) {
            return emptyListWithACL();
        }
        try {
            TypedQuery<Quests> query = em.createQuery("SELECT distinct q " +
                            "from Quests q, QuestActivity qa " +
                            "where qa.questId = q.id " +
                            "and qa.userId = :userId " +
                            "and qa.status = 'IN_PROGRESS'",
                    Quests.class);
            if (pageNumber != null && pageSize != null) {
                query = em.createQuery("SELECT distinct q " +
                                        "from Quests q, QuestActivity qa " +
                                        "where qa.questId = q.id " +
                                        "and qa.userId = :userId " +
                                        "and qa.status = 'IN_PROGRESS'",
                                Quests.class)
                        .setFirstResult((pageNumber-1) * pageSize)
                        .setMaxResults(pageSize);
            }

            query.setParameter("userId", user.getId());
            return new QuestsListWithACL(query::getResultList, em);
        } catch (final PersistenceException e) {
            Logger.error("Error finding in progress quests ::  Exception => " + e, e);
            return emptyListWithACL();
        }
    }

    public static QuestsListWithACL getCompletedQuestsForUser(User user, EntityManager em, Integer pageNumber, Integer pageSize) {
        if (user == null) {
            return emptyListWithACL();
        }
        try {
            TypedQuery<Quests> query = em.createQuery("SELECT distinct q " +
                            "FROM Quests q " +
                            "INNER JOIN QuestActivity qa ON qa.questId = q.id " +
                            "WHERE qa.userId = :userId " +
                            "AND (qa.status = 'COMPLETE' OR (qa.status = 'IN_PROGRESS' AND qa.cyclesCounter > 0)) " +
                            "ORDER BY qa.lastModifiedDate DESC",
                    Quests.class);
            if (pageNumber != null && pageSize != null) {
                query = em.createQuery("SELECT distinct q " +
                                        "FROM Quests q " +
                                        "INNER JOIN QuestActivity qa ON qa.questId = q.id " +
                                        "WHERE qa.userId = :userId " +
                                        "AND (qa.status = 'COMPLETE' OR (qa.status = 'IN_PROGRESS' AND qa.cyclesCounter > 0)) " +
                                        "ORDER BY qa.lastModifiedDate DESC",
                                Quests.class)
                        .setFirstResult((pageNumber-1) * pageSize)
                        .setMaxResults(pageSize);
            }

            query.setParameter("userId", user.getId());
            return new QuestsListWithACL(query::getResultList, em);
        } catch (final PersistenceException e) {
            Logger.error("Error finding in progress quests ::  Exception => " + e, e);
            return emptyListWithACL();
        }
    }

    public static QuestsListWithACL getQuestsNotInProgressForUser(User user, Integer limit, EntityManager em) {
        try {
            if (user != null) {
                // get user friends to check which quests to show
                List<Integer> friends = UserRelationshipDAO.getCurrentFriendsByUserId(user.getId(), em);
                Logger.info("current friends = " + Arrays.toString(friends.toArray()));
                // ensure that user's friends are not brands
                List<Integer> userFriends = UserHome.getUsersByIdWithBrand(friends, em);
                Logger.info("user friends = " + Arrays.toString(userFriends.toArray()));

                long startTime = System.currentTimeMillis();
                Logger.info("Getting quests not in progress for user => " + user.getId() + " StartTime = " + startTime);
                final TypedQuery<Quests> query = em.createQuery("SELECT q from Quests q " +
                        "WHERE q.id NOT IN (SELECT qa.questId FROM QuestActivity qa WHERE qa.userId = :userId) " +
                        "AND q.id NOT IN (SELECT qs.questId FROM QuestSaved qs WHERE qs.userId = :userId) " +
                        (userFriends.isEmpty() ? "" : "AND q.createdBy IN (:userFriends) ") +
                        "ORDER BY q.sharedCount desc, q.savedCount desc", Quests.class);
                query.setParameter("userId", user.getId());
                if (!userFriends.isEmpty()) {
                    query.setParameter("userFriends", userFriends);
                }
                if (limit != null) {
                    query.setMaxResults(limit);
                }
                query.setHint("org.hibernate.readOnly", true);
                query.setHint("org.hibernate.cacheable", true);
                query.setHint("javax.persistence.cache.retrieveMode", javax.persistence.CacheRetrieveMode.USE);
                query.setHint("org.hibernate.fetchSize", 9);
                List<Quests> quests = query.getResultList();
                long stopTime = System.currentTimeMillis();
                long elapsedTime = ((stopTime - startTime) / 1000);
                Logger.info("Finished quest fetch, time elapsed: " + elapsedTime);

                return new QuestsListWithACL(() -> quests, em);
            }
        } catch (final PersistenceException e) {
            Logger.error("Error getting quests for user => " + user.getId() + " ex => " + e, e);
            return emptyListWithACL();
        }
        return emptyListWithACL();
    }

    public static QuestsListWithACL getQuestsNotInProgressByCategory(final User user, final int limit, final String category, EntityManager em) {
        if (user == null) {
            final TypedQuery<Quests> query = em.createQuery("SELECT q from Quests q " +
                    "WHERE q.pillar = :category " +
                    "ORDER BY q.sharedCount desc, q.savedCount desc", Quests.class);
            query.setParameter("category", category);
            query.setMaxResults(limit);
            return new QuestsListWithACL(() -> query.getResultList(), em);
        } else {
            List<Integer> friends = UserRelationshipDAO.getCurrentFriendsByUserId(user.getId(), em);
            List<Integer> userFriends = UserHome.getUsersByIdWithBrand(friends, em);

            final TypedQuery<Quests> query = em.createQuery("SELECT q from Quests q " +
                    "WHERE q.id NOT IN (SELECT qa.questId FROM QuestActivity qa WHERE qa.userId = :userId) " +
                    "AND q.id NOT IN (SELECT qs.questId FROM QuestSaved qs WHERE qs.userId = :userId) " +
                    (userFriends.isEmpty() ? "" : "AND q.createdBy IN (:userFriends) ") +
                    "AND q.pillar = :category " +
                    "ORDER BY q.sharedCount desc, q.savedCount desc", Quests.class);
            query.setParameter("userId", user.getId());
            query.setParameter("category", category);
            if (!userFriends.isEmpty()) {
                query.setParameter("userFriends", userFriends);
            }
            query.setMaxResults(limit);

            return new QuestsListWithACL(query::getResultList, em);
        }
    }

    public static QuestsListWithACL getQuestsNotInProgressForUserPaginated(User user, int start, int limit, List<String> pillars, String category, String place, EntityManager em) {
        List<Integer> friends = user == null ? emptyList() : UserRelationshipDAO.getCurrentFriendsByUserId(user.getId(), em);
        List<Integer> userFriendsWithBrands = UserHome.getUsersByIdWithBrand(friends, em);

        try {
            final TypedQuery<Quests> query = em.createQuery("SELECT q FROM Quests q " +
                    "WHERE q.id NOT IN " +
                    (user == null ? "(0) " : "(SELECT qa.questId FROM QuestActivity qa WHERE qa.userId = :userId AND qa.status IN ('IN_PROGRESS', 'COMPLETE')) ") +
                    (!isEmpty(pillars) ? "AND q.pillar in ( :pillars ) " : "") +
                    (!isBlank(category) ? "AND q.category = :category " : "") +
                    (!isBlank(place) ? "AND q.place = :place " : "") +
                    "AND q.id NOT IN " +
                    (user == null ? "(0) " : "(SELECT qs.questId FROM QuestSaved qs WHERE qs.userId = :userId) ") +
                    "AND ((q.privacyLevel = 'PUBLIC') " +
                    (isEmpty(userFriendsWithBrands)
                            ? ") "
                            : "OR (q.privacyLevel = 'FRIENDS' AND q.createdBy IN :userFriendsWithBrands)) ") +
                    "ORDER BY CASE WHEN q.weight > 0 THEN (-LOG(1.0 + q.weight) * LOG(1.0 - (RAND() + RAND()) / 2) * 100) ELSE (LOG(1.0 - RAND()) * 100) END DESC", Quests.class);
            query.setFirstResult(start);
            query.setMaxResults(limit + 1);
            if (user != null) {
                query.setParameter("userId", user.getId());
            }
            if (!isEmpty(pillars)) {
                query.setParameter("pillars", pillars);
            }
            if (!isBlank(category)) {
                query.setParameter("category", category);
            }
            if (!isBlank(place)) {
                query.setParameter("place", place);
            }
            if (!isEmpty(userFriendsWithBrands)) {
                query.setParameter("userFriendsWithBrands", userFriendsWithBrands);
            }

            List<Quests> quests = query.getResultList();
            return new QuestsListWithACL(() -> quests, em);
        } catch (PersistenceException pe) {
            Logger.error("Error getting paginated quests", pe);
            return emptyListWithACL();
        }
    }

    public static QuestActivity getQuestActivityForQuestIdAndUser(final Quests quest, final User currentUser, final EntityManager em) {
        if (quest == null || currentUser == null) {
            return null;
        }

        return em.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.questId = :questId AND qa.userId = :userId ORDER BY qa.addedDate DESC", QuestActivity.class)
                .setParameter("questId", quest.getId())
                .setParameter("userId", currentUser.getId())
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static boolean doesQuestActivityExistForUserIdAndQuestId(Connection c, Long questId, Long userId) {
        if ((questId != null) && (userId != null)) {
            try (PreparedStatement ps = c.prepareStatement("select 1 from quest_activity where user_id = ? and quest_id = ? limit 1")) {
                ps.setLong(1, userId);
                ps.setLong(2, questId);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.first();
                }
            } catch (Exception e) {
                Logger.error("doesQuestActivityExistForUserIdAndQuestId - error", e);
            }
        }

        return false;
    }

    public static void removeAllQuestActivity(Integer questId, Integer userId, EntityManager em) {
        if (questId == null || userId == null) {
            Logger.warn("Empty parameters for all Quest activity removal - skipping");
            return;
        }
        try {
            Logger.info(format("Removing user's ID [%s] activity on Quest with ID [%s]", userId, questId));
            em.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.questId = :questId AND qa.userId = :userId", QuestActivity.class)
                    .setParameter("questId", questId)
                    .setParameter("userId", userId)
                    .getResultList()
                    .forEach(em::remove);
        } catch (final PersistenceException ex) {
            Logger.info("QuestActivityHome :: removeAllQuestActivity => " + ex);
        }
    }

    public static List<QuestActivity> getRecentActivityPending(Integer limit, Integer offset, Integer userId, EntityManager em) {

        try {

            // get user friends to check which quests to show
            List<Integer> friends = UserRelationshipDAO.getCurrentFriendsByUserId(userId, em);

            em.setFlushMode(FlushModeType.AUTO);
            Query query = em.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.status = 'IN_PROGRESS' " +
                    "AND qa.userId NOT IN :userId AND qa.userId IN (:friends) ORDER BY qa.addedDate DESC");
            query.setParameter("userId", userId);
            query.setParameter("friends", isEmpty(friends) ? singletonList(Integer.MIN_VALUE) : friends);
            query.setHint("org.hibernate.cacheable", true);
            query.setHint("org.hibernate.readOnly", true);
            query.setFirstResult(offset);
            query.setMaxResults(limit);

            return (List<QuestActivity>) query.getResultList();
        } catch (Exception ex) {
            Logger.error("QuestActivityHome :: getRecentActivity :: Error attempting to find most recent quests pending => " + ex, ex);
            return emptyList();
        }
    }

    public static List<QuestActivity> getRecentActivityCompleted(Integer limit, Integer userId, EntityManager em) {

        try {
            // get user friends to check which quests to show
            List<Integer> friends = UserRelationshipDAO.getCurrentFriendsByUserId(userId, em);

            em.setFlushMode(FlushModeType.AUTO);
            Query query = em.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.status = 'COMPLETE' " +
                    "AND qa.userId NOT IN :userId AND qa.userId IN (:friends) ORDER BY qa.addedDate DESC");
            query.setParameter("userId", userId);
            query.setParameter("friends", friends);
            query.setHint("org.hibernate.cacheable", true);
            query.setHint("org.hibernate.readOnly", true);
            query.setMaxResults(limit);

            return (List<QuestActivity>) query.getResultList();
        } catch (Exception ex) {
            Logger.error("QuestActivityHome :: getRecentActivity :: Error attempting to find most recent quests completed => " + ex, ex);
            return emptyList();
        }
    }

    public static List<QuestActivity> getUsersDoingQuest(final Integer questId, final String group, final EntityManager em) {
        try {
        	
        	String SQL  = "SELECT qa FROM QuestActivity qa WHERE qa.questId = :questId AND qa.group=:group";
        	
        	if(group==null || group.length()==0 || group.equalsIgnoreCase("ALL")) {
        		SQL  = "SELECT qa FROM QuestActivity qa WHERE qa.questId = :questId";
        	}
            
        	Query query = em.createQuery(SQL);
            query.setParameter("questId", questId);
            
            if(group!=null && group.length()>0 && !group.equalsIgnoreCase("ALL")) {
            	query.setParameter("group", group);
            }
            return (List<QuestActivity>) query.getResultList();
        } catch (NoResultException e) {
            return emptyList();
        } catch (PersistenceException e) {
            throw new PersistenceException(e);
        }
    }

    public static void changeQuestActivityMode(final QuestActivity activity, final QuestMode mode, final EntityManager em) {
        if (activity != null && mode != null) {
            activity.setMode(mode);
            em.merge(activity);
        }
    }

    public static List<QuestActivity> getUsersAffiliatedWithQuest(final int questId, final EntityManager entityManager) {
        return entityManager.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.questId = :questId", QuestActivity.class)
                .setParameter("questId", questId)
                .getResultList();
    }

    public static List<QuestActivity> getUsersAffiliatedWithQuestByStatus(String questActivityStatus,
                                                                          final int questId, EntityManager entityManager) {
        return entityManager.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.status = :status AND qa.questId = :questId", QuestActivity.class)
                .setParameter("status", QuestActivityStatus.valueOf(questActivityStatus))
                .setParameter("questId", questId)
                .getResultList()
                .stream()
                .collect(Collectors.toList());
    }

    public static List<QuestActivity> getUsersCompletedWithQuest(final int questId, EntityManager entityManager) {
        return entityManager.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.status = 'COMPLETE' AND qa.questId = :questId", QuestActivity.class)
                .setParameter("questId", questId)
                .getResultList()
                .stream()
                .collect(Collectors.toList());
    }

    public static List<QuestActivity> getUsersInGroup(final int questId, final String group, final EntityManager entityManager) {
        return entityManager.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.questId = :questId AND qa.group = :group", QuestActivity.class)
                .setParameter("questId", questId)
                .setParameter("group", group)
                .getResultList()
                .stream()
                .collect(Collectors.toList());
    }

    public static Quests getInProgressRealtimeQuestForUser(User user, EntityManager em) {
        if (user == null) {
            Logger.info("Cannot get realtime Quest id for null User");
            return null;
        }
        try {
            final TypedQuery<Quests> query = em.createQuery("SELECT q " +
                            "FROM Quests q " +
                            "INNER JOIN QuestActivity qa ON qa.questId = q.id " +
                            "WHERE qa.userId = :userId AND qa.status = 'IN_PROGRESS' AND q.realtime = true",
                    Quests.class);
            query.setParameter("userId", user.getId());

            if (query.getResultList().isEmpty()) {
                return null;
            } else {
                return query.getResultList().get(0);
            }

        } catch (final PersistenceException e) {
            Logger.error("Error finding realtime quests for user " + user.getId());
            return null;
        }
    }

    public static List<QuestActivity> getQuestActivitiesByQuest(final Integer questId, final EntityManager em) {
        
        return em.createQuery("SELECT qa FROM QuestActivity qa WHERE qa.questId = :questId AND qa.group IS NOT NULL GROUP BY qa.group", QuestActivity.class)
                .setParameter("questId", questId)
                .getResultList()
                .stream()
                .collect(Collectors.toList());
    }
}
