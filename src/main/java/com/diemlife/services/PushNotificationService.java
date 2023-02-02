package com.diemlife.services;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.diemlife.constants.QuestActivityStatus;
import com.diemlife.dao.*;
import com.diemlife.dto.QuestMemberDTO;
import lombok.NonNull;
import models.*;
import play.Logger;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.springframework.util.StringUtils.hasText;

public class PushNotificationService {

    @NonNull
    private final PushNotificationDeviceDAO pushNotificationDeviceDAO;
    @NonNull
    private final PushNotificationMessageInfoDAO messageInfoDAO;
    @NonNull
    private final JPAApi jpaApi;

    @Inject
    public PushNotificationService(PushNotificationDeviceDAO pushNotificationDeviceDAO,
                                   PushNotificationMessageInfoDAO messageInfoDAO, JPAApi jpaApi) {
        this.jpaApi = jpaApi;
        this.pushNotificationDeviceDAO = pushNotificationDeviceDAO;
        this.messageInfoDAO = messageInfoDAO;
    }

    public Optional<String> registerNewDevice(final String token, final User user) {
        final Date addedDate = new Date();

        final PushNotificationDevice device = new PushNotificationDevice(token, user.getId(), addedDate);

        if (!pushNotificationDeviceDAO.isDeviceRegistered(token)) {
            pushNotificationDeviceDAO.addNewDevice(device);

            Logger.info("successfully registered new device [{}]", token);
            return Optional.ofNullable(device.getToken());
        }

        Logger.info("token already exists for device, skipping... [{}]", token);

        return Optional.empty();
    }

    public void sendNotificationToQuestFollowers(final int questId, final String messageTitle, final String messageText,
                                                 String questActivityStatus, final String questActivityGroup) {
        List<QuestActivity> questActiveMembers;
        List<QuestActivity> questCompletedMembers;
        List<QuestMemberDTO> questSavedMembers = new ArrayList<>();
        List<QuestActivity> membersInQuestActivityGroup = new ArrayList<>();
        final Quests quest = QuestsDAO.findById(questId, jpaApi.em());

        if (hasText(questActivityGroup)) {
            membersInQuestActivityGroup = QuestActivityHome.getUsersInGroup(questId, questActivityGroup, jpaApi.em());
        }
        if (hasText(questActivityStatus)) {
            if (questActivityStatus.equals(QuestActivityStatus.SAVED.name())) {
                questSavedMembers = QuestSavedDAO.getSavedMembersForQuest(quest, jpaApi.em());
                Logger.info("found [{}] members with quest Id [{}] saved while sending push notification", questSavedMembers.size(), questId);
            }
            questActiveMembers = QuestActivityHome.getUsersDoingQuest(questId, null, jpaApi.em());
            questCompletedMembers = QuestActivityHome.getUsersCompletedWithQuest(questId, jpaApi.em());
            Logger.info("found [{}] members with quest Id [{}] active or completed while sending push notification", questSavedMembers.size(), questId);
        } else {
            questActiveMembers = QuestActivityHome.getUsersDoingQuest(questId, null, jpaApi.em());
            questSavedMembers = QuestSavedDAO.getSavedMembersForQuest(quest, jpaApi.em());
            questCompletedMembers = QuestActivityHome.getUsersCompletedWithQuest(questId, jpaApi.em());
            Logger.info("found [{}] members with quest Id [{}] active or completed while sending push notification", questSavedMembers.size(), questId);
        }

        final List<PushNotificationDevice> devices = new ArrayList<>();

        questActiveMembers.stream()
                .map(activity -> pushNotificationDeviceDAO.findForUser(activity.getUserId()))
                .filter(userDevice -> userDevice.size() > 0)
                .forEach(devices::addAll);

        questSavedMembers.stream()
                .map(member -> pushNotificationDeviceDAO.findForUser(member.getUserId()))
                .filter(userDevice -> userDevice.size() > 0)
                .forEach(devices::addAll);

        questCompletedMembers.stream()
                .map(member -> pushNotificationDeviceDAO.findForUser(member.getUserId()))
                .filter(userDevice -> userDevice.size() > 0)
                .forEach(devices::addAll);


        final List<Integer> activeUserIds = removeDuplicates(questActiveMembers.stream().map(QuestActivity::getUserId).collect(Collectors.toList()), Collections.emptyList());
        final List<Integer> savedUserIds = removeDuplicates(questSavedMembers.stream().map(QuestMemberDTO::getUserId).collect(Collectors.toList()), activeUserIds);
        final List<Integer> completedUserIds = removeDuplicates(questCompletedMembers.stream().map(QuestActivity::getUserId).collect(Collectors.toList()), savedUserIds);
        final List<Integer> groupUserIds = membersInQuestActivityGroup.stream().map(QuestActivity::getUserId).collect(Collectors.toList());


        final String notificationTrackingId = UUID.randomUUID().toString();

        if (!hasText(questActivityGroup)) {
            sendNotificationsForQuestActivityGroup(devices, activeUserIds, messageText, messageTitle, questId, "active", notificationTrackingId);
            sendNotificationsForQuestActivityGroup(devices, savedUserIds, messageText, messageTitle, questId, "saved", notificationTrackingId);
            sendNotificationsForQuestActivityGroup(devices, completedUserIds, messageText, messageTitle, questId, "completed", notificationTrackingId);
        } else {
            sendNotificationsForQuestActivityGroup(devices, activeUserIds.stream().filter(groupUserIds::contains).collect(Collectors.toList()),
                    messageText, messageTitle, questId, "active", notificationTrackingId);
            sendNotificationsForQuestActivityGroup(devices, savedUserIds.stream().filter(groupUserIds::contains).collect(Collectors.toList()),
                    messageText, messageTitle, questId, "saved", notificationTrackingId);
            sendNotificationsForQuestActivityGroup(devices, completedUserIds.stream().filter(groupUserIds::contains).collect(Collectors.toList()),
                    messageText, messageTitle, questId, "completed", notificationTrackingId);
        }
    }

    private void sendNotificationsForQuestActivityGroup(List<PushNotificationDevice> devices, List<Integer> userIdsForFiltering, String messageText,
                                                        String messageTitle, int questId, String activityStatus, String trackingId) {
        devices.stream()
                .filter(device -> userIdsForFiltering.contains(device.getUserId()))
                .forEachOrdered(device ->
                        sendNotificationWithQuestLink(device.getToken(),
                                format(messageText, UserHome.findById(device.getUserId(), jpaApi.em()).getFirstName()),
                                messageTitle,
                                valueOf(questId),
                                activityStatus,
                                device.getUserId(),
                                trackingId));
    }

    private List<Integer> removeDuplicates(List<Integer> userIds, List<Integer> userIdsToRemove) {
        if (!userIdsToRemove.isEmpty()) {
            userIds.removeAll(userIdsToRemove);
        }
        return userIds.stream().distinct().collect(Collectors.toList());
    }

    public void updateMessageInfoForUserOpening(@NonNull final String messageId) {

        Optional<PushNotificationMessageInfo> messageInfo = messageInfoDAO.findByMessageId(messageId);

        if (messageInfo.isPresent()) {
            Date openedDate = new Date();

            messageInfo.get().setOpenedDate(openedDate);

            messageInfoDAO.updateMessageInfo(messageInfo.get());

            Logger.info("user [{}] opened message with message id of [{}]", messageInfo.get().getUserId(), messageId);
        } else {
            Logger.warn("message info does not exist for messageId [{}]. This should not happen", messageId);
        }
    }

    private void sendNotificationWithQuestLink(final String token, final String messageText, final String messageTitle,
                                               final String questDeepLink, final String questDoerStatus, final int doerId,
                                               final String notificationTrackingId) {

        initializeFirebaseApplication();

        final String messageId = UUID.randomUUID().toString();

        Message message = Message.builder()
                .setNotification(new Notification(messageTitle, messageText))
                .putData("questId", questDeepLink)
                .putData("userId", valueOf(doerId))
                .putData("messageId", messageId)
                .putData("questDoerStatus", questDoerStatus)
                .setToken(token)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);

            Logger.info("successfully sent push notification for questId [{}] for user [{}] with activity status of [{}] response [{}]", questDeepLink, doerId, questDoerStatus, response);
        } catch (FirebaseMessagingException e) {
            Logger.error("error sending push notification [{}]", e.getMessage());
        }

        addMessageInfo(messageId, doerId, token, notificationTrackingId, messageTitle, messageText);
    }

    private void initializeFirebaseApplication() {

        try {
            FirebaseApp.getInstance();
        } catch (IllegalStateException e) {
            Logger.warn("Firebase instance has not yet been initialized. Doing it now");
            FirebaseApp.initializeApp();
        }
    }

    private void addMessageInfo(String messageId, int userId, String deviceToken, String trackingId, String messageTitle, String messageText) {
        final PushNotificationMessageInfo messageInfo = new PushNotificationMessageInfo(messageId, deviceToken, userId,
                trackingId, null, messageTitle, messageText);

        messageInfoDAO.addMessageInfo(messageInfo);

        Logger.info("successfully added new message info for user [{}] with a message Id of [{}] and tracking Id of [{}]", userId, messageId, trackingId);
    }
}
