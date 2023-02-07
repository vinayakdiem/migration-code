package com.diemlife.dao;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import com.diemlife.models.ExploreCategories;
import com.diemlife.models.QuestCategory;
import com.diemlife.models.Quests;

import play.Logger;

@Repository
public class ExploreCategoriesDAO {

	@PersistenceContext
	EntityManager em;
	
    public List<ExploreCategories> findEnabledExploreCategories() {
        return em.createQuery("SELECT ec FROM ExploreCategories ec WHERE ec.included = TRUE ORDER BY ec.order", ExploreCategories.class)
                .getResultList();
    }

    public List<ExploreCategories> addExploreCategoriesOfExistQuests() {

        List<ExploreCategories> exploreCategoriesList = new ArrayList<>();

        try {
            List<QuestCategory> questCategories = em.createQuery("SELECT q FROM QuestCategory q where q.category is not null", QuestCategory.class).getResultList();
          //FIXME Vinayak
//            Set<String> uniqueCategories = questCategories.stream().map(QuestCategory::getCategory).collect(Collectors.toSet());
            questCategories.sort(new SortByConfidence());
          //FIXME Vinayak
//            Map<String, Integer> uniqueCategoriesMap = questCategories.stream().collect(Collectors.toMap(QuestCategory::getCategory, QuestCategory::getQuestId, (oldValue, newValue) -> oldValue));

          //FIXME Vinayak
//            if (!uniqueCategoriesMap.isEmpty()) {
//                uniqueCategoriesMap.forEach((category, questId) -> {
//                    Quests quest = QuestsDAO.findById(questId, em);
//                    if (quest != null) {
//                        quest.setCategory(prepareExploreCategory(category));
//                        em.merge(quest);
//                    }
//                });
//            }
//
          //FIXME Vinayak
//            uniqueCategories.iterator().forEachRemaining(questCategory -> {
//                if (!isCategoryExists(prepareExploreCategory((questCategory)), em) && StringUtils.isNotEmpty(prepareExploreCategory((questCategory)))) {
//                    ExploreCategories exploreCategory = new ExploreCategories();
//                    exploreCategory.setCategory(prepareExploreCategory(questCategory));
//                    exploreCategory.setIncluded(true);
//                    exploreCategory.setOrder(0);
//
//                    em.merge(exploreCategory);
//                    exploreCategoriesList.add(exploreCategory);
//                }
//            });


        } catch (PersistenceException e) {
            Logger.info("Cannot add value to ExploreCategories", e);
        }
        return exploreCategoriesList;
    }

    private String prepareExploreCategory(final String category) {

        String exploreCategory = "";
        String[] categories = StringUtils.split(category, '/');
        if (category.indexOf('/') >= 0) {
            exploreCategory = (String) Array.get(categories, 1);
        }

        return exploreCategory;
    }

    static class SortByConfidence implements Comparator<QuestCategory> {
        public int compare(QuestCategory a, QuestCategory b) {
        	//FIXME Vinayak
        	return (int) 0;
//            return (int) (b.getConfidence() - a.getConfidence());
        }
    }

    public ExploreCategories addExploreCategory(ExploreCategories category, EntityManager em) {
    	//FIXME Vinayak
    	return null;
    	
//        if (category.getId() == null) {
//            try {
//                em.persist(category);
//                em.flush();
//            } catch (final PersistenceException | IllegalArgumentException e) {
//                Logger.error("Unable to persist ExploreCategory ", e);
//                throw e;
//            }
//            return em.find(ExploreCategories.class, category.getId());
//        } else {
//            return em.merge(category);
//        }
    }

    private boolean isCategoryExists(String category, EntityManager em) {
        try {
            Long exploreCategories = em.createQuery("SELECT COUNT (ep) FROM ExploreCategories ep WHERE ep.category=:category", Long.class)
                    .setParameter("category", category)
                    .getSingleResult();
            return exploreCategories > 0;
        } catch (PersistenceException | IllegalArgumentException e) {
            Logger.info("Cannot count categories in ExploreCategories", e);
        }
        return false;
    }

    public List<ExploreCategories> findAllExploreCategories(EntityManager em) {
        try {
            return em.createQuery("SELECT ec FROM ExploreCategories ec", ExploreCategories.class)
                    .getResultList();
        } catch (PersistenceException | IllegalArgumentException e) {
            Logger.info("ExploreCategoriesDAO :: findAllExploreCategories() :: Cannot get values from ExploreCategories");
        }
        return new ArrayList<>();
    }

}
