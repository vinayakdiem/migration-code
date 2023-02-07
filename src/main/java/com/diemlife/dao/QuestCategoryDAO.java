package com.diemlife.dao;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import com.diemlife.models.QuestCategory;

import play.Logger;


public class QuestCategoryDAO extends TypedDAO<QuestCategory> {

    private static final String QUEST_ID = "questId";

    @PersistenceContext
	 private EntityManager entityManager;
    
    public List<QuestCategory> findQuestCategory(final Integer questId) {
        checkNotNull(questId, QUEST_ID);
        return entityManager.createQuery("SELECT qc FROM QuestCategory qc WHERE qc.questId = :questId", QuestCategory.class)
                .setParameter(QUEST_ID, questId)
                .getResultList();
    }

    public QuestCategory findHighestRankedQuestCategory(final Integer questId) {
        checkNotNull(questId, QUEST_ID);
        QuestCategory category;

        try {
            category = entityManager.createQuery("SELECT qc FROM QuestCategory qc WHERE qc.questId = :questId ORDER BY qc.confidence desc", QuestCategory.class)
                    .setParameter(QUEST_ID, questId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            Logger.warn(format("no quest categories found for quest [%s]", questId));
            return null;
        }
        return category;
    }

    public List<Integer> findSimilarQuestCategories(final List<String> categories, final Integer startPos) {
        List<QuestCategory> ret = new ArrayList<>();

        for (String cat : categories) {
            List<QuestCategory> cats = entityManager.createQuery("SELECT qc FROM QuestCategory qc WHERE LOWER(qc.category) LIKE (:cat)", QuestCategory.class)
                    .setParameter("cat", "%" + cat + "%")
                    .setFirstResult(startPos)
                    .setMaxResults(15)
                    .getResultList();
            ret.addAll(cats);
        }

        return ret.stream().map(QuestCategory::getQuestId).collect(Collectors.toList());
    }

    public List<QuestCategory> findAll() {
        return entityManager.createQuery("SELECT qc FROM QuestCategory qc", QuestCategory.class)
                .getResultList();
    }


}
