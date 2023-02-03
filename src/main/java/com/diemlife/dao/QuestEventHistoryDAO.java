package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import forms.QuestActionPointForm;
import com.diemlife.models.QuestEventHistory;
import com.diemlife.models.QuestEvents;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.sql.Timestamp;
import java.time.Instant;

import static java.lang.String.format;

public class QuestEventHistoryDAO {

    public static void addEventHistory(final Integer questId,
                                       final Integer userId,
                                       final QuestEvents questEvent,
                                       final Integer origQuestId,
                                       final EntityManager em) {
        addEventHistory(questId, userId, questEvent, origQuestId, null, em);
    }

    public static void addEventHistory(final Integer questId,
                                       final Integer userId,
                                       final QuestEvents questEvent,
                                       final Integer origQuestId,
                                       final QuestActionPointForm point,
                                       final EntityManager em) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        if (userId == null) {
            throw new RequiredParameterMissingException("userId");
        }
        if (questEvent == null) {
            throw new RequiredParameterMissingException("questEvent");
        }
        try {
            final QuestEventHistory eventHistory = new QuestEventHistory();
            eventHistory.setQuestId(questId);
            eventHistory.setUserId(userId);
            eventHistory.setEventDesc(questEvent);
            eventHistory.setOrigQuestId(origQuestId);
            eventHistory.setAddedDate(Timestamp.from(Instant.now()));
            eventHistory.setPoint(QuestTasksDAO.buildGeoPoint(point));

            em.persist(eventHistory);

        } catch (final PersistenceException e) {
            Logger.warn(format("Error adding event history entry '%s' for Quest %s and user %s : %s", questEvent, questId, userId, e.getMessage()), e);
        }
    }

}
