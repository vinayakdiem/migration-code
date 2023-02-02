package com.diemlife.controller;

import com.diemlife.action.BasicAuth;
import lombok.NonNull;
import com.diemlife.models.User;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import com.diemlife.services.PushNotificationService;
import com.diemlife.services.UserProvider;

import javax.inject.Inject;
import java.util.Optional;

public class PushNotificationController extends Controller {

    @NonNull
    private final UserProvider userProvider;
    @NonNull
    private final PushNotificationService pushNotificationService;
    @NonNull
    private final FormFactory formFactory;
    @NonNull
    private final JPAApi jpaApi;

    @Inject
    public PushNotificationController(UserProvider userProvider, PushNotificationService pushNotificationService,
                                      FormFactory formFactory, JPAApi jpaApi) {
        this.pushNotificationService = pushNotificationService;
        this.userProvider = userProvider;
        this.formFactory = formFactory;
        this.jpaApi = jpaApi;
    }

    @Transactional
    public Result registerDevice(@NonNull final String token) {
        final User currentUser = userProvider.getUser(session());

        if (currentUser == null) {
            return badRequest();
        }

        final Optional<String> newDeviceToken = pushNotificationService.registerNewDevice(token, currentUser);

        return newDeviceToken.map(Results::ok).orElseGet(Results::ok);
    }

    @Transactional
    @BasicAuth
    public Result sendNotificationForQuestDoers(final int questId) {

        User user = (User) ctx().args.get("api-user");
        user = this.jpaApi.em().find(models.User.class, user.getId());

        if (user == null) {
            return unauthorized();
        }

        final String messageTitle = formFactory.form().bindFromRequest().get("messageTitle");
        final String messageText = formFactory.form().bindFromRequest().get("messageText");
        final String questActivityStatus = formFactory.form().bindFromRequest().get("questActivityStatus");
        final String questActivityGroup = formFactory.form().bindFromRequest().get("questActivityGroup");

        pushNotificationService.sendNotificationToQuestFollowers(questId, messageTitle, messageText, questActivityStatus,
                questActivityGroup);

        return ok();
    }

    @Transactional
    public Result pushNotificationOpened(@NonNull final String messageId) {

        pushNotificationService.updateMessageInfoForUserOpening(messageId);

        return ok();
    }

}
