package com.diemlife.services;

import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.constants.NotificationType;
import com.diemlife.dao.NotificationsDAO;
import com.diemlife.dao.QuestCommentsDAO;
import com.diemlife.dao.QuestUserFlagDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dto.CommentsDTO;
import com.diemlife.dto.GroupedNotificationDTO;
import com.diemlife.dto.NotificationDTO;
import com.diemlife.models.Notification;
import com.diemlife.models.QuestComments;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Configuration;
import play.cache.CacheApi;
import play.cache.NamedCache;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.diemlife.constants.NotificationType.COMMENT;
import static com.diemlife.constants.NotificationType.COMMENT_MENTION;
import static com.diemlife.constants.NotificationType.COMMENT_REPLIED;
import static com.diemlife.constants.NotificationType.PHOTO_VIDEO_ADDED;

@Singleton
public class NotificationService {

    private final JPAApi jpaApi;
    private final MessagesApi messages;
    private final CommentsService commentsService;
    private final CacheApi notificationsCache;
    private final Configuration config;

    @Inject
    public NotificationService(final JPAApi jpaApi,
                               final MessagesApi messages,
                               final CommentsService commentsService,
                               final @NamedCache("notifications-cache") CacheApi notificationsCache,
                               final Configuration config) {
        this.jpaApi = jpaApi;
        this.messages = messages;
        this.commentsService = commentsService;
        this.notificationsCache = notificationsCache;
        this.config = config;
    }

    public List<Integer> getUserIdsSubscribedToQuest(final Quests quest) {
        return new QuestUserFlagDAO(JPA.em()).getUsersFollowingQuest(quest);
    }

    public void addUserNotification(final Integer userId, final NotificationType notificationType, final Integer fromUserId) {
        new NotificationsDAO(notificationsCache).addNotification(userId, notificationType, fromUserId, null, null, JPA.em());
    }

    public void addQuestNotification(final Integer userId,
                                     final NotificationType notificationType,
                                     final Integer fromUserId,
                                     final Integer fromQuestId) {
        jpaApi.withTransaction(entityManager -> {
            if (new QuestUserFlagDAO(entityManager).isFollowedQuestForUser(fromQuestId, userId)) {
                new NotificationsDAO(notificationsCache).addNotification(userId, notificationType, fromUserId, fromQuestId, null, entityManager);
            }
            return entityManager;
        });
    }

    public void addCommentNotification(final Integer userId,
                                       final NotificationType notificationType,
                                       final Integer fromUserId,
                                       final Integer fromQuestId,
                                       final Integer fromCommentId) {
        final EntityManager entityManager = JPA.em();
        if (new QuestUserFlagDAO(entityManager).isFollowedQuestForUser(fromQuestId, userId)) {
            new NotificationsDAO(notificationsCache).addNotification(userId, notificationType, fromUserId, fromQuestId, fromCommentId, entityManager);
        }
    }

    public void addMentionNotification(final Integer userId,
                                       final Integer fromUserId,
                                       final Integer fromQuestId,
                                       final Integer fromCommentId) {
        new NotificationsDAO(notificationsCache).addNotification(userId, COMMENT_MENTION, fromUserId, fromQuestId, fromCommentId, jpaApi.em());
    }

    public void addCommentReplyNotification(final Integer userId,
                                            final Integer fromUserId,
                                            final Integer fromQuestId,
                                            final Integer fromCommentId) {
        new NotificationsDAO(notificationsCache).addNotification(userId, COMMENT_REPLIED, fromUserId, fromQuestId, fromCommentId, jpaApi.em());
    }

    public void clearCache(final Integer userId) {
        new NotificationsDAO(notificationsCache).clearNotificationCache(userId, notificationsCache);
    }

    public List<NotificationDTO> getNotifications(final User user, final Integer startPos, final Integer endPos, final Http.RequestHeader request) {
        final Messages message = messages.preferred(request);

        List<GroupedNotificationDTO> notifications = jpaApi.withTransaction(em -> new NotificationsDAO(notificationsCache).getGroupedNotifications(user.getId(), startPos, endPos, em));

        return getNotificationDTOs(notifications, user, message);
    }

    private List<NotificationDTO> getNotificationDTOs(final List<GroupedNotificationDTO> notifications, final User user, final Messages message) {
        final List<NotificationDTO> result = new ArrayList<>();
        notifications.forEach(groupedNotification -> {
            final User fromUser = jpaApi.withTransaction(em -> UserHome.findById(groupedNotification.lastUserId, em));
            final Quests fromQuest = jpaApi.withTransaction(em -> QuestsDAO.findById(groupedNotification.fromQuestId, em));
            final QuestComments comment = jpaApi.withTransaction(em -> QuestCommentsDAO.findById(groupedNotification.fromComment, em));
            final String text = getNotificationText(groupedNotification, message, fromUser, fromQuest);

            Notification notification = GroupedNotificationDTO.toNotification(groupedNotification);
            notification.setMessage(text);

            final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
            Optional.ofNullable(NotificationDTO.toDTO(notification, fromUser, fromQuest, envUrl))
                    .map(dto -> comment == null ? dto : dto.withComment(CommentsDTO.toDTO(comment).withMentions(commentsService.getCommentMentions(comment, user))))
                    .ifPresent(result::add);
        });
        return result;
    }

    private String getNotificationText(final GroupedNotificationDTO dto, final Messages message, final User fromUser, final Quests fromQuest) {
        if (fromQuest != null && fromUser != null) {
            final long othersCount = dto.countFromUsers - 1;
            final NotificationType notificationType = NotificationType.valueOf(dto.notificationsType);
            if (dto.notificationsCount > 1
                    && dto.countFromUsers > 1
                    && (dto.notificationsType.equals(COMMENT.name()) || dto.notificationsType.equals(PHOTO_VIDEO_ADDED.name()))) {
                return message.at(
                        notificationType.groupedMessage(),
                        fromUser.getName(),
                        othersCount,
                        othersCount > 1 ? message.at("notification.others.plural") : message.at("notification.others.singular"),
                        dto.notificationsCount,
                        dto.notificationsCount > 1 ? message.at(notificationType.pluralMessage()) : message.at(notificationType.singularMessage()),
                        fromQuest.getTitle()
                );
            } else if (dto.notificationsCount > 1
                    && dto.countFromUsers == 1
                    && (dto.notificationsType.equals(COMMENT.name()) || dto.notificationsType.equals(PHOTO_VIDEO_ADDED.name()))) {
                return message.at(
                        notificationType.groupedSingleMessage(),
                        fromUser.getName(),
                        dto.notificationsCount,
                        dto.notificationsCount > 1 ? message.at(notificationType.pluralMessage()) : message.at(notificationType.singularMessage()),
                        fromQuest.getTitle()
                );
            } else {
                return message.at(NotificationType.valueOf(dto.notificationsType).message(), fromUser.getName(), fromQuest.getTitle());
            }
        } else if (fromQuest == null && fromUser != null) {
            return message.at(NotificationType.valueOf(dto.notificationsType).message(), fromUser.getName());
        } else {
            return message.at(NotificationType.valueOf(dto.notificationsType).message());
        }
    }

}
