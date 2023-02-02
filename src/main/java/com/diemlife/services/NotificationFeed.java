package com.diemlife.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.diemlife.dao.QuestActivityHome;
import com.diemlife.dao.QuestBackingDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestBacking;
import com.diemlife.models.QuestRelatedTransaction;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;
import play.libs.Json;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by andrew on 5/9/17.
 */
public class NotificationFeed {

    public static JsonNode getUserBackingNotificationsByUserId(Integer userId, EntityManager em) {

        List<NotificationUsersBacking> notificationUsersList = new ArrayList<>();

        try {
            User user = UserService.getById(userId, em);
            List<QuestBacking> questBackingList = new QuestBackingDAO(em).getBackingsForUser(user);

            if (questBackingList != null && !questBackingList.isEmpty()) {
                for (QuestBacking questBacking : questBackingList) {
                    NotificationUsersBacking notificationUsersBacking = new NotificationUsersBacking();

                    notificationUsersBacking.backerId = Optional.ofNullable(questBacking.getPaymentTransaction().from)
                            .map(customer -> customer.user)
                            .map(User::getId)
                            .orElse(null);
                    notificationUsersBacking.backerName = questBacking.getBillingPersonalInfo().getName();
                    if (questBacking.getPaymentTransaction() instanceof QuestRelatedTransaction) {
                        Quests quest = ((QuestRelatedTransaction) questBacking.getPaymentTransaction()).quest;
                        notificationUsersBacking.questTitle = quest.getTitle();
                        notificationUsersBacking.questId = quest.getId();
                        notificationUsersBacking.lastModifiedDate = questBacking.getBackingDate();
                        notificationUsersBacking.profilePictureURL = user.getProfilePictureURL();
                        notificationUsersBacking.backerUsername = user.getUserName();
                        notificationUsersList.add(notificationUsersBacking);
                    }
                }
            }

            return Json.toJson(notificationUsersList);

        } catch (Exception ex) {
            Logger.error("NotificationFeed :: getUserNotificationsByUserId : Error getting user notifications => " + ex, ex);
            return null;
        }
    }

    public static JsonNode getUserPendingNotificationsByUserId(Integer userId, EntityManager em) {

        List<NotificationUsersPendingQuests> notificationUsersPendingQuests = new ArrayList<NotificationUsersPendingQuests>();

        try {
            List<QuestActivity> questActivitiesPending = QuestActivityHome.getRecentActivityPending(2, 2, userId, em);

            if (questActivitiesPending != null && !questActivitiesPending.isEmpty()) {
                for (QuestActivity questActivity : questActivitiesPending) {
                    User user = UserService.getById(questActivity.getUserId(), em);
                    Quests quest = QuestsDAO.findByIdPublicQuests(questActivity.getQuestId(), em);
                    if (quest != null && user != null) {
                        NotificationUsersPendingQuests notificationUsersPendingQuest = new NotificationUsersPendingQuests();
                        notificationUsersPendingQuest.backerId = user.getId();
                        notificationUsersPendingQuest.backerName = user.getName();
                        notificationUsersPendingQuest.backerUsername = user.getUserName();
                        notificationUsersPendingQuest.questId = quest.getId();
                        notificationUsersPendingQuest.questTitle = quest.getTitle();
                        notificationUsersPendingQuest.lastModifiedDate = questActivity.getLastModifiedDate();
                        notificationUsersPendingQuest.profilePictureURL = user.getProfilePictureURL();
                        notificationUsersPendingQuests.add(notificationUsersPendingQuest);
                    }
                }
            }

            return Json.toJson(notificationUsersPendingQuests);


        } catch (Exception ex) {
            Logger.error("NotificationFeed :: getUserPendingNotificationsByUserId : Error getting user notifications => " + ex, ex);
            return null;
        }
    }

    public static JsonNode getUserCompletedNotificationsByUserId(Integer userId, EntityManager em) {

        List<NotificationUsersCompletedQuests> notificationUsersCompletedQuests = new ArrayList<NotificationUsersCompletedQuests>();

        try {
            List<QuestActivity> questActivitiesCompleted = QuestActivityHome.getRecentActivityCompleted(2, userId, em);

            if (questActivitiesCompleted != null && !questActivitiesCompleted.isEmpty()) {
                for (QuestActivity questActivity : questActivitiesCompleted) {
                    User user = UserService.getById(questActivity.getUserId(), em);
                    Quests quest = QuestsDAO.findByIdPublicQuests(questActivity.getQuestId(), em);
                    if (quest != null && user != null) {
                        Logger.info("quest activity = " + questActivity.getQuestId());
                        NotificationUsersCompletedQuests notificationUsersCompletedQuest = new NotificationUsersCompletedQuests();
                        notificationUsersCompletedQuest.backerId = user.getId();
                        notificationUsersCompletedQuest.backerName = user.getName();
                        notificationUsersCompletedQuest.backerUsername = user.getUserName();
                        notificationUsersCompletedQuest.questId = quest.getId();
                        notificationUsersCompletedQuest.questTitle = quest.getTitle();
                        notificationUsersCompletedQuest.lastModifiedDate = questActivity.getLastModifiedDate();
                        notificationUsersCompletedQuest.profilePictureURL = user.getProfilePictureURL();
                        notificationUsersCompletedQuests.add(notificationUsersCompletedQuest);
                    }
                }
            }

            return Json.toJson(notificationUsersCompletedQuests);


        } catch (Exception ex) {
            Logger.error("NotificationFeed :: getUserCompletedNotificationsByUserId : Error getting user notifications => " + ex, ex);
            return null;
        }
    }

    public static class NotificationUsersBacking {

        public Integer backerId;
        public String backerName;
        public String backerUsername;
        public Integer questId;
        public String questTitle;
        public Date lastModifiedDate;
        public String profilePictureURL;
    }

    public static class NotificationUsersPendingQuests {

        public Integer backerId;
        public String backerName;
        public String backerUsername;
        public Integer questId;
        public String questTitle;
        public Date lastModifiedDate;
        public String profilePictureURL;
    }

    public static class NotificationUsersCompletedQuests {

        public Integer backerId;
        public String backerName;
        public String backerUsername;
        public Integer questId;
        public String questTitle;
        public Date lastModifiedDate;
        public String profilePictureURL;
    }

}
