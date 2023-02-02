package com.diemlife.controller;

import com.diemlife.action.BasicAuth;
import com.diemlife.dto.QuestCategoryDTO;
import com.diemlife.models.QuestCategory;
import com.diemlife.models.User;
import org.springframework.util.StopWatch;
import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.QuestCategoryService;
import services.UserProvider;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public class QuestClassificationController extends Controller {

    private final QuestCategoryService service;
    private final UserProvider userProvider;

    @Inject
    public QuestClassificationController(final QuestCategoryService service, final UserProvider userProvider) {
        this.service = service;
        this.userProvider = userProvider;
    }

    @Transactional
    public Result saveClassifications(Integer questId) {
        checkNotNull(questId, "questId");

        service.classify(questId);

        return ok();
    }

    @Transactional
    public Result findClassifications(final Integer questId) {
        checkNotNull(questId, "questId");

        Logger.info(format("fetching quest classifications for [%s]", questId));

        List<QuestCategory> categories = service.findCategories(questId);
        final String url = service.findUrl();

        List<QuestCategoryDTO> dtos = QuestCategoryDTO.listToDTO(categories, url);
        Logger.info(format("found [%s] quest classifications for quest [%s]", dtos.size(), questId));

        return ok(Json.toJson(dtos));
    }

    /**
     * the service invocation needs to be async - TODO
     */
    @Transactional
    @BasicAuth
    public Result saveClassificationsJob() {
        StopWatch watch = new StopWatch();
        User user = (User) ctx().args.get("api-user");

        if (user == null) {
            return unauthorized();
        }

        Logger.info(format("starting quest classifications job. started by user [%s]", user.getId()));
        watch.start();
        service.bulkUpdateQuestClassifications();
        watch.stop();

        Logger.info(format("completed quest classifications job in [%s]ms", watch.getTotalTimeMillis()));
        return ok();
    }
}
