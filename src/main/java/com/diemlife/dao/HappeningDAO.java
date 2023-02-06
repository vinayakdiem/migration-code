package com.diemlife.dao;

import static com.diemlife.constants.QuestActivityStatus.COMPLETE;
import static com.diemlife.constants.QuestActivityStatus.IN_PROGRESS;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.time.DateUtils.truncate;

import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Repository;

import com.diemlife.constants.QuestActivityStatus;
import com.diemlife.models.Happening;

import play.Logger;

@Repository
public class HappeningDAO extends TypedDAO<Happening> {

    

    public Happening getHappeningByQuestId(final Integer questId) {
        if (questId == null) {
            throw new IllegalArgumentException("A questId is required to retrieve a Happening");
        }
        try {
            return entityManager
                    .createQuery("select h from Happenings h join h.quest q where q.id = :questId", Happening.class)
                    .setParameter("questId", questId)
                    .getSingleResult();
        } catch (final NoResultException e) {
            Logger.warn(format("No Happening found for questId %s", questId));
            return null;
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("Unable to retrieve Happening for questId %s", questId), e);
            throw e;
        }
    }

    public boolean isQuestHappening(final Integer questId) {
        if (questId == null) {
            throw new IllegalArgumentException("A questId is required to retrieve a Happening");
        }
        try {
            Query query = entityManager.createQuery("select count(h.id) from Happenings h where h.quest.id = :questId");
            query.setParameter("questId", questId);
            return query.getSingleResult() != Long.valueOf(0);
        } catch (final NoResultException e) {
            Logger.warn(format("Blah Happening found for questId %s", questId));
            return false;
        } catch (final PersistenceException | IllegalStateException e) {
            Logger.error(format("Unable to retrieve Happening for questId %s", questId), e);
            throw e;
        }
    }

    public List<Happening> getUpcomingHappenings(final Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("A userId is required to retrieve upcoming Happening list");
        }

        final List<Happening> activeHappenings = getParticipatedHappenings(userId, IN_PROGRESS);
        final List<Happening> futureActiveHappenings = getFutureActiveHappenings();

        return activeHappenings.stream()
                .filter(futureActiveHappenings::contains)
                .collect(toList());
    }

    public List<Happening> getRecommendedHappenings(final Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("A userId is required to retrieve recommended Happening list");
        }

        final List<Happening> interestsMatchingHappenings = getInterestsMatchingHappenings(userId);
        final List<Happening> futureActiveHappenings = getFutureActiveHappenings();
        final List<Happening> participatedHappenings = getParticipatedHappenings(userId, IN_PROGRESS, COMPLETE);

        return interestsMatchingHappenings.stream()
                .filter(happening -> futureActiveHappenings.contains(happening) && !participatedHappenings.contains(happening))
                .collect(toList());
    }

    private List<Happening> getParticipatedHappenings(final @NotNull Integer userId,
                                                      final @NotNull QuestActivityStatus... statuses) {
        return entityManager.createQuery("SELECT DISTINCT h FROM Happenings h " +
                "INNER JOIN QuestActivity qa ON qa.questId = h.quest.id " +
                "WHERE qa.userId = :userId " +
                "AND qa.status IN :activityStatuses " +
                "ORDER BY h.happeningDate", Happening.class)
                .setParameter("userId", userId)
                .setParameter("activityStatuses", stream(statuses).map(QuestActivityStatus::name).collect(toList()))
                .getResultList();
    }

    private List<Happening> getFutureActiveHappenings() {
        return entityManager.createQuery("SELECT h FROM Happenings h " +
                "WHERE (h.happeningDate >= :today OR h.happeningDate IS NULL) " +
                "AND h.active = TRUE " +
                "ORDER BY h.happeningDate", Happening.class)
                .setParameter("today", truncate(Calendar.getInstance().getTime(), Calendar.DAY_OF_MONTH))
                .getResultList();
    }

    private List<Happening> getInterestsMatchingHappenings(final @NotNull Integer userId) {
        return entityManager.createQuery("SELECT h FROM Happenings h " +
                "INNER JOIN Quests q ON q.id = h.quest.id " +
                "LEFT OUTER JOIN UserFavorites uf ON uf.favorite = q.pillar " +
                "WHERE uf.userId = :userId " +
                "ORDER BY h.happeningDate", Happening.class)
                .setParameter("userId", userId)
                .getResultList();
    }

}
