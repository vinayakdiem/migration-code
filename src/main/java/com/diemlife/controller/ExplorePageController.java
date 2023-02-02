package com.diemlife.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.dao.HappeningDAO;
import com.diemlife.dao.Page;
import com.diemlife.dao.QuestActivityHome;
import com.diemlife.dao.QuestSavedDAO;
import com.diemlife.dao.QuestUserFlagDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dto.ExploreCategoriesDTO;
import com.diemlife.dto.ExplorePageDTO;
import com.diemlife.dto.QuestActivityDTO;
import com.diemlife.dto.QuestDTO;
import com.diemlife.dto.QuestUserFlagsDTO;
import forms.ExploreForm;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.QuestCategoryService;
import services.UserProvider;


import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;

@JwtSessionLogin
public class ExplorePageController extends Controller {

    private final JPAApi jpaApi;
    private final UserProvider userProvider;
    private final QuestCategoryService categoryService;
    private final FormFactory formFactory;
    private final Config config;

    @Inject
    public ExplorePageController(JPAApi jpaApi,
                                 UserProvider userProvider,
                                 QuestCategoryService categoryService,
                                 FormFactory formFactory,
                                 Config config) {
        this.jpaApi = checkNotNull(jpaApi, "jpaApi");
        this.userProvider = checkNotNull(userProvider, "userProvider");
        this.categoryService = checkNotNull(categoryService, "categoryService");
        this.formFactory = checkNotNull(formFactory, "formFactory");
        this.config = checkNotNull(config, "config");
    }

    @Transactional
    @JwtSessionLogin
    @Deprecated
    public Result explorePageDetails(final Integer userId, final Integer startPosition, final Integer endPosition) {
        Logger.info("fetching explore page details");
        final EntityManager entityManager = this.jpaApi.em();
        final DynamicForm form = formFactory.form().bindFromRequest();
        final String keyword = form.get("keyword");
        final String classification = form.get("classification");
        final JsonNode node = request().body().asJson();
        final ExploreCategoriesDTO categories = Json.fromJson(node, ExploreCategoriesDTO.class);
        final List<String> categorySearch = new ArrayList<>();
        categories.getCategories().forEach(category -> {
            if (category.getIsActive()) {
                categorySearch.add(category.getTitle());
            }
        });

        List<Quests> quests;
        User user;

        if (userId != null) {
            user = UserHome.findById(userId, entityManager);
        } else {
            user = this.userProvider.getUser(session());
        }

        checkNotNull(user, "user");
        checkNotNull(startPosition, "startPosition");
        checkNotNull(endPosition, "endPosition"); // leaving this in for now, we may end up using it

        if (isNotEmpty(keyword)) {
            quests = QuestsDAO.getQuestsBySearchCriteria(keyword, entityManager).getList(user);
        } else if (isNotEmpty(classification)) {
            quests = categoryService.findSimilarQuests(classification, startPosition, user);
        } else {
            quests = QuestActivityHome.getQuestsNotInProgressForUserPaginated(user, startPosition, endPosition - startPosition, categorySearch, null, null, entityManager).getList(user);
        }

        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());

        if (!quests.isEmpty()) {
            List<QuestDTO> questDTOs = listQuestsToDTO(quests, user, envUrl, entityManager);
            final ExplorePageDTO dto = new ExplorePageDTO(questDTOs, emptyList(), emptyList());
            final JsonNode result = Json.toJson(dto);

            Logger.info("completed fetching explore page details");
            return ok(result);
        } else {
            return notFound();
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result explorePage(final Integer startPosition, final Integer limit) {
        final EntityManager entityManager = this.jpaApi.em();

        final Form<ExploreForm> formBinding = formFactory.form(ExploreForm.class).bindFromRequest();

        if (formBinding.hasErrors()) {
            Logger.info("Explore form contains invalid data : " + formBinding.errorsAsJson());
            return badRequest(formBinding.errorsAsJson());
        }

        final ExploreForm exploreForm = formBinding.get();

        User user = this.userProvider.getUser(session());

        final List<Quests> results = QuestActivityHome.getQuestsNotInProgressForUserPaginated(user, startPosition, limit, exploreForm.getPillars(), exploreForm.getCategory(), exploreForm.getPlace(), entityManager).getList(user);
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());

        final List<QuestDTO> exploreQuests;

        if (user != null) {
            exploreQuests = listQuestsToDTO(results, user, envUrl, entityManager);
        } else {
            exploreQuests = QuestDTO.listToDTO(results).stream()
                    .map(dto -> dto.withSEOSlugs(publicQuestSEOSlugs(dto, dto.user, envUrl)))
                    .collect(toList());
        }

        final boolean hasMore = exploreQuests.size() > limit;
        Page<QuestDTO> questDTOPage = new Page<QuestDTO>(startPosition, limit, hasMore).withData(hasMore ? (exploreQuests.subList(0, exploreQuests.size() - 1)) : exploreQuests);

        return ok(Json.toJson(questDTOPage));
    }

    private List<QuestDTO> listQuestsToDTO(List<Quests> results, User user, String envUrl, EntityManager entityManager) {
        final Set<Integer> followedSet = new HashSet<>(new QuestUserFlagDAO(entityManager).getQuestsBeingFollowedForUser(user));
        final Set<Integer> savedSet = new HashSet<>(QuestSavedDAO.getSavedQuestIdsForUser(user, entityManager));
        final Set<Integer> starredSet = new HashSet<>(new QuestUserFlagDAO(entityManager).retrieveStarredQuests(user));
        final List<Quests> userDoing = QuestActivityHome.getInProgressQuestsForUser(user, entityManager).getList(user);
        final Set<Integer> userDoingSet = userDoing.stream().map(Quests::getId).collect(Collectors.toSet());
        final Map<Integer, QuestActivityDTO> repeatableMap = QuestActivityHome.getRepeatableInfoForDoer(user, entityManager)
                .stream()
                .collect(toMap(dto -> dto.questId, dto -> dto));

        return QuestDTO.listToDTO(results).stream()
                .map(dto -> dto.withUserFlags(QuestUserFlagsDTO.builder()
                        .withFollowing(followedSet.contains(dto.id))
                        .withSaved(savedSet.contains(dto.id))
                        .withStarred(starredSet.contains(dto.id))
                        .build()))
                .map(dto -> dto.withHasEvent(new HappeningDAO(entityManager).isQuestHappening(dto.id)))
                .map(dto -> dto.withUserDoing(userDoingSet.contains(dto.id)))
                .map(dto -> dto.withActivityData(repeatableMap.get(dto.id)))
                .map(dto -> dto.withSEOSlugs(publicQuestSEOSlugs(dto, dto.user, envUrl)))
                .collect(toList());
    }

    @Transactional
    @JwtSessionLogin
    public Result searchResults(final String keyword) {
        final EntityManager entityManager = this.jpaApi.em();
        final List<Quests> quests = QuestsDAO.getQuestsBySearchCriteria(keyword, entityManager).getList(null);
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        return ok(Json.toJson(QuestDTO.listToDTO(quests)
                .stream()
                .map(dto -> dto.withSEOSlugs(publicQuestSEOSlugs(dto, dto.user, envUrl)))
                .collect(toList())));
    }

}
