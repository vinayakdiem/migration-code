package com.diemlife.controller;

import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.QuestMapRouteService;
import services.UserProvider;

import javax.inject.Inject;
import java.io.File;

import static com.diemlife.utils.QuestSecurityUtils.canEditQuest;

@JwtSessionLogin
public class QuestMapAdminController extends Controller {

    private static final String FILE_NAME_GPX = "gpxFile";

    private final UserProvider userProvider;
    private final QuestMapRouteService questMapRouteService;
    private final JPAApi jpaApi;

    @Inject
    public QuestMapAdminController(final UserProvider userProvider,
                                   final JPAApi jpaApi) {
        this.userProvider = userProvider;
        this.questMapRouteService = new QuestMapRouteService(jpaApi);
        this.jpaApi = jpaApi;
    }

    /**
     * Upload file gpx to server and save quest map route
     *
     * @param questId questId
     * @return {@link Result}
     */
    @JwtSessionLogin(required = true)
    public Result uploadGpxFileByQuest(final Integer questId) {
        final User user = jpaApi.withTransaction(entityManager -> {
            final User sessionUser = this.userProvider.getUser(session());
            final Quests quest = entityManager.find(Quests.class, questId);
            if (quest != null && canEditQuest(quest, sessionUser)) {
                return sessionUser;
            } else {
                return null;
            }
        });
        if (user == null) {
            return unauthorized();
        }
        Http.MultipartFormData<File> multipartFormData = request().body().asMultipartFormData();
        return questMapRouteService.uploadGPXFile(multipartFormData.getFile(FILE_NAME_GPX),
                multipartFormData.asFormUrlEncoded(),
                questId);
    }

    /**
     * Get quest map routes by quest
     *
     * @param questId quest id
     * @return collection {@link dto.GPXQuestMapDTO}
     */
    @Transactional
    public Result getQuestMapRoutesByQuest(final Integer questId) {
        return questMapRouteService.getQuestMapRoutesByQuest(questId);
    }

    /**
     * Toggle all quest map route by quest
     *
     * @param questId questId
     * @param flag    flag (true | false)
     * @return {@link Result}
     */
    @JwtSessionLogin(required = true)
    @Transactional
    public Result toggleAllQuestMapRoute(final Integer questId, final Boolean flag) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        return questMapRouteService.toggleAllQuestMapRoute(questId, user, flag);
    }

    /**
     * Toggle status quest map route by quest
     *
     * @param questId questId
     * @return {@link Result}
     */
    @JwtSessionLogin(required = true)
    @Transactional
    public Result toggleStatusQuestMapRoute(final Integer questId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        return questMapRouteService.toggleStatusQuestMapRoute(questId);
    }
}
