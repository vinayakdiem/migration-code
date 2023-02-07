package com.diemlife.dao;

import static java.lang.String.format;

import java.sql.Timestamp;
import java.time.Instant;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Repository;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.forms.QuestActionPointForm;
import com.diemlife.models.QuestEventHistory;
import com.diemlife.models.QuestEvents;

import play.Logger;

@Repository
public class QuestEventHistoryDAO {
	
	@PersistenceContext
	private EntityManager em;

    public void addEventHistory(final Integer questId,
                                       final Integer userId,
                                       final QuestEvents questEvent,
                                       final Integer origQuestId) {
        addEventHistory(questId, userId, questEvent, origQuestId, null);
    }

    public void addEventHistory(final Integer questId,
                                       final Integer userId,
                                       final QuestEvents questEvent,
                                       final Integer origQuestId,
                                       final QuestActionPointForm point
                                       ) {
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
            //FIXME Vinayak
//            eventHistory.setPoint(QuestTasksDAO.buildGeoPoint(point));

            em.persist(eventHistory);

        } catch (final PersistenceException e) {
            Logger.warn(format("Error adding event history entry '%s' for Quest %s and user %s : %s", questEvent, questId, userId, e.getMessage()), e);
        }
    }

}
