package com.diemlife.controller;

import com.diemlife.action.BasicAuth;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.stripe.model.Charge;
import com.typesafe.config.Config;
import org.apache.commons.lang3.tuple.ImmutablePair;
import com.diemlife.constants.AccountStatus;
import com.diemlife.constants.BankAccountPurpose;
import com.diemlife.constants.HappeningsListType;
import com.diemlife.constants.Interests;
import com.diemlife.constants.PaymentMode;
import com.diemlife.constants.QuestEdgeType; 
import com.diemlife.constants.QuestMode;
import com.diemlife.dao.BrandConfigDAO;
import com.diemlife.dao.HappeningDAO;
import com.diemlife.dao.HappeningParticipantDAO;
import com.diemlife.dao.PaymentTransactionDAO;
import com.diemlife.dao.QuestActivityHome;
import com.diemlife.dao.QuestBackingDAO;
import com.diemlife.dao.QuestCommentsDAO;
import com.diemlife.dao.QuestEdgeDAO;
import com.diemlife.dao.QuestTeamDAO;
import com.diemlife.dao.QuestUserFlagDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.StripeCustomerDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dao.UserRelationshipDAO;
import com.diemlife.dto.*;
import exceptions.PaymentModeAlreadyExistsException;
import exceptions.RequiredParameterMissingException;
import exceptions.StripeApiCallException;
import exceptions.StripeInvalidPostalCodeException;
import forms.AddressForm;
import forms.BackerInfoForm;
import forms.BankAccountVerificationForm;
import forms.CountryZipForm;
import forms.CouponForm;
import forms.ExternalAccountForm;
import forms.FundraisingForm;
import forms.OrderBillingForm;
import forms.OrderBreakdownForm;
import forms.OrderForm;
import forms.OrderItemForm;
import forms.OrderParticipantForm;
import forms.ParticipantInfoForm;
import forms.PayoutForm;
import forms.PersonalInfoForm;
import forms.PlaidTokenInfo;
import forms.ProfileForm;
import forms.QuestBackingBreakdownForm;
import forms.QuestBackingForm;
import forms.QuestTeamInfoForm;
import forms.RegistrationForm;
import forms.TransactionListFilterForm;
import com.diemlife.models.Address;
import com.diemlife.models.BrandConfig;
import com.diemlife.models.EmergencyContact;
import com.diemlife.models.FundraisingLink;
import com.diemlife.models.FundraisingSupplement;
import com.diemlife.models.FundraisingTransaction;
import com.diemlife.models.Happening;
import com.diemlife.models.HappeningParticipant;
import com.diemlife.models.LeaderboardMember;
import com.diemlife.models.LinkedAccount;
import com.diemlife.models.PaymentTransaction;
import com.diemlife.models.PayoutTransaction;
import com.diemlife.models.PersonalInfo;
import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestBacking;
import com.diemlife.models.QuestBackingTransaction;
import com.diemlife.models.QuestBrandConfig.QuestBrandConfigId;
import com.diemlife.models.QuestEdge;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTeam;
import com.diemlife.models.QuestUserFlag;
import com.diemlife.models.Quests;
import com.diemlife.models.RecurringQuestBackingTransaction;
import com.diemlife.models.ReferredQuest;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.StripeCustomer;
import com.diemlife.models.StripeEntity;
import com.diemlife.models.TicketPurchaseTransaction;
import com.diemlife.models.User;
import com.diemlife.models.UserActivationPinCode;
import com.diemlife.models.UserProfile;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.LocalDateTime;
import play.Logger;
import play.cache.Cached;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import com.diemlife.providers.MyUsernamePasswordAuthProvider;
import com.diemlife.providers.MyUsernamePasswordAuthUser;
import com.diemlife.security.JwtSessionLogin;
import com.diemlife.security.UsernamePasswordAuth;
import com.diemlife.services.AmazonSESService;
import com.diemlife.services.FormSecurityService;
import com.diemlife.services.FundraisingService;
import com.diemlife.services.LeaderboardService;
import com.diemlife.services.NotificationService;
import com.diemlife.services.OutgoingEmailService;
import com.diemlife.services.PaymentTransactionFacade;
import com.diemlife.services.PlaidLinkService;
import com.diemlife.services.QuestService;
import com.diemlife.services.RecaptchaService;
import com.diemlife.services.SeoService;
import com.diemlife.services.StripeAccountCreator;
import com.diemlife.services.StripeAccountUpdater;
import com.diemlife.services.StripeConnectService;
import com.diemlife.services.StripeConnectService.ExportedBankAccountData;
import com.diemlife.services.StripeConnectService.ExportedCreditCardData;
import com.diemlife.services.StripeConnectService.ExportedPayout;
import com.diemlife.services.StripeConnectService.ExportedProduct;
import com.diemlife.services.StripeConnectService.ExportedProductVariant;
import com.diemlife.services.StripeConnectService.PaymentMethod;
import com.diemlife.services.StripeConnectService.TicketsPurchaseOrder;
import com.diemlife.services.UserActivationService;
import com.diemlife.services.UserProvider;
import com.diemlife.services.UserService;
import com.diemlife.utils.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.feth.play.module.pa.controllers.AuthenticateBase.noCache;
import static com.diemlife.constants.NotificationType.FUNDRAISER_STARTED;
import static com.diemlife.constants.QuestActivityStatus.COMPLETE;
import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.SUPPORT_ONLY;
import static com.diemlife.constants.QuestMode.TEAM;
import static com.diemlife.dao.FundraisingSupplementDAO.getFundraisingSupplement;
import static com.diemlife.dao.QuestTasksDAO.getQuestCompletionPercentage;
import static java.lang.Boolean.FALSE;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static com.diemlife.services.UserService.getByEmail;
import static com.diemlife.services.UserService.isFreeEmail;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;

@JwtSessionLogin
public class Api extends Controller implements StripeAccountCreator, StripeAccountUpdater {

    public static final String USER_ROLE = "user";
    private static final String FAILED_STRIPE_STATUS = "failed";

    private final UserProvider userProvider;
    private final MyUsernamePasswordAuthProvider userPaswAuthProvider;
    private final StripeConnectService stripeConnectService;
    private final PlaidLinkService plaidLinkService;
    private final PDFTemplateStamper pdfTemplateStamper;
    private final OutgoingEmailService emailService;
    private final UserActivationService userActivationService;
    private final FundraisingService fundraisingService;
    private final NotificationService notificationService;
    private final LeaderboardService leaderboardService;
    private final QuestService questService;
    private final PaymentTransactionFacade paymentTransactionFacade;
    private final Config configuration;

    private final MessagesApi msg;
    private final DAOProvider daoProvider;

    private final JPAApi jpaApi;
    private final FormFactory formFactory;
    private final FormSecurityService formSecurityService;
    private final RecaptchaService recaptchaService;
    private final SeoService seoService;

    private Database db;
    private Database dbRo;

    @Inject
    public Api(
            Database db,
            @NamedDatabase("ro") Database dbRo,
            final UserProvider userProvider,
            final MyUsernamePasswordAuthProvider userPaswAuthProvider,
            final StripeConnectService stripeConnectService,
            final PlaidLinkService plaidLinkService,
            final PDFTemplateStamper pdfTemplateStamper,
            final OutgoingEmailService emailService,
            final UserActivationService userActivationService,
            final FundraisingService fundraisingService,
            final NotificationService notificationService,
            final LeaderboardService leaderboardService,
            final QuestService questService,
            final PaymentTransactionFacade paymentTransactionFacade,
            final Config configuration,
            final MessagesApi msg,
            final DAOProvider daoProvider,
            final JPAApi api,
            final FormFactory factory,
            final FormSecurityService formSecurityService,
            final RecaptchaService recaptchaService,
            final SeoService seoService) {

        this.db = db;
        this.dbRo = dbRo;
        this.userProvider = userProvider;
        this.userPaswAuthProvider = userPaswAuthProvider;
        this.stripeConnectService = stripeConnectService;
        this.plaidLinkService = plaidLinkService;
        this.pdfTemplateStamper = pdfTemplateStamper;
        this.emailService = emailService;
        this.userActivationService = userActivationService;
        this.fundraisingService = fundraisingService;
        this.notificationService = notificationService;
        this.leaderboardService = leaderboardService;
        this.questService = questService;
        this.paymentTransactionFacade = paymentTransactionFacade;
        this.configuration = configuration;

        this.msg = msg;
        this.daoProvider = daoProvider;

        this.jpaApi = api;
        this.formFactory = factory;
        this.formSecurityService = formSecurityService;
        this.recaptchaService = recaptchaService;
        this.seoService = seoService;
    }

    @Override
    public StripeConnectService stripeConnectService() {
        return stripeConnectService;
    }

    @Override
    public JPAApi jpaApi() {
        return jpaApi;
    }

    public CompletionStage<Result> verifyRecaptcha(final String response) {
        return recaptchaService.verifyRecaptcha(response);
    }

    @Transactional
    @JwtSessionLogin
    public Result forgotPassword() {
        EntityManager em = this.jpaApi.em();

        DynamicForm form = formFactory.form().bindFromRequest();
        String email = form.get("email");

        if (email == null) {
            return badRequest("Missing parameter [email]");
        }

        User user = UserHome.findByEmail(email, em);

        if (user == null) {
            ObjectNode result = Json.newObject();
            result.put("msg", "Account not found");
            return status(404, result);
        }

        // yep, we have a user with this email that is active - we do
        // not know if the user owning that account has requested this
        // reset, though.
        MyUsernamePasswordAuthProvider provider = this.userPaswAuthProvider;
        // User exists
        if (!user.getEmailValidated()) {
//            provider.sendPasswordResetMailing(user, ctx());
            // In case you actually want to let (the unknown person)
            // know whether a user was found/an email was sent, use,
            // change the flash message
//        } else {
            // We need to change the message here, otherwise the user
            // does not understand whats going on - we should not verify
            // with the password reset, as a "bad" user could then sign
            // up with a fake email via OAuth and get it verified by an
            // a unsuspecting user that clicks the link.
            //flash(Application.FLASH_MESSAGE_KEY,Messages.get("playauthenticate.reset_password.message.email_not_verified"));

            // You might want to re-send the verification email here...
            // provider.sendVerifyEmailMailingAfterSignup(user, ctx());

            // validate user here
            UserHome userDao = new UserHome();
            final User verified = userDao.verify(user, em);
            // create stripe account if does not exists
            if (verified.getStripeEntity() instanceof StripeCustomer) {
                Logger.info(format("Stripe customer already exists for user '%s' having customer ID = %s", verified.getEmail(), ((StripeCustomer) verified.getStripeEntity()).stripeCustomerId));
            } else {
                try {
                    final StripeCustomer customer = createStripeCustomer(verified);
                    if (customer != null) {
                        Logger.info(format("Created StripeCustomer for User '%s' having customer ID = %s", verified.getEmail(), customer.stripeCustomerId));
                    }
                } catch (final StripeApiCallException e) {
                    Logger.error(format("Unable to create Stripe account entities for user %s", verified.getEmail()), e);
                }
            }

        }

        provider.sendPasswordResetMailing(user, ctx());

        ObjectNode result = Json.newObject();
        result.put("msg", "Please check your email to reset your password.");

        return ok(result);
    }

    @BasicAuth
    @Transactional
    public Result getProfile() throws JsonProcessingException {
        User userAPI = (User) ctx().args.get("api-user");
        userAPI = this.jpaApi.em().find(models.User.class, userAPI.getId());

        UserProfile profileDB = userAPI.getUserProfile();

        ObjectMapper mapper = new ObjectMapper();

        ProfileForm profile = new ProfileForm();
        //profile.setSalutation(profileDB.getSalutation());
        profile.setAddress1(profileDB.getAddress1());
        profile.setAddress2(profileDB.getAddress2());
        profile.setCity(profileDB.getCity());
        profile.setCounty(profileDB.getCounty());
        profile.setDob(profileDB.getDob());
        profile.setGender(profileDB.getGender());
        profile.setHomeNumber(profileDB.getHomeNumber());
        profile.setMobileNumber(profileDB.getMobileNumber());
        profile.setPostcode(profileDB.getPostcode());

        String jsonString = mapper.writeValueAsString(profile);

        JsonNode result = Json.parse(jsonString);

        return ok(result);
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result editProfile() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        DynamicForm form = formFactory.form().bindFromRequest();
        String firstName = form.get("firstName");
        String lastName = form.get("lastName");
        String name = form.get("name");
        String missionStatement = form.get("missionStatement");
        String email = form.get("email");
        String country = form.get("country");
        String zip = form.get("zip");
        String receiveEmail = form.get("receiveEmail");
        String username = form.get("username");

        boolean dirty = false;
        if (user != null) {
            if (firstName != null && !firstName.isEmpty()) {
                dirty |= !equalsIgnoreCase(trim(firstName), user.getFirstName());
                user.setFirstName(trim(firstName));
            }
            if (lastName != null && !lastName.isEmpty()) {
                dirty |= !equalsIgnoreCase(trim(lastName), user.getLastName());
                user.setLastName(trim(lastName));
            }
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            } else if (firstName != null && lastName != null) {
                user.setName(firstName + " " + lastName);
            }
            if (missionStatement != null && !missionStatement.isEmpty()) {
                user.setMissionStatement(missionStatement);
            }

            final boolean freeEmail = isFreeEmail(email, em);
            final boolean sameEmail = equalsIgnoreCase(trim(email), user.getEmail());
            if (!freeEmail && !sameEmail) {
                return forbidden();
            }
            if (email != null && !email.isEmpty()) {
                dirty |= !equalsIgnoreCase(trim(email), user.getEmail());
                user.setEmail(trim(email));
            }

            if (zip != null && !zip.isEmpty()) {
                dirty |= !equalsIgnoreCase(trim(zip), user.getZip());
                user.setZip(trim(zip));
            }
            if (isNotBlank(country)) {
                dirty |= !equalsIgnoreCase(trim(country), user.getCountry());
                user.setCountry(trim(country));
            }

            if (receiveEmail != null && !receiveEmail.isEmpty()) {
                user.setReceiveEmail(receiveEmail);
            }
            if (username != null && !username.isEmpty()) {
                String usernameStripped = username.replaceAll("[^A-Za-z0-9]", "");
                //need to ensure after string is modified that no account with that still exists
                if (UserService.doesUsernameExist(usernameStripped, em)) {
                    return forbidden().sendJson(Json.toJson("Username is already in use"));
                }
                if (usernameStripped.length() > 22) {
                    return forbidden().sendJson(Json.toJson("Username should be less than 22 characters"));
                }
                user.setUserName(usernameStripped);
            }

            user.setUpdatedOn(LocalDateTime.now().toDate());

            em.merge(user);

            //populateInterests(user, form, true);

            if (dirty) {
                final StripeEntity stripeEntity = userProvider.getStripeCustomerByUserId(user.getId(), StripeEntity.class);
                if (stripeEntity instanceof StripeCustomer) {
                    updateStripeCustomerEntity(user, (StripeCustomer) stripeEntity);
                }
                if (stripeEntity instanceof StripeAccount) {
                    updateStripeAccountEntity(user, (StripeAccount) stripeEntity, null);
                }
            }

            this.seoService.capturePageBackground(URLUtils.seoFriendlyUserProfilePath(user));
            this.seoService.capturePageBackground(URLUtils.seoFriendlierUserProfilePath(user));

            return ok();

        } else {
            Logger.error("Api :: editProfile : error saving user updates to profile");
            return ok(ResponseUtility.getErrorMessageForResponse("error updating user profile"));
        }

    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result updateConnectedAccountPersonalInfo() {
        User user = this.userProvider.getUser(session());
        DynamicForm form = formFactory.form().bindFromRequest();
        final String address = form.get("address");
        final String dob = form.get("dob");
        final String last4 = form.get("last4");
        final String personalId = form.get("personalId");
        final String phone = form.get("phone");
        final String url = form.get("url");

        final PersonalInfoDTO personalInfo = new PersonalInfoDTO(address, last4, dob, personalId, phone, url);

        if (user != null) {
            final StripeEntity stripeEntity = userProvider.getStripeCustomerByUserId(user.getId(), StripeEntity.class);
            if (stripeEntity instanceof StripeCustomer) {
                updateStripeCustomerEntity(user, (StripeCustomer) stripeEntity);
            }
            if (stripeEntity instanceof StripeAccount) {
                updateStripeAccountEntity(user, (StripeAccount) stripeEntity, personalInfo);
            }
            return ok();
        } else {
            Logger.error("tried to update the account information for an invalid user");
            return unauthorized();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result changePassword() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        DynamicForm form = formFactory.form().bindFromRequest();
        String password = form.get("password");

        if (password != null && !password.isEmpty()) {
            UserHome userDao = new UserHome();

            userDao.changePassword(user, new MyUsernamePasswordAuthUser(password), em);
            flash(Application.FLASH_MESSAGE_KEY,
                    this.msg.preferred(request()).at("playauthenticate.change_password.success"));
        }

        return ok();
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Transactional
    @JwtSessionLogin
    public Result register() {

        Form<RegistrationForm> form = this.formFactory.form(RegistrationForm.class);

        form = form.bindFromRequest();

        if (form.hasErrors()) {
            return status(406, form.errorsAsJson());
        } else {
            RegistrationForm input = form.get();

            MyUsernamePasswordAuthUser authUser = new MyUsernamePasswordAuthUser(input);

            User u = findAuhtUserByEmail(authUser);

            if (u != null) {
            	LinkedAccount linkedAccount = u.getLinkedAccounts().stream().findFirst().get();

        		if("password".equalsIgnoreCase(linkedAccount.getProviderKey())) {
	                if (u.getEmailValidated()) {
	                    // This user exists, has its email validated and is active
	                    ObjectNode result = Json.newObject();
	                    result.put("msg", "This user exists already, has its email validated and is active");
	                    result.put("reason", AccountStatus.VERIFIED.name());
	
	                    return status(406, result);
	                } else {
	                    // this user exists, is active but has not yet validated its email
	                    ObjectNode result = Json.newObject();
	                    result.put("msg", "This user exists already, is active but has not yet validated its email");
	                    result.put("reason", AccountStatus.UNVERIFIED.name());
	
	                    return status(406, result);
	                }
        	 }
            }
            u = findAuhtUserByEmail(authUser);
            if(u != null) {
            	authUser.setUserId(u.getId());
            }		
            
            User newUser = createUserByRole(authUser, null);

            addNonAuthInformationToUser(newUser, input);

            addInterestsToUser(newUser);

            this.userActivationService.populateStartQuest(newUser);

            if (isNotTrue(input.getWithPin())) {
                sendVerificationEmailAfterSignup(newUser);
            } else {
                this.userPaswAuthProvider.generateTokenForPinCodeActivation(newUser);
            }

            this.seoService.capturePageBackground(URLUtils.seoFriendlyUserProfilePath(newUser));
            this.seoService.capturePageBackground(URLUtils.seoFriendlierUserProfilePath(newUser));

            ObjectNode result = Json.newObject();
            result.put("msg", "Please check your email to complete registration");
            result.put("userId", newUser.getId());
            return ok(result);

        }
    }

    private void populateInterests(final User user, final DynamicForm form, final boolean update) {
        Arrays.stream(Interests.values()).forEach(interest -> {
            Logger.error(format("Interest and value: %s is %s", interest.getLabel(), interest.getInput()));
            if (equalsIgnoreCase(form.get(interest.getInput()), "true")) {
                if (update) {
                    updateUserFavorites(user, interest.getValue().toUpperCase());
                } else {
                    addUserFavorites(user, interest.getValue().toUpperCase());
                }
            } else {
                if (update) {
                    removeUserFavorites(user, interest.getValue().toUpperCase());
                }
            }
        });
    }

    public void addInterestsToUser(final User user) {
        Arrays.stream(Interests.values()).forEach(interest -> {
            addUserFavorites(user, interest.getValue().toUpperCase());
        });
    }

    private void sendVerificationEmailAfterSignup(User newUser) {
        MyUsernamePasswordAuthProvider provider = this.userPaswAuthProvider;

        provider.sendVerifyEmailMailingAfterSignup(newUser, ctx());
    }

    private User findByAuthUserId(MyUsernamePasswordAuthUser authUser) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        User u = userDao.findByAuthUserIdentity(authUser, em);

        return u;
    }
  private User findAuhtUserByEmail(MyUsernamePasswordAuthUser authUser) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        User u = userDao.findByEmail(authUser.getEmail(), em);

        return u;
    }

    private User createUserByRole(MyUsernamePasswordAuthUser authUser, String tiUserId) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        User newUser = userDao.createByRole(authUser, controllers.Api.USER_ROLE, null, em);

        return newUser;

    }

    private void addNonAuthInformationToUser(User newUser, RegistrationForm input) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        userDao.addNonAuthInfoToUser(newUser, input, em);
    }

    private void addUserFavorites(User newUser, String favorite) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        if (newUser != null && favorite != null) {
            userDao.addUserFavoritesToUser(newUser, favorite, em);
        }
    }

    private void updateUserFavorites(User user, String favorite) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        if (user != null && favorite != null) {
            userDao.updateUserFavorites(user, favorite, em);
        }
    }

    private void removeUserFavorites(User user, String favorite) {
        EntityManager em = this.jpaApi.em();
        UserHome userDao = new UserHome();

        if (user != null && favorite != null) {
            userDao.removeUserFavoritesForUser(user, favorite, em);
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listCreditCards() {
        return handleStripeCustomerRequest(customer -> ok(Json.newArray().addAll(stripeConnectService.exportCustomerCreditCards(customer)
                .stream()
                .map(Json::toJson)
                .collect(toList()))), userProvider, StripeCustomer.class);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result saveCreditCard() {
        final Form<ExternalAccountForm> form = formFactory.form(ExternalAccountForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }

        return handleStripeCustomerRequest(customer -> {
            stripeConnectService.saveNewCreditCard(customer, form.get().getToken());

            // send email for added credit card
            AmazonSESService.sendAddCreditCardConfirmation(
                    request(),
                    configuration.getString("aws.ses.username"),
                    configuration.getString("aws.ses.password"),
                    customer.user
            );

            return ok();
        }, userProvider, StripeCustomer.class);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result deleteCreditCard(final String lastFour) {
        return handleStripeCustomerRequest(customer -> {
            stripeConnectService.deleteExistingCreditCard(customer, lastFour);

            // send email for removed credit card
            AmazonSESService.sendDeleteCreditCardConfirmation(
                    request(),
                    configuration.getString("aws.ses.username"),
                    configuration.getString("aws.ses.password"),
                    customer.user
            );

            return ok();
        }, userProvider, StripeCustomer.class);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result availableBalance() {
        final String currency = configuration.getString("application.currency");
        return handleStripeCustomerRequest(customer ->
                ok(Json.toJson(stripeConnectService.retrieveAvailableBalance(customer, currency))), userProvider, StripeAccount.class);
    }

    @Transactional
    public Result testStripeAccount(final String userId) {
        if (isBlank(userId) || !isNumeric(userId)) {
            return badRequest("User ID must be a number");
        }
        if (hasStripeAccountEntity(Integer.valueOf(userId))) {
            return ok();
        } else {
            return notFound();
        }
    }

    @Transient
    @Cached(key = "SupportedCountriesCodes")
    public Result countiesConfig() {
        final Map<String, Collection<String>> countriesConfig = new HashMap<>();
        countriesConfig.put("stripeSupported", stripeConnectService.retrieveSupportedCountriesCodes());
        countriesConfig.put("paymentsSupported", configuration.getStringList("application.countries.paymentsSupported"));
        countriesConfig.put("payoutsSupported", configuration.getStringList("application.countries.payoutsSupported"));
        return ok(Json.toJson(countriesConfig));
    }

    @Cached(key = "ProfileInterests.Enum")
    public Result profileInterests() {
        final ArrayNode result = Json.newArray();
        Arrays.stream(Interests.values()).forEach(interest -> {
            final ObjectNode node = Json.newObject();
            node.put("value", interest.getValue());
            node.put("label", interest.getLabel());
            node.put("input", interest.getInput());
            result.add(node);
        });
        return ok(result);
    }

    private boolean hasStripeAccountEntity(final Integer userId) {
        final StripeEntity stripeEntity = userProvider.getStripeCustomerByUserId(userId, StripeEntity.class);
        return stripeEntity instanceof StripeAccount;
    }

    @Transactional
    public Result product(final String questId) {
        return handleHappeningRequest(event -> {
            final StripeAccount merchant = userProvider.getStripeCustomerByUserId(event.quest.getUser().getId(), StripeAccount.class);
            if (merchant == null) {
                return notFound();
            }
            final ExportedProduct product = stripeConnectService.retrieveProduct(merchant, event.stripeProductId);
            if (product == null || !product.active) {
                return ok(NullNode.getInstance());
            } else {
                product.registrationTemplate = event.registrationTemplate;
                product.showDiscounts = event.showDiscounts;
                product.eventDate = event.happeningDate;
                return ok(Json.toJson(product));
            }
        }, questId, jpaApi);
    }

    @Transactional
    public Result event(final String questId, final boolean withStripeInfo) {
        if (withStripeInfo) {
            return handleHappeningRequest(event -> {
                final StripeAccount merchant = userProvider.getStripeCustomerByUserId(event.quest.getUser().getId(), StripeAccount.class);
                if (merchant == null) {
                    return notFound();
                }
                return ok(Json.toJson(new HappeningDTO(event).withStripeInfo(merchant, stripeConnectService)));
            }, questId, jpaApi);
        } else {
            return handleHappeningRequest(event -> ok(Json.toJson(new HappeningDTO(event))), questId, jpaApi);
        }
    }

    @Transactional
    public Result coupon(final String questId) {
        final Form<CouponForm> form = formFactory.form(CouponForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        return handleHappeningRequest(event -> {
            final CouponForm couponForm = form.get();
            final StripeAccount merchant = userProvider.getStripeCustomerByUserId(event.quest.getUser().getId(), StripeAccount.class);
            if (merchant == null) {
                return forbidden();
            }
            try {
                return ok(Json.toJson(stripeConnectService.exportCoupon(merchant, couponForm.getCouponCode())));
            } catch (final StripeApiCallException e) {
                Logger.error(format("Cannot retrieve coupon with name '%s' for Quest %s", couponForm.getCouponCode(), questId), e);

                return badRequest();
            }
        }, questId, jpaApi);
    }

    @Transactional
    public Result breakdown(final String questId) {
        final Form<OrderBreakdownForm> form = formFactory.form(OrderBreakdownForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        return handleHappeningRequest(event -> {
            final OrderBreakdownForm breakdownForm = form.get();
            final StripeAccount merchant = userProvider.getStripeCustomerByUserId(event.quest.getUser().getId(), StripeAccount.class);
            if (merchant == null) {
                return badRequest();
            }

            return ok(Json.toJson(stripeConnectService.transactionBreakdown(
                    merchant,
                    stream(breakdownForm.getOrderItems())
                            .collect(toMap(OrderItemForm::getSkuId, OrderItemForm::getQuantity)),
                    isBlank(breakdownForm.getPaymentMode())
                            ? null
                            : PaymentMode.valueOf(capitalize(breakdownForm.getPaymentMode())),
                    breakdownForm.getCouponCode()
            )));
        }, questId, jpaApi);
    }

    @Transactional
    public Result breakdownQuestBacking(final String questId, final String doerId) {
        if (isBlank(questId) || !isNumeric(questId)) {
            return badRequest("Quest ID must be a number");
        }
        if (isBlank(doerId) || !isNumeric(doerId)) {
            return badRequest("Doer ID must be a number");
        }
        final Form<QuestBackingBreakdownForm> form = formFactory.form(QuestBackingBreakdownForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final QuestBackingBreakdownForm breakdownForm = form.get();
        final EntityManager entityManager = jpaApi.em();
        final Quests quest = QuestsDAO.findById(Integer.valueOf(questId), entityManager);
        if (quest == null) {
            return notFound("Quest not found for ID " + questId);
        }
        final User doerUser = UserHome.findById(Integer.valueOf(doerId), entityManager);
        if (doerUser == null) {
            return notFound("Doer user not found for ID " + doerId);
        }
        final Integer beneficiaryUserId = getBeneficiaryUserId(quest, doerUser, breakdownForm.getBrandUserId());
        final StripeAccount seller = userProvider.getStripeCustomerByUserId(beneficiaryUserId, StripeAccount.class);
        if (seller == null) {
            return notFound("Beneficiary user not found for ID " + beneficiaryUserId);
        }
        final boolean absorbFeesByBuyer = isNotFalse(breakdownForm.getBackerAbsorbsFees()) && !seller.user.isAbsorbFees();
        return ok(Json.toJson(stripeConnectService.transactionBreakdown(
                breakdownForm.getAmount(),
                PaymentMode.valueOf(capitalize(breakdownForm.getPaymentMode())),
                seller.user.isAbsorbFees(),
                absorbFeesByBuyer,
                seller.user,
                breakdownForm.getTip()
        )));
    }

    @Transactional
    public Result waiver(final String questId) {
        return handleHappeningRequest(event -> ok(Json.toJson(event.waiver)), questId, jpaApi);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listHappeningsUpcoming() {
        return listHappenings(HappeningsListType.UPCOMING);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listHappeningsRecommended() {
        return listHappenings(HappeningsListType.RECOMMENDED);
    }

    private Result listHappenings(final HappeningsListType type) {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final ArrayNode result = Json.newArray();
        jpaApi.withTransaction(em -> {
            final HappeningDAO happeningDAO = new HappeningDAO(em);
            final PaymentTransactionDAO transactionDAO = new PaymentTransactionDAO(em);
            final List<Happening> happenings;
            switch (type) {
                case UPCOMING:
                    happenings = happeningDAO.getUpcomingHappenings(user.getId());
                    break;
                case RECOMMENDED:
                    happenings = happeningDAO.getRecommendedHappenings(user.getId());
                    break;
                default:
                    happenings = emptyList();
                    break;
            }
            happenings.forEach(happening -> {
                em.detach(happening);
                final JsonNode node = Json.toJson(happening);
                if (node instanceof ObjectNode && happening.quest != null && happening.quest.getId() != null) {
                    ((ObjectNode) node).put(
                            "timesEventBacked",
                            transactionDAO.getHappeningTransactionsCountForQuest(happening.quest.getId())
                    );
                }
                result.add(node);
            });
            return result;
        });
        return ok(result);
    }

    @JwtSessionLogin
    @Transactional
    public Result orderTicketsGuest(final @NotNull String questId) {
        final Form<OrderForm> form = formFactory.form(OrderForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        long _questId = Long.parseLong(questId);
        final EntityManager em = jpaApi.em();

        // TODO: questId is a long in the db
        final Happening event = new HappeningDAO(em).getHappeningByQuestId((int) _questId);
        if (event == null) {
            return notFound("No event found associated to quest with ID " + questId);
        }
        final Integer merchantUserId = event.quest.getCreatedBy();
        final StripeAccount merchant = userProvider.getStripeCustomerByUserId(merchantUserId, StripeAccount.class);
        if (merchant == null) {
            return badRequest("Stripe account doesn't exist for merchant with user ID  " + merchantUserId);
        }
        final OrderForm orderForm = form.get();
        final OrderBillingForm billingForm = orderForm.getBillingInfo();
        final String billingEmail = billingForm.getPersonalInfo().getEmail();
        final StripeShippingDTO billingInfo = buildStripeShippingInfo(orderForm);
        final TicketsPurchaseOrder ticketsPurchaseOrder = buildTicketsPurchaseOrder(orderForm, event, merchant);

        final String chargeId = stripeConnectService.payOrderWithPaymentSource(
                merchant,
                null,
                billingEmail,
                billingInfo,
                ticketsPurchaseOrder,
                _questId,
                event.getId()
        );

        // TODO: the code to save the stripe order info to DynamoDB should be here.  Didn't want to refactor the Stripe service just yet.

        final String defaultBuyerId = configuration.getString("application.transactions.defaultBuyer");
        final StripeCustomer defaultBuyer = new StripeCustomerDAO(em).loadStripeCustomer(defaultBuyerId);
        final TicketPurchaseTransaction transaction = new TicketPurchaseTransaction();
        transaction.event = event;
        transaction.from = defaultBuyer;
        transaction.to = merchant;
        transaction.valid = true;
        transaction.stripeTransactionId = chargeId;
        transaction.couponUsed = ticketsPurchaseOrder.getCouponUsed();
        final PaymentTransaction savedTransaction = new PaymentTransactionDAO(em).save(transaction, TicketPurchaseTransaction.class);

        final OrderParticipantForm[] participants = saveOrderParticipants(event, orderForm, savedTransaction, em);

        Logger.info(format("Successfully created anonymous ticket purchase transaction with ID %s", savedTransaction.id));

        final TransactionBreakdown breakdown = stripeConnectService.transactionBreakdown(ticketsPurchaseOrder);

        final String billingFirstName = billingForm.getPersonalInfo().getFirstName();
        final String billingLastName = billingForm.getPersonalInfo().getLastName();
        emailService.sendOrderTicketConfirmationEmail(
                request(),
                event.quest,
                new EmailPersonalDTO(billingFirstName, billingLastName, billingEmail),
                savedTransaction,
                breakdown,
                ticketsPurchaseOrder,
                billingInfo);

        if (isTrue(orderForm.getSignUp())) {
            registerUserAfterPayment(billingForm.getPersonalInfo(), billingForm.getAddress());
        }

        final User user = userProvider.getUser(session());
        if (user != null) {
            startQuestAfterTicketPurchase(event, participants, user);
        }

        return ok(TextNode.valueOf(savedTransaction.id.toString()));
    }

    private void registerUserAfterPayment(final PersonalInfoForm personalInfo, final CountryZipForm address) {
        final String billingEmail = personalInfo.getEmail();
        final String billingFirstName = personalInfo.getFirstName();
        final String billingLastName = personalInfo.getLastName();
        final RegistrationForm registrationForm = new RegistrationForm(billingFirstName, billingLastName, billingEmail);
        registrationForm.setPassword1(EMPTY);
        registrationForm.setPassword2(EMPTY);
        registrationForm.setCountry(address.getCountry());
        registrationForm.setZip(address.getZip());
        registrationForm.setReceiveEmail(FALSE.toString());
        registrationForm.setWithPin(false);

        final MyUsernamePasswordAuthUser authUser = new MyUsernamePasswordAuthUser(registrationForm);
        final User existingUser = findByAuthUserId(authUser);

        if (existingUser == null) {
            final User newUser = createUserByRole(authUser, null);

            addNonAuthInformationToUser(newUser, registrationForm);
            addInterestsToUser(newUser);
            this.userActivationService.populateStartQuest(newUser);
            sendVerificationEmailAfterSignup(newUser);

            Logger.info(format("Registered new user '%s' after payment", newUser.getEmail()));
        } else {
            Logger.warn(format("User '%s' already exists, skipping registration", existingUser.getEmail()));
        }
    }

    private StripeShippingDTO buildStripeShippingInfo(final OrderForm orderForm) {
        final OrderBillingForm billingForm = orderForm.getBillingInfo();
        final StripeShippingDTO billingInfo = new StripeShippingDTO();
        billingInfo.name = billingForm.getPersonalInfo().getFirstName() + " " + billingForm.getPersonalInfo().getLastName();
        billingInfo.address = new StripeAddressDTO();
        billingInfo.address.line1 = trimToEmpty(billingForm.getAddress().getStreetNo());
        if (isNotBlank(billingForm.getAddress().getStreetNoAdditional())) {
            billingInfo.address.line2 = trimToNull(billingForm.getAddress().getStreetNoAdditional());
        }
        billingInfo.address.city = trimToEmpty(billingForm.getAddress().getCity());
        billingInfo.address.state = trimToEmpty(billingForm.getAddress().getState());
        billingInfo.address.country = trimToEmpty(billingForm.getAddress().getCountry());
        billingInfo.address.postalCode = trimToEmpty(billingForm.getAddress().getZip());

        return billingInfo;
    }

    private Map<String, Integer> buildOrderItems(final OrderForm orderForm) {
        return stream(orderForm.getOrderItems()).collect(toMap(OrderItemForm::getSkuId, OrderItemForm::getQuantity));
    }

    private TicketsPurchaseOrder buildTicketsPurchaseOrder(final OrderForm orderForm, final Happening event, final StripeAccount merchant) {
        final OrderBillingForm billingForm = orderForm.getBillingInfo();
        final String couponCode = upperCase(orderForm.getCouponCode());
        final Map<String, Integer> orderItems = buildOrderItems(orderForm);
        final PaymentMode paymentMode = PaymentMode.valueOf(capitalize(billingForm.getPaymentMode()));
        final PaymentMethod paymentMethod = new PaymentMethod(paymentMode, billingForm.getLastFour(), billingForm.getToken(), billingForm.isSave());
        final String currency = configuration.getString("application.currency");

        final TicketsPurchaseOrder ticketsPurchaseOrder = new TicketsPurchaseOrder();
        ticketsPurchaseOrder.happening = event;
        ticketsPurchaseOrder.currency = currency;
        ticketsPurchaseOrder.paymentMethod = paymentMethod;
        ticketsPurchaseOrder.product = stripeConnectService.retrieveProduct(merchant, event.stripeProductId);
        ticketsPurchaseOrder.coupon = isBlank(couponCode)
                ? null
                : stripeConnectService.exportCoupon(merchant, couponCode);
        ticketsPurchaseOrder.orderItems = stream(ticketsPurchaseOrder.product.variants)
                .peek(variant -> {
                    variant.attributes = variant.attributes.entrySet().stream()
                            .filter(attribute -> !"order".equalsIgnoreCase(attribute.getKey()))
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
                })
                .collect(toMap(variant -> variant.id, variant -> Pair.of(variant, Optional.ofNullable(orderItems.get(variant.id)).orElse(0))));
        return ticketsPurchaseOrder;
    }

    private OrderParticipantForm[] saveOrderParticipants(final Happening event,
                                                         final OrderForm orderForm,
                                                         final PaymentTransaction savedTransaction,
                                                         final EntityManager em) {
        final OrderParticipantForm[] participantForms = orderForm.getParticipants();
        for (final OrderParticipantForm participantForm : participantForms) {
            final HappeningParticipant participant = fromParticipantForm(participantForm);
            participant.event = event;
            participant.order = savedTransaction;
            participant.registrationDate = Date.from(Instant.now());
            if (participant.address == null) {
                participant.address = fromBillingAddressForm(orderForm.getBillingInfo().getAddress());
            }

            final HappeningParticipant savedParticipant = new HappeningParticipantDAO(em)
                    .save(participant, HappeningParticipant.class);

            Logger.info(format("Successfully saved event participant with ID %s", savedParticipant.id));

            if (event.quest.isLeaderboardEnabled()) {
                final LeaderboardMember savedLeaderboardMember = leaderboardService.initializeLeaderboardMember(savedParticipant);

                Logger.info(format("Successfully saved leaderboard member with ID %s", savedLeaderboardMember.id));
            }

            // If possible, map the user to the team for the quest
            Long selectedTeam;
            if ((selectedTeam = participant.person.teamId) != null) {

                // Team was assigned in form, so grab the participant's user account (if possible)
                User participantUser;
                if ((participantUser = UserHome.findByEmail(participant.person.email, em)) != null) {
                    QuestTeamDAO qtd = new QuestTeamDAO(em);
                    QuestTeam qt = qtd.getTeam(selectedTeam);

                    // Check if this user has this quest in progress
                    QuestActivity questActivity = QuestActivityHome.getQuestActivityForQuestIdAndUser(qt.getQuest(), participantUser, em);
                    boolean startInactive = ((questActivity == null) || COMPLETE.equals(questActivity.getStatus()));

                    // Found the user so let's map them to the team if we can
                    if (qtd.joinTeam(qt, participantUser, startInactive)) {
                        Logger.info("saveOrderParticipants - added user " + participant.person.email + " to team " + selectedTeam + (startInactive ? " as inactive member" : ""));
                    } else {
                        Logger.warn("saveOrderParticipants - failed to add " + participant.person.email + " to team " + selectedTeam);
                    }
                } else {
                    Logger.warn("saveOrderParticipants - participant " + participant.person.email + " selected team " + selectedTeam + ", but can't find a corresponding user so taking no action.");
                }
            }
        }
        return participantForms;
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result orderTickets(final String questId) {
        if (isBlank(questId) || !isNumeric(questId)) {
            return badRequest("Quest ID must be a number");
        }
        final Form<OrderForm> form = formFactory.form(OrderForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final long _questId = Long.parseLong(questId);
        final String currency = configuration.getString("application.currency");

        // TODO: questId is a long in the db
        final Happening event = jpaApi.withTransaction(em -> new HappeningDAO(em).getHappeningByQuestId((int) _questId));
        if (event == null) {
            return badRequest("No event found associated to quest with ID " + questId);
        }
        final Integer merchantUserId = event.quest.getCreatedBy();
        final StripeAccount merchant = jpaApi.withTransaction(em -> userProvider.getStripeCustomerByUserId(merchantUserId, StripeAccount.class));
        if (merchant == null) {
            return badRequest("Stripe account doesn't exist for merchant with user ID  " + merchantUserId);
        }

        final User user = this.userProvider.getUser(session());
        final StripeCustomer existingCustomer = user == null ? null : userProvider.getStripeCustomerByUserId(user.getId(), StripeCustomer.class);
        if (existingCustomer == null && user != null) {
            createStripeCustomer(user);
        }

        return handleStripeCustomerRequest(customer -> {
            final OrderForm orderForm = form.get();

            // added condition to check if incoming call requires password, 0 tickets and guests
            if (isNotFalse(orderForm.getCheckPassword()) && !formSecurityService.formPasswordMatches(customer.user, orderForm.getPassword())) {
                return forbidden();
            }
            final OrderBillingForm billingForm = orderForm.getBillingInfo();
            final String billingEmail = billingForm.getPersonalInfo().getEmail();
            final StripeShippingDTO billingInfo = buildStripeShippingInfo(orderForm);
            final TicketsPurchaseOrder ticketsPurchaseOrder = buildTicketsPurchaseOrder(orderForm, event, merchant);

            final String chargeId = stripeConnectService.payOrderWithPaymentSource(
                    merchant,
                    customer,
                    billingEmail,
                    billingInfo,
                    ticketsPurchaseOrder,
                    _questId,
                    event.getId()
            );

            // TODO: the code to save the stripe order info to DynamoDB should be here.  Didn't want to refactor the Stripe service just yet.

            final TicketPurchaseTransaction transaction = new TicketPurchaseTransaction();
            transaction.event = event;
            transaction.from = customer;
            transaction.to = merchant;
            transaction.valid = true;
            transaction.stripeTransactionId = chargeId;
            transaction.couponUsed = ticketsPurchaseOrder.getCouponUsed();

            final EntityManager em = jpaApi.em();
            final PaymentTransaction savedTransaction = new PaymentTransactionDAO(em).save(transaction, TicketPurchaseTransaction.class);

            Logger.info(format("Successfully created ticket purchase transaction with ID %s", savedTransaction.id));

            final OrderParticipantForm[] participantForms = saveOrderParticipants(event, orderForm, savedTransaction, em);

            Logger.info(format("Successfully placed ticket purchase order with transaction ID %s", savedTransaction.stripeTransactionId));

            if (orderForm.getReferredQuestToBack() != null) {
                final ReferredQuest referredQuest = em.find(ReferredQuest.class, orderForm.getReferredQuestToBack());
                if (referredQuest == null) {
                    Logger.warn(format("Invalid referred Quest [%s] configuration for event [%s]", orderForm.getReferredQuestToBack(), event.id));
                } else {
                    Logger.info(format("Backing referred Quest [%s] referred in the event [%s]", referredQuest.quest.getId(), referredQuest.event.id));

                    final StripeAccount referredMerchant = userProvider.getStripeCustomerByUserId(referredQuest.quest.getCreatedBy(), StripeAccount.class);
                    final String referralChargeId = stripeConnectService.backUserWithPaymentSource(referredMerchant, customer, ticketsPurchaseOrder.paymentMethod, currency, referredQuest.amount, orderForm.getTip(), _questId);
                    final QuestBackingTransaction referredTransaction = new QuestBackingTransaction();
                    referredTransaction.quest = referredQuest.quest;
                    referredTransaction.from = customer;
                    referredTransaction.to = referredMerchant;
                    referredTransaction.valid = true;
                    referredTransaction.stripeTransactionId = referralChargeId;
                    final QuestBackingTransaction savedReferredTransaction = new PaymentTransactionDAO(em).save(referredTransaction, QuestBackingTransaction.class);

                    Logger.info(format("Successfully created referral Quest backing transaction with ID [%s]", savedReferredTransaction.id));
                }
            }

            final User customerUser = customer.user;

            startQuestAfterTicketPurchase(event, participantForms, customerUser);

            //Calculate transaction for order confirmation
            final TransactionBreakdown breakdown = stripeConnectService.transactionBreakdown(ticketsPurchaseOrder);

            if (event.eventEmail != null) {
                AmazonSESService.sendOrderTicketConfirmationWithEmail(
                        request(),
                        event.quest,
                        event.eventEmail,
                        customerUser.getEmail(),
                        configuration.getString("aws.ses.username"),
                        configuration.getString("aws.ses.password"));
            } else {
                if (!event.registrationTemplate.equalsIgnoreCase("generic")) {
                    emailService.sendOrderTicketConfirmationEmail(
                            request(),
                            event.quest,
                            new EmailPersonalDTO(billingForm.getPersonalInfo().getFirstName(), billingForm.getPersonalInfo().getLastName(), billingEmail),
                            savedTransaction,
                            breakdown,
                            ticketsPurchaseOrder,
                            billingInfo
                    );
                } else {
                    List<String> details = new ArrayList<>();
                    List<HappeningParticipant> participants = new ArrayList<>();
                    //TODO refactor with dynamic fields ASAP.
                    for (final OrderParticipantForm participantForm : participantForms) {
                        final HappeningParticipant participant = fromParticipantForm(participantForm);
                        participants.add(participant);
                    }
                    // Get product information and pricing for order confirmation
                    final Map<String, Integer> orderItems = buildOrderItems(orderForm);
                    final List<ExportedProductVariant> productVariants = stream(ticketsPurchaseOrder.product.variants)
                            .filter(sku -> orderItems.containsKey(sku.id) && orderItems.get(sku.id) > 0)
                            .collect(toList());
                    emailService.sendGenericOrderTicketConfirmationEmail(request(),
                            customer.user,
                            event.quest,
                            savedTransaction.id,
                            billingInfo.address.line1,
                            billingInfo.address.line2,
                            billingInfo.address.city,
                            billingInfo.address.state,
                            billingInfo.address.postalCode,
                            billingInfo.phone,
                            billingEmail,
                            breakdown.brutTotal,
                            productVariants,
                            orderItems,
                            participants,
                            details);
                }
            }
            return ok(TextNode.valueOf(savedTransaction.id.toString()));
        }, userProvider, StripeCustomer.class);
    }

    private void startQuestAfterTicketPurchase(final Happening event, final OrderParticipantForm[] participants, final User customerUser) {
        // Now we add the quest to doing after the user orders the tickets.
        // ensure quest is not in progress for user yet.
        boolean isQuestActivity;
        try (Connection c = dbRo.getConnection()) {
            isQuestActivity = QuestActivityHome.doesQuestActivityExistForUserIdAndQuestId(c, (long) event.quest.getId(), (long) customerUser.getId());
        } catch (SQLException e) {
            Logger.error("orderTickets - quest activity lookup error", e);
            isQuestActivity = false;
        }

        if (isQuestActivity) {
            Logger.warn(format("Api::startQuestAfterTicketPurchase - customer user %s activity already present for Quest [%s]", customerUser.getEmail(), event.quest.getId()));
        } else {
            final String customerEmail = customerUser.getEmail();

            Logger.info(format("Starting Quest with ID [%s] for user '%s' after tickets purchase", event.quest.getId(), customerEmail));

            final QuestTeamInfoForm customersTeamForm = getQuestTeamFromParticipants(participants, customerEmail);
            final QuestMode requestedMode = getTicketOrderQuestMode(event, customersTeamForm);

            // TODO: move this to a higher level later on
            try (Connection c = db.getConnection()) {
                questService.startQuest(c, event.quest, event.quest.getUser(), customerUser, requestedMode, customersTeamForm, null);
            } catch (SQLException e) {
                Logger.error("orderTickets - start quest error", e);
            }
        }
    }

    private QuestTeamInfoForm getQuestTeamFromParticipants(final OrderParticipantForm[] participants, final String customerEmail) {
        return Stream.of(participants)
                .filter(participantForm -> customerEmail.equalsIgnoreCase(participantForm.getParticipantInfo().getEmail()))
                .map(OrderParticipantForm::getTeamInfo)
                .findFirst()
                .orElse(null);
    }

    private QuestMode getTicketOrderQuestMode(final Happening event, final QuestTeamInfoForm customersTeamForm) {
        switch (event.quest.getMode()) {
            case PACE_YOURSELF:
                return PACE_YOURSELF;
            case TEAM:
                return customersTeamForm == null ? PACE_YOURSELF : TEAM;
            case SUPPORT_ONLY:
                return SUPPORT_ONLY;
            default:
                return null;
        }
    }

    @JwtSessionLogin
    @Transactional
    public Result backQuest(final String questId, final String backeeId) {
        if (isBlank(questId) || !isNumeric(questId)) {
            return badRequest("Quest ID must be a number");
        }
        if (isBlank(backeeId) || !isNumeric(backeeId)) {
            return badRequest("User ID must be a number");
        }
        final User user = this.userProvider.getUser(session());
        final boolean guest = user == null;

        final Form<QuestBackingForm> form = formFactory.form(QuestBackingForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final QuestBackingForm questBackingForm = form.get();
        final String message = questBackingForm.getMessage();
        final Long amount = questBackingForm.getAmount();
        final String currency = isBlank(questBackingForm.getCurrency())
                ? configuration.getString("application.currency")
                : questBackingForm.getCurrency();
        final PaymentMode paymentMode = PaymentMode.valueOf(capitalize(questBackingForm.getPaymentMode()));
        final boolean recurring = isTrue(questBackingForm.getRecurrent()) && !guest;

        final EntityManager entityManager = jpaApi.em();
        final Quests quest = QuestsDAO.findById(Integer.valueOf(questId), entityManager);
        final User backeeUser = UserHome.findById(Integer.valueOf(backeeId), entityManager);
        if (quest == null || backeeUser == null) {
            return notFound();
        }

        final boolean isFundraisingQuest = quest.isFundraising();
        final User fundraiser = isFundraisingQuest ? backeeUser : null;
        final FundraisingLink link = isFundraisingQuest ? fundraisingService.getFundraisingLink(quest, fundraiser) : null;
        if (isFundraisingQuest && link == null) {
            return badRequest(format("Fundraising link not found for Quest ID %s and doer %s", quest.getId(), fundraiser.getEmail()));
        }
        final boolean fundraising = link != null;

        final Integer beneficiaryUserId = getBeneficiaryUserId(quest, backeeUser, questBackingForm.getBrandUserId());

        QuestBackingTransaction transaction;
        StripeAccount backee;
        StripeCustomer backer;
        String stripeTransactionId = null;

        final long _questId = Long.parseLong(questId);
        backee = userProvider.getStripeCustomerByUserId(beneficiaryUserId, StripeAccount.class);
        if (guest) {
            backer = null;
        } else {
            final StripeCustomer existing = userProvider.getStripeCustomerByUserId(user.getId(), StripeCustomer.class);
            if (existing == null) {
                backer = createStripeCustomer(user);
            } else {
                backer = existing;
            }
        }

        if (paymentMode != PaymentMode.OfflineMode) {
            PaymentMethod paymentMethod = new PaymentMethod(paymentMode, questBackingForm.getLastFour(), questBackingForm.getToken(), questBackingForm.isSave());

            if (recurring) {
                final boolean absorbFees = isTrue(questBackingForm.getAbsorbFees());
                final boolean payNow = isTrue(questBackingForm.getPayNow());
                stripeTransactionId = stripeConnectService.createQuestBackingSubscription(backee, backer, absorbFees, payNow, quest, paymentMethod, currency, amount, questBackingForm.getTip());
                transaction = new RecurringQuestBackingTransaction();
            } else if (fundraising) {
                stripeTransactionId = stripeConnectService.raiseFundsForQuestWithPaymentSource(backee, backer, link, paymentMethod, currency, amount, questBackingForm.getTip(), _questId);
                transaction = new FundraisingTransaction(fundraiser);
            } else {
                stripeTransactionId = stripeConnectService.backUserWithPaymentSource(backee, backer, paymentMethod, currency, amount, questBackingForm.getTip(), _questId);
                transaction = new QuestBackingTransaction();
            }
            if (guest) {
                final String defaultBuyerId = configuration.getString("application.transactions.defaultBuyer");
                transaction.from = new StripeCustomerDAO(entityManager).loadStripeCustomer(defaultBuyerId);
            } else {
                transaction.from = backer;
            }
            transaction.to = backee;
            transaction.valid = true;
            transaction.quest = quest;
            transaction.stripeTransactionId = stripeTransactionId;
            transaction.isAnonymous = questBackingForm.getAnonymous() != null ? questBackingForm.getAnonymous() : false;
        } else {
            stripeTransactionId = "FREE";
            if (recurring) {
                transaction = new RecurringQuestBackingTransaction();
            } else if (fundraising) {
                transaction = new FundraisingTransaction(fundraiser);
            } else {
                transaction = new QuestBackingTransaction();
            }
            transaction.to = backee;
            transaction.valid = true;
            transaction.quest = quest;
            transaction.from = backer;
            // check if donator email id is saved in db, else use transaction.from = backer
            String donatorEmail = questBackingForm.getBillingInfo().getPersonalInfo().getEmail();
            if (donatorEmail != null) {
                User donatorUser = UserHome.findByEmail(donatorEmail, entityManager);
                if (donatorUser != null) {
                    final StripeCustomer existing = userProvider.getStripeCustomerByUserId(donatorUser.getId(), StripeCustomer.class);
                    if (existing == null) {
                        transaction.from = createStripeCustomer(donatorUser);
                    } else {
                        transaction.from = existing;
                    }
                }
            }
            transaction.stripeTransactionId = stripeTransactionId;
        }
        transaction.isMailing = isTrue(questBackingForm.getMailing());

        final PaymentTransaction savedTransaction;
        final PaymentTransactionDAO dao = daoProvider.paymentTransactionDAO(entityManager);
        if (recurring) {
            savedTransaction = dao.save((RecurringQuestBackingTransaction) transaction, RecurringQuestBackingTransaction.class);
        } else if (fundraising) {
            savedTransaction = dao.save((FundraisingTransaction) transaction, FundraisingTransaction.class);
        } else {
            savedTransaction = dao.save(transaction, QuestBackingTransaction.class);
        }

        if (fundraising && transaction instanceof FundraisingTransaction) {
            fundraisingService.addBackingTransaction(link, (FundraisingTransaction) savedTransaction);
        }

        if (savedTransaction != null) {
            Logger.info(format("Successfully created %s Quest backing transaction with ID %s", (guest ? "guest" : "logged"), savedTransaction.id));

            final PersonalInfoForm personalInfo = questBackingForm.getBillingInfo().getPersonalInfo();
            final boolean absorbFeesBySeller = backee.user.isAbsorbFees();
            final boolean absorbFeesByBuyer = isNotFalse(questBackingForm.getAbsorbFees()) && !absorbFeesBySeller;
            final TransactionBreakdown breakdown = stripeConnectService.transactionBreakdown(
                    amount,
                    PaymentMode.valueOf(capitalize(questBackingForm.getPaymentMode())),
                    absorbFeesBySeller,
                    absorbFeesByBuyer,
                    backee.user,
                    questBackingForm.getTip());

            final QuestBacking questBacking = fromBackerInfoForm(questBackingForm.getBillingInfo());
            questBacking.setPaymentTransaction(savedTransaction);
            questBacking.setBackingDate(Date.from(Instant.now()));
            questBacking.setMessage(trimToNull(questBackingForm.getMessage()));
            if (absorbFeesBySeller) {
                questBacking.setAmountInCents(Long.valueOf(breakdown.brutTotal - breakdown.brutTip).intValue());
                questBacking.setTip(breakdown.brutTip);
            } else if (absorbFeesByBuyer) {
                questBacking.setAmountInCents(Long.valueOf(breakdown.netTotal - breakdown.netTip).intValue());
                questBacking.setTip(breakdown.netTip);
            } else {
                questBacking.setAmountInCents(Long.valueOf(breakdown.brutTotal - breakdown.brutTip).intValue());
                questBacking.setTip(breakdown.brutTip);
            }

            if (paymentMode == PaymentMode.OfflineMode) {
                questBacking.setOfflineMode(true);
            }

            if (isNotBlank(questBackingForm.getBackerDisplayFirstName())
                    || isNotBlank(questBackingForm.getBackerDisplayLastName())) {
                questBacking.setBackedOnBehalf(true);
                questBacking.setBackerFirstName(questBackingForm.getBackerDisplayFirstName());
                questBacking.setBackerLastName(questBackingForm.getBackerDisplayLastName());
            }

            questBacking.setCurrency(questBackingForm.getCurrency());
            final QuestBacking savedBacking = new QuestBackingDAO(entityManager).save(questBacking, QuestBacking.class);

            Logger.info(format("Successfully saved Quest backing %s", savedBacking.id));

            if (isNotBlank(questBackingForm.getBackerDisplayComment()) && questBacking.getBackedOnBehalf() && user != null) {
                QuestCommentsDAO.addCommentsToQuestByUserIdAndQuestId(user.getId(), quest.getId(),
                        null, questBackingForm.getBackerDisplayComment(), savedBacking.id, jpaApi.em());
            }

            // send email notifying of backing success.
            if (fundraising) {
                final String lastFour = getLastFourDigitsOfPaymentMethod(paymentMode, backer, questBackingForm.getLastFour());

                // get team to which user donated to
//                final User questCreator = UserHome.findById((int) quest.getCreatedBy(), entityManager);
                final QuestTeam questTeam = new QuestTeamDAO(entityManager).getAllTeamForQuestAndUser(quest, backeeUser);

                // get MQ detail / parent teams or quests details
                final List<Quests> parentQuestList = new ArrayList<>();
                QuestEdge edgeChild = null;
                try (Connection c = dbRo.getConnection()) {
                    QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
                    edgeChild = qeDao.getQuestForEdge(c, _questId, QuestEdgeType.CHILD.toString());
                } catch (Exception e) {
                    Logger.error("getParentAndMQ - error with edges", e);
                }
                boolean isParentQuest = (edgeChild == null) ? false : true;

                if (isParentQuest) {
                    // get parent name and mq name
                    Quests megaQuestQuest = QuestsDAO.findById((int) edgeChild.getQuestSrc(), entityManager);
                    parentQuestList.add(megaQuestQuest);
                }
                Quests parentQuest = QuestsDAO.findById((int) _questId, entityManager);
                parentQuestList.add(parentQuest);

                emailService.sendFundraisingBackingConfirmationEmail(request(), personalInfo, backee.user, quest,
                        amount, breakdown.brutTotal, breakdown.platformFee + breakdown.stripeFee, questBackingForm.getTip(), paymentMode != PaymentMode.OfflineMode ? lastFour : "Offline",
                    link, questTeam, parentQuestList);
            } else {
                emailService.sendQuestBackingConfirmationEmail(request(), personalInfo, backee.user, quest, amount, savedTransaction.id);
            }

            if (fundraising) {
                emailService.sendFundraisingBackingBeneficiaryEmail(request(), fundraiser, personalInfo, quest, amount, currency);
                if (backee.user.getId().intValue() != fundraiser.getId().intValue()) {
                    emailService.sendFundraisingBackingBeneficiaryEmail(request(), backee.user, personalInfo, quest, amount, currency);
                }
            } else {
                // send email to backee notifying of backing success
                if (!transaction.isAnonymous) {
                    emailService.sendQuestBackedNotificationEmail(request(), personalInfo, quest, amount, message, backee.user);
                } else {
                    emailService.sendQuestBackedAnonNotificationEmail(request(), personalInfo, quest, amount, message, backee.user);
                }
            }
        }

        if (guest && isTrue(questBackingForm.getSignUp())) {
            registerUserAfterPayment(questBackingForm.getBillingInfo().getPersonalInfo(), questBackingForm.getBillingInfo().getAddress());
        }

        return ok(TextNode.valueOf(stripeTransactionId));
    }

    private Integer getBeneficiaryUserId(final Quests quest, final User backeeUser, final Integer brandUserId) {
        final boolean isFundraisingQuest = quest.isFundraising();
        final boolean isMultiSellerQuest = quest.isMultiSellerEnabled();

        final User fundraiser = isFundraisingQuest ? backeeUser : null;
        final FundraisingLink link = isFundraisingQuest ? fundraisingService.getFundraisingLink(quest, fundraiser) : null;
        if (isFundraisingQuest && link == null) {
            throw new IllegalStateException(format("Fundraising link not found for Quest ID %s and doer '%s'", quest.getId(), fundraiser.getEmail()));
        }

        Integer questBrandUserId;
        if (isMultiSellerQuest) {
            questBrandUserId = new BrandConfigDAO(jpaApi).exists(new QuestBrandConfigId(quest.getId(), brandUserId)) ? brandUserId : null;
            //This is currently hardcoded information for the General Fund donation which goes to DIEMlife
            //DL-2297 for more info
            if (brandUserId == 73 || brandUserId == 1386) {
                questBrandUserId = brandUserId;
            }
        } else {
            questBrandUserId = null;
        }
        final Integer multiSellerBackeeId = BackingUtils.getSelectedMultiSellerId(quest, link, questBrandUserId);
        if (isMultiSellerQuest && multiSellerBackeeId == null) {
            throw new IllegalStateException(format("Multi-seller feature not configured for Quest ID %s", quest.getId()));
        }
        final Integer singleSellerBackeeId = isFundraisingQuest ? quest.getCreatedBy() : backeeUser.getId();
        final Integer beneficiaryUserId = isMultiSellerQuest ? multiSellerBackeeId : singleSellerBackeeId;

        Logger.info(format("Beneficiary user ID %s found for Quest ID %s, backee user ID %s and multi-seller brand user ID %s", beneficiaryUserId, quest.getId(), backeeUser.getId(), brandUserId));

        return beneficiaryUserId;
    }

    private String getLastFourDigitsOfPaymentMethod(final @Nonnull PaymentMode paymentMode,
                                                    final @Nullable StripeCustomer backer,
                                                    final @Nullable String creditCardLast4) {
        if (paymentMode == PaymentMode.OfflineMode) {
            return "Offline";
        }
        if (backer == null) {
            return creditCardLast4;
        }
        final String lastFour;
        if (PaymentMode.BankAccount.equals(paymentMode)) {
            final ExportedBankAccountData bankAccountData = stripeConnectService.retrieveFirstBankAccountForCustomer(backer);
            lastFour = bankAccountData == null ? null : bankAccountData.lastFourDigits;
        } else {
            final ExportedCreditCardData creditCardData = stripeConnectService.exportCustomerCreditCard(backer, creditCardLast4);
            lastFour = creditCardData == null ? null : creditCardData.lastFourDigits;
        }
        return lastFour;
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result savePlaidAccount() {
        final PlaidTokenInfo plaidTokenInfo;
        try {
            plaidTokenInfo = fromRequest(PlaidTokenInfo.class);
        } catch (final FormValidationException e) {
            return badRequest(e.getErrorsAsJson());
        }

        return handleStripeCustomerRequest(customer -> {
            final String plaidToken = plaidLinkService.getBankAccountToken(plaidTokenInfo.linkPublicToken, plaidTokenInfo.accountId);
            try {
                final ExportedBankAccountData data = stripeConnectService.createFirstBankAccountForCustomer(customer, plaidToken);
                return ok(Json.toJson(data));
            } catch (final PaymentModeAlreadyExistsException e) {
                Logger.error(e.getMessage(), e);

                return ok(NullNode.getInstance());
            }
        }, userProvider, StripeCustomer.class);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result saveBankAccountForPayments() {
        return saveBankAccount(BankAccountPurpose.PAYMENTS);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result saveBankAccountForPayouts() {
        return saveBankAccount(BankAccountPurpose.PAYOUTS);
    }

    private Result saveBankAccount(final @NotNull BankAccountPurpose accountType) {
        final ExternalAccountForm bankAccountForm;
        try {
            bankAccountForm = fromRequest(ExternalAccountForm.class);
        } catch (final FormValidationException e) {
            return badRequest(e.getErrorsAsJson());
        }

        switch (accountType) {
            case PAYMENTS:
                return handleStripeCustomerRequest(customer -> {
                    try {
                        final ExportedBankAccountData data = stripeConnectService.createFirstBankAccountForCustomer(customer, bankAccountForm.getToken());
                        return ok(data == null ? NullNode.getInstance() : Json.toJson(data));
                    } catch (final PaymentModeAlreadyExistsException e) {
                        Logger.warn(e.getMessage(), e);

                        return ok(Json.toJson(e.getBankAccountData()));
                    }
                }, userProvider, StripeCustomer.class);
            case PAYOUTS:
                return handleStripeCustomerRequest(customer -> {
                    try {
                        final ExportedBankAccountData data = stripeConnectService.createFirstBankAccountForConnectedAccount(customer, bankAccountForm.getToken());
                        return ok(data == null ? NullNode.getInstance() : Json.toJson(data));
                    } catch (final PaymentModeAlreadyExistsException e) {
                        Logger.warn(e.getMessage(), e);

                        return ok(Json.toJson(e.getBankAccountData()));
                    }
                }, userProvider, StripeAccount.class);
            default:
                return badRequest();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listBankAccountsForPayments() {
        return listBankAccounts(BankAccountPurpose.PAYMENTS);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listBankAccountsForPayouts() {
        return listBankAccounts(BankAccountPurpose.PAYOUTS);
    }

    private Result listBankAccounts(final @NotNull BankAccountPurpose type) {
        return handleStripeCustomerRequest(entity -> {
            switch (type) {
                case PAYMENTS:
                    return entity instanceof StripeCustomer
                            ? Optional.ofNullable(stripeConnectService.retrieveFirstBankAccountForCustomer((StripeCustomer) entity))
                            .map(bankAccount -> ok(Json.newArray().add(Json.toJson(bankAccount))))
                            .orElse(ok(Json.newArray()))
                            : ok(Json.newArray());
                case PAYOUTS:
                    return entity instanceof StripeAccount
                            ? Optional.ofNullable(stripeConnectService.retrieveFirstBankAccountForConnectedAccount((StripeAccount) entity))
                            .map(bankAccount -> ok(Json.newArray().add(Json.toJson(bankAccount))))
                            .orElse(ok(Json.newArray()))
                            : ok(Json.newArray());
                default:
                    return badRequest();
            }
        }, userProvider, StripeEntity.class);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result deleteExternalAccount() {
        return handleStripeCustomerRequest(customer -> {
            stripeConnectService.deleteFirstBankAccountForCustomer(customer);
            return ok();
        }, userProvider, StripeCustomer.class);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result verifyExternalAccount() {
        final BankAccountVerificationForm verificationForm;
        try {
            verificationForm = fromRequest(BankAccountVerificationForm.class);
        } catch (final FormValidationException e) {
            return badRequest(e.getErrorsAsJson());
        }
        return handleStripeCustomerRequest(customer ->
                ok(BooleanNode.valueOf(stripeConnectService.verifyFirstBankAccountForCustomer(
                        customer,
                        verificationForm.getFirstDebit(),
                        verificationForm.getSecondDebit()
                ))), userProvider, StripeCustomer.class);
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result createPayout() {
        final PayoutForm payoutForm;
        try {
            payoutForm = fromRequest(PayoutForm.class);
        } catch (final FormValidationException e) {
            return badRequest(e.getErrorsAsJson());
        }
        return handleStripeCustomerRequest(account -> {
            final ExportedPayout payout = stripeConnectService.createPayout(account, payoutForm.getAmount(), payoutForm.getCurrency());

            if (payout.verification != null) {
                final Messages messages = Http.Context.current().messages();

                if (messages.isDefinedAt("error." + payout.verification.reason)) {
                    payout.verification.reason = messages.at("error." + payout.verification.reason);
                }

                final List<String> missingFields = new ArrayList<>();
                payout.verification.missingFields.forEach(field -> {
                    if (messages.isDefinedAt(field)) {
                        missingFields.add(messages.at(field));
                    } else {
                        missingFields.add(field);
                    }
                });
                payout.verification.missingFields.clear();
                payout.verification.missingFields.addAll(missingFields);

                return forbidden(Json.toJson(payout));
            } else {
                payout.payouts.forEach(partialPayout -> {
                    final PayoutTransaction transaction = new PayoutTransaction();
                    transaction.from = account;
                    transaction.to = account;
                    transaction.valid = true;
                    transaction.stripeTransactionId = partialPayout.id;

                    final PaymentTransaction savedTransaction = jpaApi
                            .withTransaction(em -> new PaymentTransactionDAO(em).save(transaction, PayoutTransaction.class));

                    Logger.info(format("Successfully created payout transaction transaction with ID %s", savedTransaction.id));
                });

                return ok(Json.toJson(payout));
            }
        }, userProvider, StripeAccount.class);
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result checkForMoreFieldsNeeded() {
        return handleStripeCustomerRequest(entity -> {
            if (entity instanceof StripeAccount) {
                final Optional<String> neededField = stripeConnectService.doesAccountNeedFields((StripeAccount) entity);

                return neededField.isPresent() ? ok(Json.toJson(neededField)) : ok(NullNode.getInstance());
            } else {
                return ok(Json.newArray());
            }
        }, userProvider, StripeEntity.class);
    }

    private static class FormValidationException extends Exception {
        private final JsonNode errorsAsJson;

        private FormValidationException(final JsonNode errorsAsJson) {
            this.errorsAsJson = errorsAsJson;
        }

        private JsonNode getErrorsAsJson() {
            return errorsAsJson;
        }
    }

    private <T> T fromRequest(final Class<T> type) throws FormValidationException {
        final Form<T> form = formFactory.form(type).bindFromRequest();
        if (form.hasErrors()) {
            throw new FormValidationException(form.errorsAsJson());
        } else {
            return form.get();
        }
    }

    private QuestBacking fromBackerInfoForm(final BackerInfoForm backerInfoForm) {
        final QuestBacking backing = new QuestBacking();
        if (backerInfoForm != null && backerInfoForm.getAddress() != null) {
            backing.setBillingAddress(fromCountryZipForm(backerInfoForm.getAddress()));
        }
        if (backerInfoForm != null && backerInfoForm.getPersonalInfo() != null) {
            backing.setBillingPersonalInfo(fromPersonalInfoForm(backerInfoForm.getPersonalInfo()));
        }
        return backing;
    }

    private Address fromCountryZipForm(final CountryZipForm countryZipForm) {
        if (countryZipForm == null || isBlank(countryZipForm.getCountry()) || isBlank(countryZipForm.getZip())) {
            return null;
        }
        final Address address = new Address();
        address.country = countryZipForm.getCountry();
        address.zip = countryZipForm.getZip();
        return address;
    }

    private PersonalInfo fromPersonalInfoForm(final PersonalInfoForm personalInfoForm) {
        final PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.firstName = personalInfoForm.getFirstName();
        personalInfo.lastName = personalInfoForm.getLastName();
        personalInfo.email = personalInfoForm.getEmail();
        return personalInfo;
    }

    private HappeningParticipant fromParticipantForm(final OrderParticipantForm participantForm) {
        final HappeningParticipant participant = new HappeningParticipant();

        if (participantForm != null && participantForm.getAddress() != null) {
            final Address address = fromCountryZipForm(participantForm.getAddress());
            if (address != null) {
                address.lineOne = participantForm.getAddress().getStreetNo();
                address.lineTwo = participantForm.getAddress().getStreetNoAdditional();
                address.city = participantForm.getAddress().getCity();
                address.state = participantForm.getAddress().getState();
            }
            participant.address = address;
        }

        if (participantForm != null && participantForm.getParticipantInfo() != null) {
            final ParticipantInfoForm participantInfo = participantForm.getParticipantInfo();

            final PersonalInfo personalInfo = fromPersonalInfoForm(participantInfo);

            personalInfo.homePhone = participantInfo.getHomePhone();
            personalInfo.cellPhone = participantInfo.getCellPhone();
            personalInfo.gender = participantInfo.getGender();
            personalInfo.birthDate = participantInfo.getBirthDate();
            personalInfo.age = isNumeric(participantInfo.getAge()) ? parseInt(participantInfo.getAge()) : null;
            personalInfo.shirtSize = participantInfo.getShirtSize();
            personalInfo.burgerTemp = participantInfo.getBurgerTemp();
            personalInfo.withCheese = participantInfo.getWithCheese();
            personalInfo.specialRequests = participantInfo.getSpecialRequests();

            final QuestTeamInfoForm questTeamInfo = participantForm.getTeamInfo();
            personalInfo.teamId = questTeamInfo == null ? null : questTeamInfo.getQuestTeamId();

            participant.person = personalInfo;
        }

        if (participantForm != null && participantForm.getEmergencyContact() != null) {
            final EmergencyContact emergencyContact = new EmergencyContact();
            emergencyContact.name = participantForm.getEmergencyContact().getName();
            emergencyContact.phone = participantForm.getEmergencyContact().getNumber();
            emergencyContact.email = participantForm.getEmergencyContact().getEmail();

            participant.contact = emergencyContact;
        }

        if (participantForm != null) {
            participant.stripeSkuId = participantForm.getSkuId();
            participant.skuPrice = participantForm.getSkuPrice();
            participant.skuFee = participantForm.getSkuFee();
        }

        return participant;
    }

    private Address fromBillingAddressForm(AddressForm form) {
        Address address = new Address();
        address.city = form.getCity();
        address.country = form.getCountry();
        address.lineOne = form.getStreetNo();
        address.lineTwo = form.getStreetNoAdditional();
        address.state = form.getState();
        address.zip = form.getZip();
        return address;
    }

    private static <T extends StripeEntity> Result handleStripeCustomerRequest(final StripeCustomerRequestHandler<T> handler, final UserProvider userProvider, final Class<T> type) {
        return handler.handleRequest(userProvider, type);
    }

    private interface StripeCustomerRequestHandler<T extends StripeEntity> extends Function<T, Result> {
        default Result handleRequest(final UserProvider userProvider, final Class<T> type) {
            noCache(response());
            final User user = userProvider.getUser(session());
            final StripeEntity connectedCustomer = user == null ? null : user.getStripeEntity();
            if (connectedCustomer == null) {
                return notFound();
            } else if (type.isAssignableFrom(connectedCustomer.getClass())) {
                return apply(type.cast(connectedCustomer));
            } else {
                return forbidden();
            }
        }
    }

    private static Result handleHappeningRequest(final HappeningRequestHandler handler, final String questId, final JPAApi jpaApi) {
        return handler.handleRequest(questId, jpaApi);
    }

    private interface HappeningRequestHandler extends Function<Happening, Result> {
        default Result handleRequest(final String questId, final JPAApi jpaApi) {
            if (isBlank(questId) || !isNumeric(questId)) {
                return badRequest();
            }
            final Happening event = jpaApi.withTransaction(em -> new HappeningDAO(em).getHappeningByQuestId(parseInt(questId)));
            if (event == null) {
                Logger.info("No event found associated to quest with ID " + questId);

                return ok(NullNode.getInstance());
            }
            return apply(event);
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result recentTransactions() {
        final Form<TransactionListFilterForm> form = formFactory.form(TransactionListFilterForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final User user = userProvider.getUser(session());
        if (user == null) {
            throw new IllegalStateException("Session user must not be null");
        }
        final List<TransactionResponse> result = paymentTransactionFacade.listTransactions(user, isTrue(form.get().getAll()), false, jpaApi.em(), Optional.empty());
        return ok(Json.toJson(result));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result listQuestStarred() {
        final User user = this.userProvider.getUser(session());
        if (user != null) {
            final EntityManager em = jpaApi.em();
            final QuestUserFlagDAO dao = new QuestUserFlagDAO(em);
            final boolean withMilestones = toBoolean(request().getQueryString("milestones"));
            final boolean withPercentage = toBoolean(request().getQueryString("percentage"));
            try {
                return ok(populateQuestsListResponse(dao.retrieveStarredQuests(user)
                        .stream()
                        .map(id -> QuestsDAO.findById(id, em))
                        .collect(toList()), user, withMilestones, withPercentage));
            } catch (final RequiredParameterMissingException e) {
                return badRequest();
            }
        } else {
            return unauthorized();
        }
    }

    private ArrayNode populateQuestsListResponse(final List<Quests> quests, final User user, boolean withMilestones, boolean withPercentage) {
        final ArrayNode result = Json.newArray();
        final EntityManager em = jpaApi.em();
        quests.forEach(quest -> {
            quest.getAdmins().forEach(em::detach);
            em.detach(quest.getUser());
            em.detach(quest);

            final ObjectNode questNode = (ObjectNode) Json.toJson(quest);
            result.add(questNode);
            if (withMilestones || withPercentage) {
                final List<QuestTasks> milestones = questService.listMilestonesForQuest(quest, user);
                if (withMilestones) {
                    questNode.set("milestones", Json.toJson(milestones));
                }
                if (withPercentage) {
                    questNode.set("percentage", TextNode.valueOf(getQuestCompletionPercentage(milestones)));
                }
            }
        });
        return result;
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result toggleQuestStarred(final Integer questId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(questId, em);
        if (quest == null) {
            return notFound();
        }
        final QuestUserFlagDAO dao = new QuestUserFlagDAO(em);
        try {
            final QuestUserFlag starFlag;
            if (dao.isStarredQuestForUser(quest.getId(), user.getId())) {
                starFlag = dao.unStarQuestForUser(quest, user);
            } else {
                starFlag = dao.starQuestForUser(quest, user);
            }
            return ok(BooleanNode.valueOf(starFlag.flagValue));
        } catch (final RequiredParameterMissingException e) {
            return badRequest();
        }
    }

    @Transactional
    public Result generateActivationPinCode() {
        final DynamicForm form = formFactory.form().bindFromRequest();
        final String email = form.get("email");
        if (isBlank(email)) {
            return badRequest();
        }
        final User user = getByEmail(email, jpaApi.em());
        if (user == null) {
            return notFound();
        }
        final UserActivationPinCode code = userActivationService.generateCode(user);
        if (code == null) {
            return notFound();
        }

        emailService.sendUserActivationPinCodeEmail(request(), user, code);

        return ok();
    }

    @Transactional
    public Result consumeActivationPinCode() {
        final DynamicForm form = formFactory.form().bindFromRequest();
        final String email = form.get("email");
        final String code = form.get("code");
        if (isBlank(code) || isBlank(email)) {
            return badRequest();
        }
        final EntityManager em = jpaApi.em();
        final User user = getByEmail(email, em);
        if (user == null) {
            return notFound();
        }
        if (userActivationService.isValidPinCode(user, code)) {
            userActivationService.consumePinCode(user, code);

            final UserHome userHome = new UserHome();
            final User verified = userHome.verify(user, em);

            try {
                createStripeCustomer(verified);
            } catch (final StripeInvalidPostalCodeException e) {
                return created(e.getMessage());
            } catch (final StripeApiCallException e) {
                Logger.error(format("Failed to create Stripe entities for verified user %s", user.getEmail()), e);
            }

            return ok();
        } else {
            return badRequest();
        }
    }

    @Transactional
    public Result startFundraising() {
        final Form<FundraisingForm> formBinding = this.formFactory.form(FundraisingForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final FundraisingForm form = formBinding.get();
        return jpaApi.withTransaction(em -> {
            final Quests quest = em.find(Quests.class, form.getQuestId());
            final User fundraiser = em.find(User.class, form.getDoerId());
            //This is currently hardcoded information for the General Fund donation which goes to DIEMlife
            //DL-2297 for more info
            BrandConfig brand = new BrandConfigDAO(jpaApi).exists(new QuestBrandConfigId(form.getQuestId(), form.getBrandUserId()))
                    ? em.find(BrandConfig.class, form.getBrandUserId())
                    : null;
            if (form.getBrandUserId() == 73 || form.getBrandUserId() == 1386) {
                brand = em.find(BrandConfig.class, form.getBrandUserId());
            }
//            final BrandConfig brand = new BrandConfigDAO(jpaApi).exists(new QuestBrandConfigId(form.getQuestId(), form.getBrandUserId()))
//                    ? em.find(BrandConfig.class, form.getBrandUserId())
//                    : null;
            final BrandConfig secondaryBrand = new BrandConfigDAO(jpaApi).exists(new QuestBrandConfigId(form.getQuestId(), form.getSecondaryBrandUserId()))
                    ? em.find(BrandConfig.class, form.getSecondaryBrandUserId())
                    : null;
            final boolean update = fundraisingService.fundraisingLinkExists(quest, fundraiser);
            final User creator = em.find(User.class, quest.getCreatedBy());
            final FundraisingLinkDTO link = fundraisingService.startFundraising(
                    quest,
                    fundraiser,
                    form.getTargetAmount(),
                    form.getCurrency(),
                    brand,
                    secondaryBrand,
                    form.getCampaignName(),
                    form.getCoverImageUrl()
            );
            if (link == null) {
                return notFound();
            } else {
                if (!update) {
                    final Http.Request request = request();
                    emailService.sendFundraisingStartFundraiserEmail(request, creator, fundraiser, quest);
                    emailService.sendFundraisingNotificationEmail(request, creator, fundraiser, quest);
                }
                final List<Integer> friendIds = UserRelationshipDAO.getCurrentFriendsByUserId(fundraiser.getId(), jpaApi.em());
                AsyncUtils.processIdsAsync(configuration, friendIds, friend ->
                        notificationService.addQuestNotification(
                                friend,
                                FUNDRAISER_STARTED,
                                fundraiser.getId(),
                                quest.getId()
                        ));
                return ok(Json.toJson(link));
            }
        });
    }

    @Transactional
    public Result fundraisingParties(final @NotNull String questId, final @NotNull String doerId) {
        if (isNumeric(questId) && isNumeric(doerId)) {
            final FundraisingLinkDTO dto = fundraisingService.setupParties(parseInt(questId), parseInt(doerId));
            if (dto == null) {
                return ok(NullNode.getInstance());
            } else {
                return ok(Json.toJson(dto));
            }
        } else {
            return badRequest();
        }
    }

    // Helper function to try and capture logic that might need to be run multiple times to sum up values for a parent quest
    private boolean fundraisingLinkHelper(FundraisingLinkDTO originalDto, FundraisingLinkDTO dto, int questId, int doerId) {
        final StripeAccount beneficiary = userProvider.getStripeCustomerByUserId(dto.creator.id, StripeAccount.class);
        if (beneficiary == null) {
            Logger.warn(format("Beneficiary not found for fundraising Quest %s", questId));
            return false;
        }

        final FundraisingSummary summary;
        if (dto.creator.id.equals(dto.doer.id)) {
            summary = fundraisingService.getQuestFundraisingLinks(dto.quest.id)
                    .stream()
                    .map(link -> calculateCurrentFundraisingAmount(link, beneficiary))
                    .reduce(FundraisingSummary.empty(), FundraisingSummary::add);
        } else {
            summary = calculateCurrentFundraisingAmount(dto, beneficiary);
        }

        // Note: this had been wrapping the summation variable in an AtomicReference ... have no idea why that was done.  It
        // looked WACKY!  If all of a sudden there are mysterious bugs with fundraising amounts, revisit my changes.
        long supplementAmount = 0;
        List<FundraisingSupplement> supplements = getFundraisingSupplement(questId, doerId, jpaApi.em());
        for (FundraisingSupplement supplement : supplements) {
            supplementAmount += supplement.getAmount();
        }

        if (originalDto.currentAmount == null) {
            originalDto.currentAmount = summary.amount + supplementAmount;
        } else {
            originalDto.currentAmount += summary.amount + supplementAmount; 
        }

        if (originalDto.timesBacked == null) {
            originalDto.timesBacked = summary.count + supplements.size();
        } else {
            originalDto.timesBacked += summary.count + supplements.size();
        }

        if (originalDto != dto) {
            if (originalDto.targetAmount == null) {
                originalDto.targetAmount = dto.targetAmount;
            } else {
                originalDto.targetAmount += dto.targetAmount;
            }
        }

        return true;
    }

    @Transactional
    public Result fundraisingLink(final String questId, final String doerId) {
        if (isNumeric(questId) && isNumeric(doerId)) {
            int _questId = parseInt(questId);
            int _doerId =  parseInt(doerId);

            // TODO: factor this out into some shared thing
            long __questId = (long) _questId;
            List<QuestEdge> children;
            try (Connection c = dbRo.getConnection()) {
                QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
                children = qeDao.getEdgesByType(c, __questId, QuestEdgeType.CHILD.toString());
            } catch (Exception e) {
                Logger.error("fundraisingLink - error with edges", e);
                children = new LinkedList<QuestEdge>();
            }
            boolean isMegaQuest = !children.isEmpty();

            FundraisingLinkDTO dto = fundraisingService.getFundraisingLinkDTO(_questId, _doerId);
            boolean noLinkFound = (dto == null);

            if (!isMegaQuest && noLinkFound) {
                return ok(NullNode.getInstance());
            } else if (isMegaQuest && noLinkFound) {
                dto = fundraisingService.getFundraisingLinkForParentQuest(_questId, _doerId);
            }

            if (isMegaQuest) {
                for (QuestEdge child : children) {
                    long childQuestId = child.getQuestDst();

                    // grab the quest link and add it to total
                    // get the Quest detail and pass the quest id and doer id as creator id of quest
                    final EntityManager em = this.jpaApi.em();
                    final Quests quest = QuestsDAO.findById((int) childQuestId, em);
                    FundraisingLinkDTO _dto = fundraisingService.getFundraisingLinkDTO((int) childQuestId, quest.getCreatedBy());

                    if (_dto == null) {
                        Logger.warn("fundraisingLink - child quest " + childQuestId + " has no link ... is that expected?");
                        continue;
                    }

                    if (!fundraisingLinkHelper(dto, _dto, (int) childQuestId, quest.getCreatedBy())) {
                        continue;
                    }
                }
            } else {
                // not a parent quest so use trickery of original and current dto being the same
                if (!fundraisingLinkHelper(dto, dto, _questId, _doerId)) {
                    return badRequest();
                }
            }

            // get transaction amount also here for the tickets purchase
            List<Long> ticketAmount = new LinkedList<>();
            final EntityManager em = this.jpaApi.em();
            final Quests quest = QuestsDAO.findById(_questId, em);
            final StripeAccount merchant = userProvider.getStripeCustomerByUserId(quest.getCreatedBy(), StripeAccount.class);
            if (merchant != null) {
                final PaymentTransactionDAO transactionDAO = new PaymentTransactionDAO(em);
                final List<TicketPurchaseTransaction> orders = transactionDAO.getHappeningTransactionsForQuest(_questId);
                orders.forEach(order -> {
                    if (!(("FREE").equalsIgnoreCase(order.stripeTransactionId))) {
                        final Charge stripeCharge = stripeConnectService.retrieveChargeInformation(order.stripeTransactionId, merchant, true);
                        final Pair<Long, String> amountAndCurrency = Optional.ofNullable(stripeCharge)
                            .map(Charge::getTransferObject)
                            .map(transfer -> new ImmutablePair<>(transfer.getAmount(), transfer.getCurrency()))
                            .orElse(Optional.ofNullable(stripeCharge)
                                    .map(Charge::getBalanceTransactionObject)
                                    .map(transaction -> new ImmutablePair<>(transaction.getNet().longValue(), transaction.getCurrency()))
                                    .orElse(Optional.ofNullable(stripeCharge)
                                            .map(charge -> new ImmutablePair<>(charge.getAmount(), charge.getCurrency()))
                                            .orElse(new ImmutablePair<>(0L, "usd"))
                                    )
                            );

                        ticketAmount.add(amountAndCurrency.getLeft());
                    }
                });
            }

            dto.ticketTotalAmount = (long) ticketAmount.stream().mapToDouble(a -> a).sum();

            return ok(Json.toJson(dto));
    } else {
            return badRequest();
        }
    }

    private FundraisingSummary calculateCurrentFundraisingAmount(final FundraisingLinkDTO dto, final StripeAccount beneficiary) {
        //TODO stripeSummary left commented out unless users are to absorb fees, if this is the case, remove tip from stripeSummary
//        final List<Charge> charges = stripeConnectService.retrieveCharges(dto.quest.id, dto.doer.id, beneficiary);
        final List<FundraisingTotalDTO> totals = paymentTransactionFacade.getQuestBackingFundraisingTotals(dto);

//        final FundraisingSummary stripeSummary = charges.stream()
//                .filter(charge -> !FAILED_STRIPE_STATUS.equalsIgnoreCase(charge.getStatus()))
//                .map((Charge charge) -> FundraisingSummary.from(charge, beneficiary.user))
//                .reduce(FundraisingSummary.empty(), FundraisingSummary::add);
        final FundraisingSummary backingSummary = totals.stream()
                .map(total -> new FundraisingSummary((long) total.getAmount(), 1))
                .reduce(FundraisingSummary.empty(), FundraisingSummary::add);
//        return stripeSummary.add(backingSummary);
        return backingSummary;
    }

    private static class FundraisingSummary {
        private final Long amount;
        private final int count;

        private FundraisingSummary(final Long amount, final int count) {
            this.amount = amount;
            this.count = count;
        }
        //TODO stripeSummary left commented out unless users are to absorb fees, if this is the case, remove tip from stripeSummary
//        private static FundraisingSummary from(final Charge charge, final User beneficiary) {
//            final Long netAmount;
//            if (charge == null) {
//                netAmount = 0L;
//            } else if (beneficiary != null && beneficiary.isAbsorbFees()) {
//                netAmount = charge.getAmount() == null ? 0L : charge.getAmount().intValue();
//            } else if (charge.getTransferObject() != null) {
//                netAmount = charge.getTransferObject().getAmount();
//            } else if (charge.getBalanceTransactionObject() != null) {
//                netAmount = charge.getBalanceTransactionObject().getNet();
//            } else {
//                netAmount = 0L;
//            }
//            return new FundraisingSummary(netAmount, 1);
//        }

        private static FundraisingSummary empty() {
            return new FundraisingSummary(0L, 0);
        }

        private FundraisingSummary add(final FundraisingSummary delta) {
            Long amount = this.amount + delta.amount;
            return new FundraisingSummary(amount, this.count + delta.count);
        }
    }

}
