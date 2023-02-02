package com.diemlife.dao;

import constants.QuestUserFlagKey;
import models.QuestUserFlag;
import models.Quests;
import models.User;
import play.Logger;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static constants.QuestUserFlagKey.FOLLOWED;
import static constants.QuestUserFlagKey.STARRED;
import static java.lang.String.format;

public class QuestUserFlagDAO extends TypedDAO<QuestUserFlag> {

    public QuestUserFlagDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public QuestUserFlag followQuestForUser(final @Nonnull Quests quest, final @Nonnull User user) {
        return addQuestFlagForUser(quest, user, FOLLOWED);
    }

    public QuestUserFlag unFollowQuestForUser(final @Nonnull Quests quest, final @Nonnull User user) {
        return removeQuestFlagForUser(quest, user, FOLLOWED);
    }

    public boolean isFollowedQuestForUser(final @Nonnull Integer questId, final @Nonnull Integer userId) {
        return isQuestFlagPresentForUser(questId, userId, FOLLOWED);
    }

    public long getFollowingCountForQuest(final @Nonnull Quests quest) {
        return getUserFlagsCountForQuest(quest, FOLLOWED);
    }

    public List<Integer> getQuestsBeingFollowedForUser(final @Nonnull User user) {
        return getQuestIdsWithFlagForUser(user, FOLLOWED);
    }

    public List<Integer> getUsersFollowingQuest(final @Nonnull Quests quest) {
        return getUserIdsWithFlagForQuest(quest, FOLLOWED);
    }

    public QuestUserFlag starQuestForUser(final @Nonnull Quests quest, final @Nonnull User user) {
        return addQuestFlagForUser(quest, user, STARRED);
    }

    public QuestUserFlag unStarQuestForUser(final @Nonnull Quests quest, final @Nonnull User user) {
        return removeQuestFlagForUser(quest, user, STARRED);
    }

    public boolean isStarredQuestForUser(final @Nonnull Integer questId, final @Nonnull Integer userId) {
        return isQuestFlagPresentForUser(questId, userId, STARRED);
    }

    public List<Integer> retrieveStarredQuests(final @Nonnull User user) {
        return getQuestIdsWithFlagForUser(user, STARRED);
    }

    private Optional<QuestUserFlag> getQuestFlagForUser(final @Nonnull Integer questId, final @Nonnull Integer userId, final @Nonnull QuestUserFlagKey flag) {
        return entityManager.createQuery("SELECT f FROM QuestUserFlags f " +
                "WHERE f.quest.id = :questId " +
                "AND f.user.id = :userId " +
                "AND f.flagKey = :flagKey", QuestUserFlag.class)
                .setParameter("questId", questId)
                .setParameter("userId", userId)
                .setParameter("flagKey", flag)
                .getResultList()
                .stream()
                .findFirst();
    }

    private QuestUserFlag addQuestFlagForUser(final @Nonnull Quests quest, final @Nonnull User user, final @Nonnull QuestUserFlagKey flag) {
        return getQuestFlagForUser(quest.getId(), user.getId(), flag)
                .map(questUserFlag -> {
                    questUserFlag.flagValue = true;
                    questUserFlag.modificationDate = Date.from(Instant.now());
                    return entityManager.merge(questUserFlag);
                })
                .orElseGet(() -> {
                    final QuestUserFlag questActivity = new QuestUserFlag();
                    questActivity.quest = quest;
                    questActivity.user = user;
                    questActivity.flagKey = flag;
                    questActivity.flagValue = true;
                    questActivity.creationDate = Date.from(Instant.now());

                    try {
                        entityManager.persist(questActivity);
                    } catch (final PersistenceException e) {
                        Logger.error(format("QuestUserFlagDAO :: followQuestForUser : error persisting followed flag: [%s]", e.getMessage()), e);
                    }

                    return questActivity;
                });
    }

    private QuestUserFlag removeQuestFlagForUser(final @Nonnull Quests quest, final @Nonnull User user, final @Nonnull QuestUserFlagKey flag) {
        return getQuestFlagForUser(quest.getId(), user.getId(), flag)
                .map(questUserFlag -> {
                    questUserFlag.flagValue = false;
                    questUserFlag.modificationDate = Date.from(Instant.now());
                    return entityManager.merge(questUserFlag);
                })
                .orElse(null);
    }

    private boolean isQuestFlagPresentForUser(final @Nonnull Integer questId, final @Nonnull Integer userId, final @Nonnull QuestUserFlagKey flag) {
        final Optional<QuestUserFlag> optionalFlag = getQuestFlagForUser(questId, userId, flag);
        return optionalFlag
                .map(questUserFlag -> questUserFlag.flagValue)
                .orElse(false);
    }

    private long getUserFlagsCountForQuest(final @Nonnull Quests quest, final @Nonnull QuestUserFlagKey flag) {
        return entityManager.createQuery("SELECT count(f) FROM QuestUserFlags f " +
                "WHERE f.quest.id = :questId " +
                "AND f.flagKey = :flagKey " +
                "AND f.flagValue = true", Long.class)
                .setParameter("questId", quest.getId())
                .setParameter("flagKey", flag)
                .getSingleResult();
    }

    private List<Integer> getQuestIdsWithFlagForUser(final @Nonnull User user, final @Nonnull QuestUserFlagKey flag) {
        return entityManager.createQuery("SELECT f.quest.id FROM QuestUserFlags f " +
                "WHERE f.user.id = :userId " +
                "AND f.flagKey = :flagKey " +
                "AND f.flagValue = true", Integer.class)
                .setParameter("userId", user.getId())
                .setParameter("flagKey", flag)
                .getResultList();
    }

    private List<Integer> getUserIdsWithFlagForQuest(final @Nonnull Quests quest, final @Nonnull QuestUserFlagKey flag) {
        return entityManager.createQuery("SELECT f.user.id FROM QuestUserFlags f " +
                "WHERE f.quest.id = :questId " +
                "AND f.flagKey = :flagKey " +
                "AND f.flagValue = true", Integer.class)
                .setParameter("questId", quest.getId())
                .setParameter("flagKey", flag)
                .getResultList();
    }

}
