package com.diemlife.services;

import com.google.common.base.CaseFormat;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.dao.ExploreCategoriesDAO;
import com.diemlife.dao.QuestCategoryDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dto.NaturalLanguageResults;
import com.diemlife.dto.QuestCategoryDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import com.diemlife.models.ExploreCategories;
import com.diemlife.models.QuestCategory;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import play.Logger;


import static java.util.Collections.emptyList;
import static lombok.Lombok.checkNotNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

@Service
public class QuestCategoryService {

	@Autowired
    private NaturalLanguageRepository repository;
	
	@Autowired
	private Config config;

    /**
     * Given a Quest, we want to be able to classify it via Google's Natural Lang API
     *
     * @param questId - the Quest to be classified
     */
    public void classify(final Integer questId) {
        checkNotNull(questId, "questId is required");

//        FIXME Vinayak
//        Quests quest = jpaApi.withTransaction(em -> QuestsDAO.findById(questId, em));

//        if (quest != null) {
//            final String questDescription = prepareQuestInfo(quest);
//
//            List<NaturalLanguageResults> results;
//
//            if (new StringTokenizer(questDescription).countTokens() < 20) {
//                Logger.info("Won't classify Quest " + questId + " with Google as its API requires the text to have at least 20 words");
//
//                results = emptyList();
//            } else {
//                results = repository.classify(questDescription);
//
//                if (results.isEmpty()) {
//                    Logger.info("Quest " + questId + " not classified with Google API");
//                }
//            }
//
//            List<QuestCategory> categories = toQuestCategory(results, quest);
//            List<QuestCategory> currentCategories = findCategories(questId);

//            saveResults(removeDuplicateCategories(currentCategories, categories));
//
//            classifyExplore(quest);

//        }
    }

    private void classifyExplore(Quests quest) {
//      FIXME Vinayak
//        List<QuestCategory> questCategories = jpaApi.withTransaction(em -> new QuestCategoryDAO(em).findQuestCategory(quest.getId()));
//        List<ExploreCategories> exploreCategories = toExploreCategory(questCategories);
        List<ExploreCategories> currentExploreCategories = findAllExploreCategories();

//        if (!questCategories.isEmpty()) {
//            questCategories.sort(Comparator.comparingDouble(o -> -o.getConfidence()));
//            quest.setCategory(prepareExploreCategory(questCategories.get(0).getCategory()));
//            jpaApi.withTransaction(em -> em.merge(quest));
//        }
//
//        saveToExploreCategories(removeDuplicateExploreCategories(currentExploreCategories, exploreCategories));
    }

    /**
     * Fetches the saved categories for an individual Quest
     *
     * @param questId - the Quest to be searched on
     * @return - a List of categories from quest_category
     */
    public List<QuestCategory> findCategories(final Integer questId) {
        checkNotNull(questId, "questId");

//      FIXME Vinayak
        return null;
//        return jpaApi.withTransaction(em -> new QuestCategoryDAO(em).findQuestCategory(questId));
    }

    private List<ExploreCategories> findAllExploreCategories() {
//      FIXME Vinayak
    	return null;
//        return jpaApi.withTransaction(ExploreCategoriesDAO::findAllExploreCategories);
    }

    @SuppressWarnings("unused")
    public QuestCategory findHighestRankedCategory(final Integer questId) {
        checkNotNull(questId, "questId");

//      FIXME Vinayak
//        List<QuestCategory> questCategories = jpaApi.withTransaction(em -> new QuestCategoryDAO(em).findQuestCategory(questId));
//        List<QuestCategory> userModifiedCategories = questCategories.stream().filter(QuestCategory::isUserModified).collect(Collectors.toList());
//
//        if (!userModifiedCategories.isEmpty()) {
//            return userModifiedCategories.get(0);
//        } else {
//            return jpaApi.withTransaction(em -> new QuestCategoryDAO(em).findHighestRankedQuestCategory(questId));
//        }
    }

    /**
     * fetches the proper environment URL to be used for the returned links
     *
     * @return - base url of the environment we are deployed to
     */
    public String findUrl() {
        return config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
    }

    /**
     * loops through all quests in the repository and re-classifies them
     */
    public void bulkUpdateQuestClassifications() {

//      FIXME Vinayak
//        List<Quests> quests = jpaApi.withTransaction(QuestsDAO::all);

//        quests.forEach(q -> classify(q.getId()));
    }

    public List<Quests> findSimilarQuests(final String category, final Integer startPos, final User user) {
        final List<String> preparedCategories = decodeCategory(category);

        List<Integer> questIds = new QuestCategoryDAO().findSimilarQuestCategories(preparedCategories, startPos);

//      FIXME Vinayak
//        return removeDuplicatesFrom(QuestsDAO.findAllQuestsByIdsWithACL(questIds).getList(user));
    }

    /**
     * finds all Quest Categories, without any duplicates
     *
     * @return - a list of {@link QuestCategoryDTO}
     */
    public List<QuestCategoryDTO> findAll() {
//      FIXME Vinayak
//        List<QuestCategory> questCategories = jpaApi.withTransaction(em -> new QuestCategoryDAO().findAll());
//        final String url = findUrl();
//
//        return QuestCategoryDTO.listToDTO(questCategories, url);
    }

    private void saveResults(List<QuestCategory> results) {
//      FIXME Vinayak
//        results.forEach(category -> jpaApi.withTransaction(em -> new QuestCategoryDAO().save(category, QuestCategory.class)));
    }

    private void saveToExploreCategories(List<ExploreCategories> results) {
//      FIXME Vinayak
//        results.forEach(category -> jpaApi.withTransaction(em -> ExploreCategoriesDAO.addExploreCategory(category)));
    }

    /**
     * Convert a list of NaturalLanguageResults from the Google API to a List of QuestCategories
     *
     * @param results - results we got from the API
     * @param quest   - the current quest being used
     * @return - a list of QuestCategories to be saved
     */
    private List<QuestCategory> toQuestCategory(List<NaturalLanguageResults> results, final Quests quest) {
        List<QuestCategory> categories = new ArrayList<>();
        results.forEach(res -> {
            QuestCategory category = new QuestCategory();
            category.setQuestId(quest.getId());
            category.setCategory(prepareQuestCategory(res.getCategory(), quest.getPillar()));
            category.setConfidence(res.getConfidence());
            categories.add(category);
        });
        return categories;
    }

    private List<ExploreCategories> toExploreCategory(List<QuestCategory> results) {
        List<ExploreCategories> categories = new ArrayList<>();
        results.forEach(res -> {
            ExploreCategories category = new ExploreCategories();
            category.setCategory(prepareExploreCategory(res.getCategory()));
            category.setIncluded(true);
            category.setOrder(0);
            categories.add(category);
        });
        return categories;
    }

    /**
     * We dont want to save any duplicate categories, so this is a way to ensure we dont
     *
     * @param oldCategories - list of categories we already have saved
     * @param newCategories - new list of categories we just fetched
     * @return - a list of non-duped categories, or empty if they are all dupes
     */
    private List<QuestCategory> removeDuplicateCategories(List<QuestCategory> oldCategories, List<QuestCategory> newCategories) {
        List<QuestCategory> categories = new ArrayList<>();

        for (QuestCategory category : newCategories) {
            if (!oldCategories.contains(category)) {
                categories.add(category);
            }
        }
        return categories;
    }

    private List<ExploreCategories> removeDuplicateExploreCategories(List<ExploreCategories> oldCategories, List<ExploreCategories> newCategories) {
        List<ExploreCategories> categories = new ArrayList<>();

        for (ExploreCategories category : newCategories) {
            if (!oldCategories.contains(category)) {
                categories.add(category);
            }
        }
        return categories;
    }

    /**
     * Removing the HTML from descriptions and appending it all together for the Google
     * Natural Language API to handle
     *
     * @param quest - quest for classification
     * @return all fields concatenated into a String for easy processing by the API
     */
    private String prepareQuestInfo(final Quests quest) {
        final StringBuilder builder = new StringBuilder();
        final String description = Jsoup.parse(quest.getQuestFeed()).text();

        return builder.append(quest.getTitle())
                .append(" ")
                .append(quest.getPillar())
                .append(" ")
                .append(quest.getShortDescription())
                .append(" ")
                .append(description)
                .toString();
    }

    /**
     * Add the Pillar/category code of the Quest as first level of category
     *
     * @param category - category gotten back from the Google API
     * @param pillar   - the pillar/category code of the Quest
     * @return - a formatted category to be persisted
     */
    private String prepareQuestCategory(final String category, final String pillar) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, pillar) + category;
    }

    private String prepareExploreCategory(final String category) {
        final String[] tokens = StringUtils.split(category, '/');
        if (tokens == null || tokens.length < 2) {
            return null;
        } else {
            return trimToNull(tokens[1]);
        }
    }

    private List<String> decodeCategory(final String category) {
        if (category == null) {
            return emptyList();
        }
        final String[] cat = category.split("-");
        return Arrays.asList(cat);
    }

    private List<Quests> removeDuplicatesFrom(final List<Quests> quests) {
        if (!quests.isEmpty()) {
            Set<Quests> temp = new HashSet<>(quests);
            quests.clear();
            quests.addAll(temp);

            return quests;
        }
        return emptyList();
    }

}
