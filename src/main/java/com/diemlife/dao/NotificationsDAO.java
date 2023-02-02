package dao;

import constants.NotificationType;
import dto.GroupedNotificationDTO;
import models.Notification;
import models.User;
import play.Logger;
import play.cache.CacheApi;
import play.cache.NamedCache;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public class NotificationsDAO {

    private final CacheApi notificationsCache;

    @Inject
    public NotificationsDAO(@NamedCache("notifications-cache") final CacheApi notificationsCache) {
        this.notificationsCache = notificationsCache;
    }

    /**
     * A utility method to be used anytime we want to create a notification in the application
     *
     * @param userId
     * @param notificationType
     * @param fromUserId
     * @param fromQuestId
     */
    public void addNotification(final Integer userId, final NotificationType notificationType, final Integer fromUserId,
                                final Integer fromQuestId, final Integer fromCommentId, final EntityManager entityManager) {
        checkNotNull(userId, "userId");
        checkNotNull(notificationType, "notificationType");
        checkNotNull(entityManager, "entityManager");

        Logger.info(format("adding a new notification for user: [%s] of type: [%s]", userId, notificationType.name()));
        createNotification(userId, notificationType, fromUserId, fromQuestId, fromCommentId, entityManager);
        clearNotificationCache(userId, notificationsCache);
    }

    public void clearNotificationCache(final Integer userId, final CacheApi notificationsCache) {
        notificationsCache.remove(userId + "-notifications");
    }

    /**
     * Creates a notification for a given user
     *
     * @param userId           id of the user that the notification is for
     * @param notificationType type of notification being generated
     * @param fromUserId       if the notification is another user interacting with another, then fill this; else null
     * @param fromQuestId      if the notification is pertaining to a quest, then fill this; else null
     */
    private static void createNotification(final Integer userId, final NotificationType notificationType, final Integer fromUserId,
                                           final Integer fromQuestId, final Integer fromCommentId, final EntityManager entityManager) {
        if (!doesNotificationAlreadyExist(userId, notificationType, fromUserId, fromQuestId, fromCommentId, entityManager)) {
            try {
                Notification notification = new Notification();
                notification.setType(notificationType.name());
                notification.setUserId(userId);
                notification.setRead(false);
                notification.setCreatedDate(new Date());
                if (fromUserId != null) {
                    notification.setFromUser(fromUserId);
                }
                if (fromQuestId != null) {
                    notification.setFromQuest(fromQuestId);
                }
                if (fromCommentId != null) {
                    notification.setFromComment(fromCommentId);
                }
                entityManager.persist(notification);
            } catch (PersistenceException e) {
                Logger.error("NotificationsDAO :: createNotification : error persisting notification");
                throw new PersistenceException(e);
            }
        }
    }

    /**
     * Used the mark a notification as read for a given notificationId
     *
     * @param questId          id of the quest to be marked as read
     * @param notificationType type of the notification to be updated
     * @param isRead           boolean value holding whether the notification has been read or not
     */
    public void updateNotification(final String notificationType, final Integer questId, final boolean isRead, final EntityManager entityManager, final User user) {
        checkNotNull(notificationType, "notificationType");
        try {
            Date currentDate = new Date();

            Integer userId = user.getId();

            Query query = entityManager.createQuery("UPDATE Notification SET isRead = :isRead, readDate = :currentDate WHERE " + (questId == null ? "fromQuest IS NULL" : "fromQuest = :questId")
                    + " AND userId = :userId AND type = :notificationType AND isRead = false")
                    .setParameter("userId", userId)
                    .setParameter("notificationType", notificationType)
                    .setParameter("isRead", isRead)
                    .setParameter("currentDate", currentDate);

            if (questId == null) {
                query.executeUpdate();
            } else {
                query.setParameter("questId", questId)
                        .executeUpdate();
            }

            clearNotificationCache(userId, notificationsCache);

        } catch (IllegalStateException e) {
            Logger.info("NotificationsDAO :: updateNotification : operation not acceptable for this query");
        } catch (PersistenceException e) {
            Logger.error("NotificationsDAO :: updateNotification : error update notifications ");
            throw new PersistenceException(e);
        }
    }

    /**
     * gets all notifications for a given user
     *
     * @param userId the user id to get the unread notifications
     * @return a List of unread notifications for a given user
     */
    @SuppressWarnings("unchecked")
    public List<GroupedNotificationDTO> getGroupedNotifications(final Integer userId, final Integer startPos, final Integer endPos, final EntityManager entityManager) {
        checkNotNull(userId, "userId");
        try {
            return entityManager.createNativeQuery(
                    "SELECT n.from_user    AS lastUserId, " +
                            "       u.first_name   AS lastUserName, " +
                            "       j.count_ids    AS notificationsCount, " +
                            "       n.created_date AS createdDate, " +
                            "       n.type         AS notificationsType, " +
                            "       n.from_quest   AS fromQuestId, " +
                            "       q.title        AS fromQuestTitle, " +
                            "       j.is_read      AS isRead, " +
                            "       j.count_from_users AS countFromUsers, " +
                            "       j.from_comment_id  AS fromComment " +
                            "FROM notifications n " +
                            "         INNER JOIN (SELECT COUNT(nj.id)                        AS count_ids, " +
                            "                            COUNT(distinct nj.from_user)        AS count_from_users, " +
                            "                            MAX(nj.created_date)                AS max_created_date, " +
                            "                            CASE " +
                            "                                WHEN nj.type = 'COMMENT' OR nj.type = 'PHOTO_VIDEO_ADDED' THEN nj.type " +
                            "                                ELSE CONCAT(nj.type, nj.id) END AS grouping_type, " +
                            "                            CASE " +
                            "                                WHEN COUNT(nj.id) < 2 THEN nj.from_comment " +
                            "                                ELSE NULL END                   AS from_comment_id, " +
                            "                            nj.user_id, " +
                            "                            nj.type, " +
                            "                            nj.from_quest, " +
                            "                            nj.is_read " +
                            "                     FROM notifications nj " +
                            "                     WHERE nj.user_id = :userId " +
                            "                       AND nj.from_user != :userId " +
                            "                     GROUP BY nj.user_id, grouping_type, nj.from_quest, nj.is_read, nj.time_read) j " +
                            "                    ON j.user_id = n.user_id " +
                            "                        AND j.type = n.type " +
                            "                        AND j.from_quest = n.from_quest " +
                            "                        AND j.max_created_date = n.created_date " +
                            "         INNER JOIN user u " +
                            "                    ON u.Id = n.from_user " +
                            "         INNER JOIN quest_feed q " +
                            "                    ON q.id = n.from_quest " +
                            "         LEFT OUTER JOIN quest_comments qc " +
                            "                         ON qc.id = j.from_comment_id " +
                            "WHERE n.user_id = :userId " +
                            "  AND n.from_user != :userId " +
                            "ORDER BY n.created_date DESC",
                    GroupedNotificationDTO.class)
                    .setParameter("userId", userId)
                    .setFirstResult(startPos)
                    .setMaxResults(endPos)
                    .getResultList();
        } catch (PersistenceException e) {
            Logger.error(format("NotificationsDAO :: getGroupedNotifications : error fetching unread grouped notifications for user: [%s]", userId));
            throw new PersistenceException(e);
        }
    }


    public int isUnreadMessages(final Integer userId, final EntityManager entityManager) {
        checkNotNull(userId, "userId");

        try {
            Long count = entityManager.createQuery("SELECT count(n) FROM Notification n WHERE n.isRead = false and n.userId = :userId", Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return count.intValue();
        } catch (NoResultException e) {
            return 0;
        } catch (PersistenceException e) {
            Logger.error(format("NotificationsDAO :: getUnreadNotifications : error checking for unread notifications for user: [%s]", userId));
            throw new PersistenceException(e);
        }
    }

    private static boolean doesNotificationAlreadyExist(final Integer userId, final NotificationType type, final Integer fromUser,
                                                        final Integer fromQuest, final Integer fromComment, final EntityManager entityManager) {

        try {
            Long count = entityManager.createQuery("SELECT count(n) FROM Notification n WHERE n.userId = :userId " +
                    "AND n.type = :type AND n.fromUser = :fromUser AND n.fromQuest = :fromQuest AND n.isRead = false AND n.fromComment = :fromComment", Long.class)
                    .setParameter("userId", userId)
                    .setParameter("type", type.code())
                    .setParameter("fromUser", fromUser)
                    .setParameter("fromQuest", fromQuest)
                    .setParameter("fromComment", fromComment)
                    .getSingleResult();
            return count.intValue() > 0;
        } catch (NoResultException e) {
            return false;
        } catch (PersistenceException e) {
            Logger.error(format("NotificationsDAO :: doesNotificationAlreadyExist : error checking for notifications for user: [%s]", userId));
            throw new PersistenceException(e);
        }
    }
}
