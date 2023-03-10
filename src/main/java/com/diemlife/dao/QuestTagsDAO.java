package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.QuestTags;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Repository;

import java.util.List;

import static java.lang.String.format;

@Repository
public class QuestTagsDAO {

	@PersistenceContext
	EntityManager em;
	
    public List<QuestTags> getQuestTagsById(final Integer questId) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        try {
            return em.createQuery("SELECT qt FROM QuestTags qt WHERE qt.questId = :questId",
                    QuestTags.class)
                    .setParameter("questId", questId)
                    .getResultList();
        } catch (final PersistenceException ex) {
            Logger.error(format("QuestTagsDAO :: getQuestTagsById : error finding quest tags [%s]", ex));
            throw new PersistenceException(ex);
        }
    }

    public List<QuestTags> getQuestTagsByTag(final String tag) {
        if (tag == null) {
            throw new RequiredParameterMissingException("tag");
        }
        try {
            return em.createQuery("SELECT qt FROM QuestTags qt WHERE qt.tag = :tag",
                    QuestTags.class)
                    .setParameter("tag", tag)
                    .getResultList();
        } catch (final PersistenceException ex) {
            Logger.error(format("QuestTagsDAO :: getQuestTagsByTag : error finding tags by tag name [%s]", ex));
            throw new PersistenceException(ex);
        }
    }

}
