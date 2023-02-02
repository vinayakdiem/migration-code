package com.diemlife.controller;

import com.fasterxml.jackson.databind.node.TextNode;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.dao.LinkedAccountHome;
import com.diemlife.dao.TokenActionHome;
import com.diemlife.dao.UserHome;
import exceptions.StripeApiCallException;
import com.diemlife.models.LinkedAccount;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.StripeCustomer;
import com.diemlife.models.TokenAction;
import com.diemlife.models.User;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Result;
import com.diemlife.providers.MyUsernamePasswordAuthProvider;
import com.diemlife.providers.MyUsernamePasswordAuthProvider.MyIdentity;
import com.diemlife.providers.MyUsernamePasswordAuthUser;
import com.diemlife.security.JwtSessionLogin;
import com.diemlife.security.UsernamePasswordAuth;
import com.diemlife.services.AmazonSESService;
import com.diemlife.services.StripeAccountCreator;
import com.diemlife.services.StripeConnectService;
import com.diemlife.services.UserProvider;
import com.diemlife.utils.ResponseUtility;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Arrays;

import static com.feth.play.module.pa.controllers.AuthenticateBase.noCache;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

@JwtSessionLogin
public class Signup extends Controller implements StripeAccountCreator {

    public static class PasswordReset extends Account.PasswordChange {

        public String token;

        public PasswordReset() {
        }

        public PasswordReset(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    private final Form<PasswordReset> PASSWORD_RESET_FORM;

    private final Form<MyIdentity> FORGOT_PASSWORD_FORM;

    private final UserProvider userProvider;

    private final MyUsernamePasswordAuthProvider userPaswAuthProvider;

    private final MessagesApi msg;

    private final JPAApi jpaApi;

    private final StripeConnectService stripeConnectService;

    private final FormFactory formFactory;

    @Inject
    private Config config;

    @Inject
    public Signup(final UserProvider userProvider,
                  final MyUsernamePasswordAuthProvider userPaswAuthProvider,
                  final FormFactory formFactory, final MessagesApi msg, JPAApi api,
                  final StripeConnectService stripeConnectService) {
        this.userProvider = userProvider;
        this.userPaswAuthProvider = userPaswAuthProvider;
        this.formFactory = formFactory;
        this.PASSWORD_RESET_FORM = formFactory.form(PasswordReset.class);
        this.FORGOT_PASSWORD_FORM = formFactory.form(MyIdentity.class);

        this.msg = msg;

        this.jpaApi = api;

        this.stripeConnectService = stripeConnectService;
    }

    @Override
    public StripeConnectService stripeConnectService() {
        return stripeConnectService;
    }

    @Override
    public JPAApi jpaApi() {
        return jpaApi;
    }

    @Transactional
    @JwtSessionLogin
    public Result doForgotPassword() {
        EntityManager em = this.jpaApi.em();
        noCache(response());
        Form<MyIdentity> filledForm = FORGOT_PASSWORD_FORM.bindFromRequest();
        if (filledForm.hasErrors()) {
            // User did not fill in his/her email
            //em.close();
            return badRequest();
        } else {
            // The email address given *BY AN UNKNWON PERSON* to the form - we
            // should find out if we actually have a user with this email
            // address and whether password login is enabled for him/her. Also
            // only send if the email address of the user has been verified.
            String email = filledForm.get().getEmail();

            // We don't want to expose whether a given email address is signed
            // up, so just say an email has been sent, even though it might not
            // be true - that's protecting our user privacy.
            flash(Application.FLASH_MESSAGE_KEY,
                    this.msg.preferred(request()).at(
                            "playauthenticate.reset_password.message.instructions_sent",
                            email));

            User user = UserHome.findByEmail(email, em);
            if (user != null) {
                // yep, we have a user with this email that is active - we do
                // not know if the user owning that account has requested this
                // reset, though.
                MyUsernamePasswordAuthProvider provider = this.userPaswAuthProvider;
                // User exists
                if (user.getEmailValidated()) {
                    provider.sendPasswordResetMailing(user, ctx());
                    // In case you actually want to let (the unknown person)
                    // know whether a user was found/an email was sent, use,
                    // change the flash message
                } else {
                    // We need to change the message here, otherwise the user
                    // does not understand whats going on - we should not verify
                    // with the password reset, as a "bad" user could then sign
                    // up with a fake email via OAuth and get it verified by an
                    // a unsuspecting user that clicks the link.
                    flash(Application.FLASH_MESSAGE_KEY,
                            this.msg.preferred(request()).at("playauthenticate.reset_password.message.email_not_verified"));

                    // You might want to re-send the verification email here...
                    provider.sendVerifyEmailMailingAfterSignup(user, ctx());
                }
            }
            return redirect(routes.Application.index());
        }
    }

    private TokenAction tokenIsValid(final String token, final String type) {
        final TokenActionHome tokenDao = new TokenActionHome();
        if (token != null && !token.trim().isEmpty()) {
            final TokenAction ta = tokenDao.findByToken(token, type, jpaApi.em());
            if (ta == null) {
                return null;
            } else if (!ta.isValid()) {
                Logger.warn(format("Token [%s] of type '%s' is invalid", token, type));
                return null;
            } else {
                return ta;
            }
        }

        return null;
    }

    @Transactional
    @JwtSessionLogin
    public Result resetPassword(String token) {
        noCache(response());
        TokenAction ta = tokenIsValid(token, "PASSWORD_RESET");
        if (ta == null) {
            return badRequest();
        }

        return ok();
    }

    @Transactional
    @JwtSessionLogin
    public Result doResetPassword() {
        noCache(response());

        Form<PasswordReset> filledForm = PASSWORD_RESET_FORM.bindFromRequest();
        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            String token = filledForm.get().token;
            String newPassword = filledForm.get().password;

            TokenAction ta = tokenIsValid(token, "PASSWORD_RESET");
            if (ta == null) {
                return badRequest(filledForm.errorsAsJson());
            }

            EntityManager em = this.jpaApi.em();
            TokenActionHome tokenDao = new TokenActionHome();

            ta = tokenDao.findById(ta.getId(), em);

            try {
                // Pass true for the second parameter if you want to
                // automatically create a password and the exception never to
                // happen
                UserHome userDao = new UserHome();

                userDao.resetPassword(ta.getUser(), new MyUsernamePasswordAuthUser(newPassword), em);
            } catch (RuntimeException re) {
                Logger.info("User has not yet been set up for password usage", re);
                flash(Application.FLASH_MESSAGE_KEY, this.msg.preferred(request()).at("playauthenticate.reset_password.message.no_password_account"));
            }
            boolean login = this.userPaswAuthProvider
                    .isLoginAfterPasswordReset();

            AmazonSESService.sendPasswordResetConfirmation(
                    request(),
                    config.getString("aws.ses.username"),
                    config.getString("aws.ses.password"),
                    ta.getUser());

            if (login) {
                // automatically log in
                flash(Application.FLASH_MESSAGE_KEY,
                        this.msg.preferred(request()).at("playauthenticate.reset_password.message.success.auto_login"));

                Logger.info("Logging in and redirecting...");
                // send the user to the login page
                return ok();
            } else {
                // send the user to the login page
                return ok();
            }
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result accountStatus() {
        noCache(response());
        final DynamicForm form = formFactory.form().bindFromRequest(request());
        final String email = form.get("email");
        if (isBlank(email)) {
            return badRequest();
        }
        return ok(TextNode.valueOf(userProvider.getAccountStatusByEmail(email).name()));
    }

    @Transactional
    @JwtSessionLogin
    public Result verify(String token) {

        EntityManager em = this.jpaApi.em();
        noCache(response());
        TokenAction ta = tokenIsValid(token, "EMAIL_VERIFICATION");
        if (ta == null) {
            return redirectToLogin();
        }
        TokenActionHome tokenDao = new TokenActionHome();

        ta = tokenDao.findById(ta.getId(), em);

        String email = ta.getUser().getEmail();

        UserHome userDao = new UserHome();

        final User verified = userDao.verify(ta.getUser(), em);
        createStripeCustomerIfDoesNotExist(verified);

        flash(Application.FLASH_MESSAGE_KEY,
                this.msg.preferred(request()).at("playauthenticate.verify_email.success", email));

        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        final LinkedAccount account = new LinkedAccountHome().findByProviderKey(verified, UsernamePasswordAuthProvider.PROVIDER_KEY, em);
        final UsernamePasswordAuth auth = new UsernamePasswordAuth(verified.getEmail(), EMPTY);
        if (auth.checkPassword(account.getProviderUserId())) {
            final String passwordResetToken = userPaswAuthProvider.generatePasswordResetRecord(verified);
            return redirect(envUrl + "/passwordReset/" + passwordResetToken);
        } else if (this.userProvider.getUser(session()) != null) {
            return redirect(envUrl + "/explore");
        } else {
            return redirect(envUrl);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result verifyByUserId(String userId) {

        EntityManager em = this.jpaApi.em();
        noCache(response());
        TokenAction ta = TokenActionHome.findByUserId(Integer.valueOf(userId), "EMAIL_VERIFICATION", em);
        if (ta == null) {
            return redirectToLogin();
        }
        TokenActionHome tokenDao = new TokenActionHome();

        ta = tokenDao.findById(ta.getId(), em);

        UserHome userDao = new UserHome();

        final User verified = userDao.verify(ta.getUser(), em);

        flash(Application.FLASH_MESSAGE_KEY,
                this.msg.preferred(request()).at("playauthenticate.verify_email.success", verified.getEmail()));

        return ok();
    }

    private void createStripeCustomerIfDoesNotExist(final User user) {
        if (user.getStripeEntity() instanceof StripeCustomer) {
            Logger.info(format("Stripe customer already exists for user '%s' having customer ID = %s", user.getEmail(), ((StripeCustomer) user.getStripeEntity()).stripeCustomerId));
            return;
        }
        try {
            final StripeCustomer customer = createStripeCustomer(user);
            if (customer != null) {
                Logger.info(format("Created StripeCustomer for User '%s' having customer ID = %s", user.getEmail(), customer.stripeCustomerId));
            }
        } catch (final StripeApiCallException e) {
            Logger.error(format("Unable to create Stripe account entities for user %s", user.getEmail()), e);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result activateStripeAccount() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        try {
            final StripeAccount account = createStripeAccount(user, user.getCountry(), request().remoteAddress());
            if (account != null) {
                Logger.info(format("Created StripeAccount for User '%s' having custom account ID = %s and customer ID = %s", user.getEmail(), account.stripeAccountId, account.stripeCustomerId));
                return ok(ResponseUtility.getSuccessMessageForResponse("successfully created Stripe account"));
            }
        } catch (final StripeApiCallException e) {
            Logger.error(format("Unable to create StripeAccount for user %s", user.getEmail()), e);
            final StripeException cause = e.getCause();
            if (cause instanceof InvalidRequestException) {
                final InvalidRequestException realCause = InvalidRequestException.class.cast(cause);
                return badRequest(TextNode.valueOf(realCause.getParam()));
            }
            return internalServerError(ResponseUtility.getErrorMessageForResponse("error creating Stripe account"));

        }
        return ok(ResponseUtility.getErrorMessageForResponse("error creating Stripe connected account"));
    }

    @Transactional
    public Result toLogin() {
        return redirectToLogin();
    }

    private Result redirectToLogin() {
        final String envKey = config.getString("play.env");
        if (Arrays.stream(DeploymentEnvironments.values()).anyMatch(env -> env.name().equalsIgnoreCase(envKey))) {
            final DeploymentEnvironments env = DeploymentEnvironments.valueOf(envKey);
            return redirect(config.getString(env.getBaseUrlKey()));
        } else {
            return redirect("/");
        }
    }

}
