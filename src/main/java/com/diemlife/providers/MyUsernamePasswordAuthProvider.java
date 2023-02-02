package com.diemlife.providers;

import com.feth.play.module.mail.Mailer.Mail.Body;
import com.feth.play.module.mail.Mailer.MailerFactory;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.constants.JpaConstants;
import com.diemlife.controllers.routes;
import com.diemlife.dao.TokenActionHome;
import com.diemlife.dao.UserHome;
import com.diemlife.models.LinkedAccount;
import com.diemlife.models.User;
import org.joda.time.LocalDateTime;
import play.Application;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.inject.ApplicationLifecycle;
import play.mvc.Call;
import play.mvc.Http.Context;
import com.diemlife.security.UsernamePasswordAuth;
import com.diemlife.services.AmazonSESService;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@Singleton
public class MyUsernamePasswordAuthProvider
		extends
        UsernamePasswordAuthProvider<String, LoginUsernamePasswordAuthUser, MyUsernamePasswordAuthUser, MyUsernamePasswordAuthProvider.MyLogin, MyUsernamePasswordAuthProvider.MySignup> {

	private static final String SETTING_KEY_VERIFICATION_LINK_SECURE = SETTING_KEY_MAIL
			+ "." + "verificationLink.secure";
	private static final String SETTING_KEY_PASSWORD_RESET_LINK_SECURE = SETTING_KEY_MAIL
			+ "." + "passwordResetLink.secure";
	private static final String SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET = "loginAfterPasswordReset";

	private static final String EMAIL_TEMPLATE_FALLBACK_LANGUAGE = "en";

	@Override
	protected List<String> neededSettingKeys() {
		List<String> needed = new ArrayList<String>(
				super.neededSettingKeys());
		needed.add(SETTING_KEY_VERIFICATION_LINK_SECURE);
		needed.add(SETTING_KEY_PASSWORD_RESET_LINK_SECURE);
		needed.add(SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET);
		return needed;
	}

	public static class MyIdentity {

		public MyIdentity() {
		}

		public MyIdentity(final String email) {
			this.email = email;
		}

		@Required
		@Email
		protected String email;

		public String getEmail() {
			return email;
		}

		public void setEmail(final String email) {
			this.email = email;
		}
	}

	public static class MyLogin extends MyIdentity
			implements
			com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.UsernamePassword {

		@Required
		@MinLength(8)
		protected String password;

		@Override
		public String getPassword() {
			return password;
		}

		public void setPassword(final String password) {
			this.password = password;
		}
	}

	public static class MySignup extends MyLogin {

		@Required
		@MinLength(8)
		protected String repeatPassword;

		@Required
		protected String name;

		@Required
		protected String goals;

		@Required
		protected String zip;

		@Required
		protected String userName;

		@Required
		protected String receiveEmail;

		@Required
		protected String firstName;

		@Required
		protected String lastName;

		public MySignup() {
		}

		public String getRepeatPassword() {
			return repeatPassword;
		}

		public void setRepeatPassword(final String repeatPassword) {
			this.repeatPassword = repeatPassword;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getGoals() {
			return goals;
		}

		public void setGoals(final String goals) {
			this.goals = goals;
		}

		public String getZip() {
			return zip;
		}

		public void setZip(final String zip) {
			this.zip = zip;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(final String userName) {
			this.userName = userName;
		}

		public String getReceiveEmail() {
			return receiveEmail;
		}

		public void setReceiveEmail(final String receiveEmail) {
			this.receiveEmail = receiveEmail;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(final String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(final String lastName) {
			this.lastName = lastName;
		}
	}

	private final JPAApi jpaApi;
	private final MessagesApi msg;
    private final Config config;
    private final FormFactory formFactory;
    private final Provider<Application> application;


    @Inject
	public MyUsernamePasswordAuthProvider(final PlayAuthenticate auth, final FormFactory formFactory,
										  final ApplicationLifecycle lifecycle, MailerFactory mailerFactory, final JPAApi api,
										  MessagesApi msg, Config config, Provider<Application> application) {
		super(auth, lifecycle, mailerFactory);

		this.jpaApi = api;
		this.msg = msg;
        this.config = config;
        this.formFactory = formFactory;
        this.application = application;
	}

	public Form<MySignup> getSignupForm() {
		return formFactory.form(MySignup.class);
	}

	public Form<MyLogin> getLoginForm() {
		return formFactory.form(MyLogin.class);
	}

	@Override
	protected com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.SignupResult signupUser(MyUsernamePasswordAuthUser user) {

		EntityManager em = this.jpaApi.em(JpaConstants.DB);

		UserHome userDao = new UserHome();

		User u = userDao.findByUsernamePasswordIdentity(user.getEmail(), UsernamePasswordAuth.PROVIDER_KEY, em);
		if (u != null) {
			if (u.getEmailValidated()) {
				// This user exists, has its email validated and is active
				em.close();
				return SignupResult.USER_EXISTS;
			} else {
				// this user exists, is active but has not yet validated its
				// email
				em.close();
				return SignupResult.USER_EXISTS_UNVERIFIED;
			}
		}
		// The user either does not exist or is inactive - create a new one
		@SuppressWarnings("unused")
		User newUser = userDao.create(user, em);
		// Usually the email should be verified before allowing login, however
		// if you return
		// return SignupResult.USER_CREATED;
		// then the user gets logged in directly

		em.close();
		return SignupResult.USER_CREATED_UNVERIFIED;
	}

	@Override
	protected MySignup getSignup(Context ctx) {
		Context.current.set(ctx);
		final Form<MySignup> filledForm = getSignupForm().bindFromRequest();
		return filledForm.get();
	}

	@Override
	protected MyLogin getLogin(Context ctx) {
		Context.current.set(ctx);
		final Form<MyLogin> filledForm = getLoginForm().bindFromRequest();
		return filledForm.get();
	}

	@Override
    protected com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.LoginResult loginUser(LoginUsernamePasswordAuthUser authUser) {

		EntityManager em = this.jpaApi.em(JpaConstants.DB);

		UserHome userDao = new UserHome();

		User u = userDao.findByUsernamePasswordIdentity(authUser.getEmail(), UsernamePasswordAuth.PROVIDER_KEY, em);
		if (u == null) {
			em.close();
			return LoginResult.NOT_FOUND;
		} else {
			if (!u.getEmailValidated()) {
				em.close();
				return LoginResult.USER_UNVERIFIED;
			} else {
				for (LinkedAccount acc : u.getLinkedAccounts()) {
					if (getKey().equals(acc.getProviderKey())) {
						if (authUser.checkPassword(acc.getProviderUserId(), authUser.getPassword())) {
							em.getTransaction().begin();
							u.setLastLogin(LocalDateTime.now().toDate());
							em.getTransaction().commit();
							em.close();
							return LoginResult.USER_LOGGED_IN;
						} else {
							// if you don't return here,
							// you would allow the user to have
							// multiple passwords defined
							// usually we don't want this
							em.close();
							return LoginResult.WRONG_PASSWORD;
						}
					}
				}
				em.close();
				return LoginResult.WRONG_PASSWORD;
			}
		}
	}

	@Override
	protected Call userExists(UsernamePasswordAuthUser authUser) {
		return routes.Application.index();
	}

	@Override
	protected Call userUnverified(UsernamePasswordAuthUser authUser) {
		return routes.Application.index();
	}

	@Override
	protected MyUsernamePasswordAuthUser buildSignupAuthUser(MySignup signup, Context ctx) {
		return new MyUsernamePasswordAuthUser(signup);
	}

	@Override
    protected LoginUsernamePasswordAuthUser buildLoginAuthUser(MyLogin login, Context ctx) {
        return new LoginUsernamePasswordAuthUser(login.getPassword(),
				login.getEmail());
	}


	@Override
    protected LoginUsernamePasswordAuthUser transformAuthUser(MyUsernamePasswordAuthUser authUser, Context context) {
        return new LoginUsernamePasswordAuthUser(authUser.getEmail());
	}

	@Override
	protected String getVerifyEmailMailingSubject(MyUsernamePasswordAuthUser user, Context ctx) {
		return this.msg.preferred(ctx.request()).at("playauthenticate.password.verify_signup.subject");
	}

	@Override
	protected String onLoginUserNotFound(Context context) {
		context.flash()
				.put(controllers.Application.FLASH_ERROR_KEY,
						this.msg.preferred(context.request()).at("playauthenticate.password.login.unknown_user_or_pw"));

        if (config.getString("play.env").equals("LOCAL")) {
            return "http://localhost:9000";
        } else if (config.getString("play.env").equals("DEV")) {
            return "https://dev.diem.life";
        } else {
            return "https://diem.life";
        }
	}

	@Override
	protected Body getVerifyEmailMailingBody(String token,MyUsernamePasswordAuthUser user, Context ctx) {

		boolean isSecure = getConfiguration().getBoolean(
				SETTING_KEY_VERIFICATION_LINK_SECURE);
		String url = routes.Signup.verify(token).absoluteURL(
				ctx.request(), isSecure);

		Lang lang = Lang.preferred(application.get(), ctx.request().acceptLanguages());
		String langCode = lang.code();

		String html = getEmailTemplate(
				"views.html.account.signup.email.verify_email", langCode, url,
				token, user.getName(), user.getEmail());
		String text = getEmailTemplate(
				"views.txt.account.signup.email.verify_email", langCode, url,
				token, user.getName(), user.getEmail());

		return new Body(text, html);
	}

	private static String generateToken() {
		return UUID.randomUUID().toString();
	}

	@Override
	protected String generateVerificationRecord(MyUsernamePasswordAuthUser user) {
		EntityManager em = this.jpaApi.em(JpaConstants.DB);

		UserHome userDao = new UserHome();

		String verf = generateVerificationRecord(userDao.findByAuthUserIdentity(user, em));

		em.close();
		return verf;
	}

	@Transactional
	protected String generateVerificationRecord(User user) {

		EntityManager em = this.jpaApi.em();

		String token = generateToken();
		// Do database actions, etc.
		TokenActionHome tokenDao = new TokenActionHome();

		tokenDao.create("EMAIL_VERIFICATION", token, user, em);

		return token;
	}

	@Transactional
	public String generatePasswordResetRecord(User u) {

		EntityManager em = this.jpaApi.em();

		String token = generateToken();

		TokenActionHome tokenDao = new TokenActionHome();

		tokenDao.create("PASSWORD_RESET", token, u, em);

		Logger.info("TOKEN = " + token);
		return token;
	}

	protected String getPasswordResetMailingSubject(User user, Context ctx) {
		return this.msg.preferred(ctx.request()).at("playauthenticate.password.reset_email.subject");
	}

	protected Body getPasswordResetMailingBody(String token, User user, Context ctx) {
		String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
		String url = envUrl + "/passwordReset/" + token;

		Lang lang = Lang.preferred(application.get(), ctx.request().acceptLanguages());
		String langCode = lang.code();

		String html = getEmailTemplate(
				"views.html.account.email.password_reset", langCode, url,
				token, user.getFirstName(), user.getEmail());
		String text = getEmailTemplate(
				"views.txt.account.email.password_reset", langCode, url, token,
				user.getFirstName(), user.getEmail());

		return new Body(text, html);
	}

	public void sendPasswordResetMailing(User user, Context ctx) {
		String token = generatePasswordResetRecord(user);
		String subject = getPasswordResetMailingSubject(user, ctx);
		Body body = getPasswordResetMailingBody(token, user, ctx);
		sendMail(subject, body, getEmailName(user));
	}

	public boolean isLoginAfterPasswordReset() {
		return getConfiguration().getBoolean(
				SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET);
	}

	protected String getVerifyEmailMailingSubjectAfterSignup(User user,
															 Context ctx) {
		return this.msg.preferred(ctx.request()).at("playauthenticate.password.verify_email.subject");
	}

	protected String getEmailTemplate(String template,
									  String langCode, String url, String token,
									  String name, String email) {
		Class<?> cls = null;
		String ret = null;
		try {
			cls = Class.forName(template + "_" + langCode);
		} catch (ClassNotFoundException e) {
			Logger.warn("Template: '"
					+ template
					+ "_"
					+ langCode
					+ "' was not found! Trying to use English fallback template instead.", e);
		}
		if (cls == null) {
			try {
				cls = Class.forName(template + "_"
						+ EMAIL_TEMPLATE_FALLBACK_LANGUAGE);
			} catch (ClassNotFoundException e) {
				Logger.error("Fallback template: '" + template + "_"
						+ EMAIL_TEMPLATE_FALLBACK_LANGUAGE
						+ "' was not found either!", e);
			}
		}
		if (cls != null) {
			Method htmlRender = null;
			try {
				htmlRender = cls.getMethod("render", String.class,
						String.class, String.class, String.class);
				ret = htmlRender.invoke(null, url, token, name, email)
						.toString();

			} catch (NoSuchMethodException e) {
				Logger.debug("could not find method: ", e);
			} catch (IllegalAccessException e) {
				Logger.debug("could not find method: ", e);
			} catch (InvocationTargetException e) {
				Logger.debug("could not find method: ", e);
			}
		}
		return ret;
	}

	protected Body getVerifyEmailMailingBodyAfterSignup(String token,
														User user, Context ctx) {

		String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
		String url = envUrl + "/api/accounts/verify/" + token;

		Lang lang = Lang.preferred(application.get(), ctx.request().acceptLanguages());
		String langCode = lang.code();

		String html = getEmailTemplate(
				"views.html.account.email.verify_email", langCode, url, token,
				user.getFirstName(), user.getEmail());
		String text = getEmailTemplate(
				"views.txt.account.email.verify_email", langCode, url, token,
				user.getFirstName(), user.getEmail());

		Logger.info("Sending this body = " + html + " text = " + text);

		return new Body(text, html);
	}

	public void sendVerifyEmailMailingAfterSignup(User user, Context ctx) {

		String subject = getVerifyEmailMailingSubjectAfterSignup(user, ctx);
		String token = generateVerificationRecord(user);
		Body body = getVerifyEmailMailingBodyAfterSignup(token, user, ctx);
		//TODO use AWS SES service here.
		sendMail(subject, body, getEmailName(user));
		AmazonSESService.sendAdminEmail(config.getString("aws.ses.username"),
				config.getString("aws.ses.password"),
				subject,
				body.getHtml(),
				getEmailName(user));
	}

    public void generateTokenForPinCodeActivation(User user) {
        final String token = generateVerificationRecord(user);

        Logger.info(format("successfully generated new token for user [%s]", user.getId()));
    }

	private String getEmailName(User user) {
		return getEmailName(user.getEmail(), user.getFirstName());
	}
}
