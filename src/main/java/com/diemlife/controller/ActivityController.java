package com.diemlife.controller;

import com.typesafe.config.Config;

import com.diemlife.dto.ActivityDTO;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.diemlife.models.Activity;
import com.diemlife.models.User;
import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.data.Form;
import play.data.FormFactory;

import com.diemlife.security.JwtSessionLogin;

import forms.ActivityCommentForm;
import com.diemlife.services.ActivityService;
import com.diemlife.services.UserProvider;

@JwtSessionLogin
public class ActivityController extends Controller {

    private final Config config;
    private final FormFactory formFactory;
    private final UserProvider userProvider;
    private final ActivityService activityService;
    private Database dbRo;

    @Inject
    public ActivityController(final Config config, final FormFactory formFactory, final UserProvider userProvider, final ActivityService activityService, @NamedDatabase("ro") Database dbRo) {
        this.config = config;
        this.formFactory = formFactory;
        this.userProvider = userProvider;
        this.activityService = activityService;
        this.dbRo = dbRo;
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivity(final @NotNull String uid) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        List<ActivityDTO> activityDtoList;
        if (!uid.isEmpty()) {
            activityDtoList = this.activityService.activityToDto(this.activityService.getActivity(uid), sessionUsername);
        } else {
            activityDtoList = new LinkedList<ActivityDTO>();
        }

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByUser(final @NotNull String username) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByUserAndTs(sessionUsername, username, null, limit);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByUserAndTs(final @NotNull String username, final @NotNull Long ts) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByUserAndTs(sessionUsername, username, ts, limit);
    }

    private Result _getActivityByUserAndTs(String sessionUsername, String username, Long ts, Integer limit) {
        List<ActivityDTO> activityDtoList;
        if (!username.isEmpty()) {
            activityDtoList = this.activityService.activityToDto(this.activityService.getActivityByUser(username, ts, limit), sessionUsername);
        } else {
            activityDtoList = new LinkedList<ActivityDTO>();
        }

        Logger.debug("_getActivityByUserAndTs - found " + activityDtoList.size() + " activity records for user " + username + " after " + ts);

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getRecentActivityByUser(final @NotNull String username) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getRecentActivityByUser(sessionUsername, username, limit);
    }

    private Result _getRecentActivityByUser(String sessionUsername, String username, Integer limit) {
        List<ActivityDTO> activityDtoList;
        if (!username.isEmpty()) {
            activityDtoList = this.activityService.activityToDto(this.activityService.getRecentActivityByUser(username, limit), sessionUsername);
        } else {
            activityDtoList = new LinkedList<ActivityDTO>();
        }

        Logger.debug("_getRecentActivityByUser - found " + activityDtoList.size() + " activity records for user " + username + " before now.");

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByQuest(final @NotNull Long questId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByQuestAndTs(sessionUsername, questId, null, limit);
    }
    
    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByQuestAndTs(final @NotNull Long questId, final @NotNull Long ts) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByQuestAndTs(sessionUsername, questId, ts, limit);
    }

    private Result _getActivityByQuestAndTs(String sessionUsername, Long questId, Long ts, Integer limit) {
        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getActivityByQuest(questId, ts, limit), sessionUsername);

        Logger.debug("_getActivityQuestAndTs - found " + activityDtoList.size() + " activity records for quest " + questId + " after " + ts);

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getRecentActivityByQuest(final @NotNull Long questId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getRecentActivityByQuest(sessionUsername, questId, limit);
    }

    private Result _getRecentActivityByQuest(String sessionUsername, Long questId, Integer limit) {
        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getRecentActivityByQuest(questId, limit), sessionUsername);

        Logger.debug("_getRecentActivityByQuest - found " + activityDtoList.size() + " activity records for quest " + questId + " before now.");

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByTask(final @NotNull Long taskId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByTaskAndTs(sessionUsername, taskId, null, limit);
    }
    
    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByTaskAndTs(final @NotNull Long taskId, final @NotNull Long ts) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByTaskAndTs(sessionUsername, taskId, ts, limit);
    }

    private Result _getActivityByTaskAndTs(String sessionUsername, Long taskId, Long ts, Integer limit) {
        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getActivityByTask(taskId, ts, limit), sessionUsername);

        Logger.debug("_getActivityTaskAndTs - found " + activityDtoList.size() + " activity records for task " + taskId + " after " + ts);

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getRecentActivityByTask(final @NotNull Long taskId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getRecentActivityByTask(sessionUsername, taskId, limit);
    }

    private Result _getRecentActivityByTask(String sessionUsername, Long taskId, Integer limit) {
        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getRecentActivityByTask(taskId, limit), sessionUsername);

        Logger.debug("_getRecentActivityByTask - found " + activityDtoList.size() + " activity records for task " + taskId + " before now.");

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByTeam(final @NotNull Long teamId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByTeamAndTs(sessionUsername, teamId, null, limit);
    }
    
    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByTeamAndTs(final @NotNull Long teamId, final @NotNull Long ts) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getActivityByTeamAndTs(sessionUsername, teamId, ts, limit);
    }

    private Result _getActivityByTeamAndTs(String sessionUsername, Long teamId, Long ts, Integer limit) {
        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getActivityByTeam(teamId, ts, limit), sessionUsername);

        Logger.debug("_getActivityTeamAndTs - found " + activityDtoList.size() + " activity records for team " + teamId + " after " + ts);

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getRecentActivityByTeam(final @NotNull Long teamId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

       // TODO: check params for limit=?
       Integer limit = null;
       return _getRecentActivityByTeam(sessionUsername, teamId, limit);
    }

    private Result _getRecentActivityByTeam(String sessionUsername, Long teamId, Integer limit) {
        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getRecentActivityByTeam(teamId, limit), sessionUsername);

        Logger.debug("_getRecentActivityByTeam - found " + activityDtoList.size() + " activity records for team " + teamId + " before now.");

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByTs(final @NotNull Long ts) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        // TODO: check params for limit=?
        Integer limit = null;

        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getActivityByTs(ts, limit), sessionUsername);

        Logger.debug("getActivityByTs - found " + activityDtoList.size() + " activity records after " + ts);

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByGeo(final @NotNull Integer level, final @NotNull Double lat, final @NotNull Double lon, final @NotNull Double radius) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        // TODO: check params for limit=?
        Integer limit = null;
        Long ts = null;

        List<ActivityDTO> activityDtoList = this.activityService.activityToDto(this.activityService.getActivityByGeo(level, lat, lon, radius, ts, limit), sessionUsername);

        Logger.debug("getActivityByGeo - found " + activityDtoList.size() + " activity records for " + lat + "," + lon + " " + radius + "m");

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result cheerActivity(final @NotNull String uid, final @NotNull String username) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (!username.equals(sessionUsername)) {
            return unauthorized();
        }

        String result = this.activityService.cheerActivity(sessionUsername, uid);

        if (result != null) {
            return ok(Json.toJson(result));
        } else {
            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result uncheerActivity(final @NotNull String uid, final @NotNull String username) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (!username.equals(sessionUsername)) {
            return unauthorized();
        }

        if (this.activityService.uncheerActivity(sessionUsername, uid)) {
            return ok();
        } else {
            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result updateActivity(final @NotNull String uid) {
        final Form<ActivityCommentForm> formBinding = formFactory.form(ActivityCommentForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final ActivityCommentForm form = formBinding.get();
        String comment = form.getComment();
        String commentImgUrl = form.getCommentImgUrl();
        if ((comment == null) && (commentImgUrl == null)) {
            return badRequest();
        }

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (this.activityService.editComment(sessionUsername, uid, comment, commentImgUrl)) {
            return ok();
        } else {
            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result deleteActivity(final @NotNull String uid) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (this.activityService.deleteComment(sessionUsername, uid)) {
            return ok();
        } else {
            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result postTargetedComment(final @NotNull String uid) {
        final Form<ActivityCommentForm> formBinding = formFactory.form(ActivityCommentForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final ActivityCommentForm form = formBinding.get();
        String comment = form.getComment();
        String commentImgUrl = form.getCommentImgUrl();
        if ((comment == null) && (commentImgUrl == null)) {
            return badRequest();
        }

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (this.activityService.postTargetedComment(sessionUsername, uid, comment, commentImgUrl)) {
            return ok();
        } else {
            return internalServerError();
        }
    }
    
    @JwtSessionLogin(required = true)
    @Transactional
    public Result postUserComment(final @NotNull String username) {
        final Form<ActivityCommentForm> formBinding = formFactory.form(ActivityCommentForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final ActivityCommentForm form = formBinding.get();
        String comment = form.getComment();
        String commentImgUrl = form.getCommentImgUrl();
        if ((comment == "") && (commentImgUrl == null)) {
            return badRequest();
        }

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (this.activityService.postUserComment(sessionUsername, username, comment, commentImgUrl)) {
            return ok();
        } else {
            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result postTeamComment(final @NotNull Long teamId) {
        final Form<ActivityCommentForm> formBinding = formFactory.form(ActivityCommentForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final ActivityCommentForm form = formBinding.get();
        String comment = form.getComment();
        String commentImgUrl = form.getCommentImgUrl();
        if ((comment == null) && (commentImgUrl == null)) {
            return badRequest();
        }

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (this.activityService.postTeamComment(sessionUsername, teamId, comment, commentImgUrl)) {
            return ok();
        } else {
            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result postQuestComment(final @NotNull Long questId) {
        final Form<ActivityCommentForm> formBinding = formFactory.form(ActivityCommentForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final ActivityCommentForm form = formBinding.get();
        String comment = form.getComment();
        String commentImgUrl = form.getCommentImgUrl();
        if ((comment == null) && (commentImgUrl == null)) {
            return badRequest();
        }

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        if (this.activityService.postQuestComment(sessionUsername, questId, null, comment, commentImgUrl)) {
            return ok();
        } else {
            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result postTaskComment(final @NotNull Long taskId) {
        // Unimplemented for now
        return forbidden();
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getCheersForActivity(final @NotNull String uid) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        // TODO: ensure the logged in user can see cheers for this activity's owner

        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getCheersForActivity(uid), sessionUsername);
        Logger.debug("getCheersForActivity - found " + activityDtoList.size() + " records");

        return ok(Json.toJson(activityDtoList));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getCommentsForActivity(final @NotNull String uid) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String sessionUsername = user.getUserName();

        // TODO: ensure the logged in user can see comments for this activity's owner

        List<ActivityDTO> activityDtoList;
        activityDtoList = this.activityService.activityToDto(this.activityService.getCommentsForActivity(uid), sessionUsername);
        Logger.debug("getCommentsForActivity - found " + activityDtoList.size() + " records");

        return ok(Json.toJson(activityDtoList));
    } 
}
