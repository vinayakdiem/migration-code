package com.diemlife.controller;

import com.diemlife.dao.NotificationsDAO;
import com.diemlife.dto.NotificationDTO;
import forms.NotificationForm;
import com.diemlife.models.User;
import play.Logger;
import play.cache.CacheApi;
import play.cache.NamedCache;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.NotificationService;
import services.UserProvider;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

@JwtSessionLogin
public class NotificationController extends Controller {

    private final JPAApi jpaApi;
    private final NotificationService notificationService;
    private final UserProvider userProvider;
    private final CacheApi notificationsCache;
    private final FormFactory formFactory;

    @Inject
    public NotificationController(final JPAApi jpaApi,
                                  final NotificationService notificationService,
                                  final UserProvider userProvider,
                                  @NamedCache("notifications-cache") final CacheApi notificationsCache,
                                  final FormFactory formFactory) {
        this.jpaApi = checkNotNull(jpaApi, "jpaApi");
        this.userProvider = userProvider;
        this.notificationService = checkNotNull(notificationService, "notificationService");
        this.notificationsCache = checkNotNull(notificationsCache, "notificationsCache");
        this.formFactory = formFactory;
    }

    @Transactional
    @JwtSessionLogin
    public Result checkNotifications(@Nonnull final Integer userId) {
        return ok(Json.toJson(jpaApi.withTransaction(em -> new NotificationsDAO(notificationsCache).isUnreadMessages(userId, em))));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getNotifications(@Nonnull final Integer userId, @Nonnull final Integer startPos, @Nonnull final Integer endPos) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        Logger.info(format("fetching notifications for user [%s]", userId));
        List<NotificationDTO> notifications = this.notificationService.getNotifications(user, startPos, endPos, request());
        Logger.info(format("fetched [%s] notifications for user [%s]", notifications.size(), userId));
        return ok(Json.toJson(notifications));
    }

    @Transactional
    @JwtSessionLogin
    public Result updateNotification() {

        final Form<NotificationForm> form = formFactory.form(NotificationForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final NotificationForm notificationForm = form.get();
        String notificationType = notificationForm.getNotificationType();
        Integer questId = notificationForm.getQuestId();

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        EntityManager entityManager = this.jpaApi.em();
        NotificationsDAO notificationsDAO = new NotificationsDAO(notificationsCache);
        notificationsDAO.updateNotification(notificationType, questId,true, entityManager, user);

        return ok();
    }
}
