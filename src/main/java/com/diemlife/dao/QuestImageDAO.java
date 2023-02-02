package com.diemlife.dao;

import exceptions.RequiredParameterMissingException;
import models.QuestImage;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuestImageDAO {

	 public static QuestImage findById(Integer id, EntityManager em) {
        if (id == null) {
            return null;
        }
        return em.find(QuestImage.class, id);
    }
    
    public static QuestImage addNewQuestImage(Integer userId, Integer questId, String url, String caption, EntityManager em) {
        if (userId == null) {
            throw new RequiredParameterMissingException("userId");
        }
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        if (url == null) {
            throw new RequiredParameterMissingException("url");
        }
        Logger.info("Saving image : user = " + userId + ", quest = " + questId + ", url = " + url + ", caption = " + caption);
        try {
            QuestImage questImage = new QuestImage();
            questImage.setQuestId(questId);
            questImage.setUserId(userId);
            questImage.setQuestImageUrl(url);
            questImage.setCaption(caption);

            final Date now = new Date();
            questImage.setCreatedDate(now);
            questImage.setLastModifiedDate(now);

            em.persist(questImage);

            return questImage;

        } catch (final PersistenceException e) {
            Logger.error("QuestImageDAO :: addNewQuestImage : Error saving quest image => " + e.getMessage(), e);

            throw e;
        }
    }

    public static List<QuestImage> getQuestImagesForQuest(final Integer questId, final EntityManager em) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        try {
            return em.createQuery("SELECT q FROM QuestImage q WHERE q.questId = :questId AND q.deleted IS NULL ORDER BY q.createdDate DESC", QuestImage.class)
                    .setParameter("questId", questId)
                    .getResultList();
        } catch (final PersistenceException e) {
            Logger.error("QuestImageDAO :: getQuestImagesForQuest : error finding quest images => " + e, e);
            return new ArrayList<>();
        }
    }

    public static void removeQuestPhotoById(Integer questPhotoId, Integer userId, EntityManager em) {
        try {
            QuestImage image = em.find(QuestImage.class, questPhotoId);

            if (image.getUserId().equals(userId)) {
                image.setDeleted("Y");
                image.setDeletedDate(new Date());
                em.merge(image);
            }
        } catch (PersistenceException pe) {
            Logger.error("QuestImageDAO :: removeQuestPhotoById : " + pe,pe);
        }
    }
}
