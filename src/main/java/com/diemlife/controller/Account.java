package com.diemlife.controller;

import com.feth.play.module.pa.controllers.Authenticate;
import constants.JpaConstants;
import dao.UserHome;
import models.User;
import play.data.Form;
import play.data.FormFactory;
import play.data.format.Formats.NonEmpty;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Result;
import providers.MyUsernamePasswordAuthProvider;
import providers.MyUsernamePasswordAuthUser;
import security.JwtSessionLogin;
import services.ContentReportingService;
import services.OutgoingEmailService;
import services.UserProvider;

import javax.inject.Inject;
import javax.persistence.EntityManager;

@JwtSessionLogin
public class Account extends Controller {

    public static class Accept {

        @Required
        @NonEmpty
        public Boolean accept;

        public Boolean getAccept() {
            return accept;
        }

        public void setAccept(Boolean accept) {
            this.accept = accept;
        }

    }

    public static class PasswordChange {
        @MinLength(8)
        @Required
        public String password;

        @MinLength(8)
        @Required
        public String repeatPassword;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRepeatPassword() {
            return repeatPassword;
        }

        public void setRepeatPassword(String repeatPassword) {
            this.repeatPassword = repeatPassword;
        }
    }

    private final Form<Account.PasswordChange> PASSWORD_CHANGE_FORM;

    private final UserProvider userProvider;
    private final MyUsernamePasswordAuthProvider myUsrPaswProvider;

    private final ContentReportingService contentReportingService;
    private final OutgoingEmailService emailService;

    private final MessagesApi msg;

    private final JPAApi jpaApi;

    @Inject
    public Account(final UserProvider userProvider,
                   final MyUsernamePasswordAuthProvider myUsrPaswProvider,
                   final FormFactory formFactory,
                   final ContentReportingService contentReportingService,
                   final OutgoingEmailService emailService,
                   final MessagesApi msg,
                   final JPAApi api) {
        this.userProvider = userProvider;
        this.myUsrPaswProvider = myUsrPaswProvider;

        this.PASSWORD_CHANGE_FORM = formFactory.form(Account.PasswordChange.class);

        this.contentReportingService = contentReportingService;
        this.emailService = emailService;
        this.msg = msg;

        this.jpaApi = api;
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result verifyEmail() {
        Authenticate.noCache(response());
        User user = this.userProvider.getUser(session());
        if (user == null) {
            return notFound();
        }
        if (user.getEmailValidated()) {
            // E-Mail has been validated already
            flash(Application.FLASH_MESSAGE_KEY,
                    this.msg.preferred(request()).at("playauthenticate.verify_email.error.already_validated"));
        } else if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            flash(Application.FLASH_MESSAGE_KEY, this.msg.preferred(request()).at(
                    "playauthenticate.verify_email.message.instructions_sent",
                    user.getEmail()));
            this.myUsrPaswProvider.sendVerifyEmailMailingAfterSignup(user, ctx());
        } else {
            flash(Application.FLASH_MESSAGE_KEY, this.msg.preferred(request()).at(
                    "playauthenticate.verify_email.error.set_email_first",
                    user.getEmail()));
        }
        //return redirect(routes.Application.profile());
        return ok();
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result doChangePassword() {
        EntityManager em = this.jpaApi.em(JpaConstants.DB);
        Authenticate.noCache(response());
        Form<Account.PasswordChange> filledForm = PASSWORD_CHANGE_FORM
                .bindFromRequest();
        if (filledForm.hasErrors()) {
            // User did not select whether to link or not link
            return badRequest();
        } else {
            User user = this.userProvider.getUser(session());
            String newPassword = filledForm.get().password;

            UserHome userDao = new UserHome();

            userDao.changePassword(user, new MyUsernamePasswordAuthUser(newPassword), em);

            flash(Application.FLASH_MESSAGE_KEY,
                    this.msg.preferred(request()).at("playauthenticate.change_password.success"));

            return ok();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result reportUser(Integer userId) {
        final User reporter = this.userProvider.getUser(session());
        final User accusedUser = UserHome.findById(userId, jpaApi.em());
        if (accusedUser == null) {
            return notFound();
        }
        contentReportingService.reportUser(accusedUser, reporter);
        emailService.sendContentReportEmail(request(), reporter, User.class.getSimpleName(), accusedUser.getId());
        return ok();
    }

}
