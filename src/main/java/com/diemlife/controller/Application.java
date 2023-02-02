package com.diemlife.controller;

import com.diemlife.acl.QuestEntityWithACL;
import com.diemlife.acl.QuestsListWithACL;
import com.diemlife.acl.VoterPredicate.VotingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.stripe.model.Subscription;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.constants.ImageDimensions;
import com.diemlife.constants.ImageType;
import com.diemlife.constants.Interests;
import com.diemlife.constants.NotificationType;
import com.diemlife.constants.PrivacyLevel;
import com.diemlife.constants.QuestActivityStatus;
import com.diemlife.constants.QuestCreatorTypes;
import com.diemlife.constants.QuestMode;
import com.diemlife.constants.UserRelationshipStatus;
import com.diemlife.constants.VideoProvider;
import com.diemlife.dao.*;
import com.diemlife.dto.AllPillarsCount;
import com.diemlife.dto.AsActivityAttributesDTO;
import com.diemlife.dto.CommentsDTO;
import com.diemlife.dto.ExploreDTO;
import com.diemlife.dto.FundraisingLinkDTO;
import com.diemlife.dto.LeaderboardScoreDTO;
import com.diemlife.dto.MilestoneDTO;
import com.diemlife.dto.QuestActivityDTO;
import com.diemlife.dto.QuestDTO;
import com.diemlife.dto.QuestImageDTO;
import com.diemlife.dto.QuestListDTO;
import com.diemlife.dto.QuestListDetailDTO;
import com.diemlife.dto.QuestLiteDTO;
import com.diemlife.dto.QuestPermissionsDTO;
import com.diemlife.dto.QuestTeamDTO;
import com.diemlife.dto.QuestUserFlagsDTO;
import com.diemlife.dto.UserDTO;
import com.diemlife.dto.UserProfileStatsDTO;
import com.diemlife.dto.UserQuestsStatsDTO;
import com.diemlife.dto.UserSearchDTO;
import com.diemlife.dto.UserToInviteDTO;
import com.diemlife.dto.UserWithFiendStatusDTO;
import com.diemlife.dto.WhoIAmDTO;
import exceptions.QuestOperationForbiddenException;
import forms.AttributeValueForm;
import forms.CommentAddForm;
import forms.MilestoneToggleForm;
import forms.QuestActionPointForm;
import forms.QuestCompletionForm;
import forms.QuestCreateForm;
import forms.QuestEditForm;
import forms.QuestForm;
import forms.QuestRepeatForm;
import forms.QuestStartForm;
import forms.QuestTeamInfoForm;
import forms.QuestTeamInfoForm.TeamAction;
import forms.RealtimeQuestForm;
import forms.ShareQuestForm;
import forms.TaskCreateForm;
import forms.TaskEditForm;
import forms.TasksGroupManageForm;
import forms.TasksGroupManageEditForm;

import models.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.util.StopWatch;
import play.Environment;
import play.Logger;
import play.cache.CacheApi;
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
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http;
import play.libs.Files.TemporaryFile;
import com.diemlife.providers.MyUsernamePasswordAuthProvider;
import com.diemlife.security.JwtSessionLogin;
import com.diemlife.services.ActivityService;
import com.diemlife.services.AmazonSESService;
import com.diemlife.services.AsActivityService;
import com.diemlife.services.CommentsService;
import com.diemlife.services.ContentReportingService;
import com.diemlife.services.FundraisingService;
import com.diemlife.services.ImageProcessingService;
import com.diemlife.services.LeaderboardService;
import com.diemlife.services.LinkPreviewService;
import com.diemlife.services.MilestoneService;
import com.diemlife.services.NotificationFeed;
import com.diemlife.services.NotificationService;
import com.diemlife.services.OutgoingEmailService;
import com.diemlife.services.QuestListService;
import com.diemlife.services.QuestService;
import com.diemlife.services.SeoService;
import com.diemlife.services.StripeConnectService;
import com.diemlife.services.TaskGroupService;
import com.diemlife.services.UserProvider;
import com.diemlife.services.UserService;
import com.diemlife.utils.*;
import com.diemlife.utils.URLUtils.QuestSEOSlugs;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;
import javax.xml.ws.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.diemlife.acl.VoterPredicate.VotingResult.Abstain;
import static com.diemlife.acl.VoterPredicate.VotingResult.For;
import static com.feth.play.module.pa.controllers.AuthenticateBase.noCache;
import static com.diemlife.constants.NotificationType.PHOTO_VIDEO_ADDED;
import static com.diemlife.constants.NotificationType.QUEST_ACHIEVED;
import static com.diemlife.constants.NotificationType.QUEST_SAVED;
import static com.diemlife.constants.NotificationType.QUEST_STARTED;
import static com.diemlife.constants.PrivacyLevel.INVITE;
import static com.diemlife.constants.PrivacyLevel.PUBLIC;
import static com.diemlife.constants.QuestActivityStatus.COMPLETE;
import static com.diemlife.constants.QuestActivityStatus.IN_PROGRESS;
import static com.diemlife.constants.QuestCreatorTypes.BRAND;
import static com.diemlife.constants.QuestCreatorTypes.MEMBER;
import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.SUPPORT_ONLY;
import static com.diemlife.constants.QuestMode.TEAM;
import static com.diemlife.dao.QuestsDAO.findById;
import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static com.diemlife.models.QuestEvents.QUEST_CREATE;
import static com.diemlife.models.QuestEvents.QUEST_EDIT;
import static com.diemlife.models.QuestEvents.QUEST_EDIT_NEW;
import static com.diemlife.models.QuestEvents.QUEST_SAVE;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.springframework.util.StringUtils.hasText;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;
import static com.diemlife.utils.URLUtils.publicTeamQuestSEOSlugs;

public class Application extends Controller {

    public static final String FLASH_MESSAGE_KEY = "message";
    public static final String FLASH_ERROR_KEY = "error";
    public static final String USER_ROLE = "user";
    private static final Integer MAX_SHARE_QUEST_EMAILS = 10;
    private final Config config;
    private final MyUsernamePasswordAuthProvider provider;
    private final UserProvider userProvider;
    private final FormFactory formFactory;
    private final CacheApi cache;
    private final JPAApi jpaApi;
    private final StripeConnectService stripeService;
    private final OutgoingEmailService emailService;
    private final FundraisingService fundraisingService;
    private final NotificationService notificationService;
    private final LeaderboardService leaderboardService;
    private final QuestService questService;
    private final CommentsService commentsService;
    private final ContentReportingService contentReportingService;
    private final SitemapUtil sitemapUtil;
    private final Environment environment;
    private final ImageProcessingService imageService;
    private final LinkPreviewService linkPreviewService;
    private final SeoService seoService;
    private final MessagesApi messages;
    private final MilestoneService milestoneService;
    private final TaskGroupService taskGroupService;
    private final QuestListService questListService;
    private final ActivityService activityService;
    private final AsActivityService asActivityService;

    private Database db;
    private Database dbRo;

    @Inject
    public Application(
            Database db,
            @NamedDatabase("ro") Database dbRo,
            final Config config,
            final MyUsernamePasswordAuthProvider provider,
            final FormFactory formFactory,
            final CacheApi cache,
            final UserProvider userProvider,
            final JPAApi api,
            final StripeConnectService stripeService,
            final OutgoingEmailService emailService,
            final FundraisingService fundraisingService,
            final NotificationService notificationService,
            final LeaderboardService leaderboardService,
            final QuestService questService,
            final CommentsService commentsService,
            final ContentReportingService contentReportingService,
            final SitemapUtil sitemapUtil,
            final Environment environment,
            final ImageProcessingService imageService,
            final LinkPreviewService linkPreviewService,
            final SeoService seoService,
            final MessagesApi messages,
            final MilestoneService milestoneService,
            final TaskGroupService taskGroupService,
            final QuestListService questListService,
            final ActivityService activityService,
            final AsActivityService asActivityService) {

        this.db = db;
        this.dbRo = dbRo;
        this.config = config;
        this.provider = provider;
        this.userProvider = userProvider;
        this.jpaApi = api;
        this.formFactory = formFactory;
        this.cache = cache;
        this.stripeService = stripeService;
        this.emailService = emailService;
        this.fundraisingService = fundraisingService;
        this.notificationService = notificationService;
        this.leaderboardService = leaderboardService;
        this.questService = questService;
        this.commentsService = commentsService;
        this.contentReportingService = contentReportingService;
        this.sitemapUtil = sitemapUtil;
        this.environment = environment;
        this.imageService = imageService;
        this.linkPreviewService = linkPreviewService;
        this.seoService = seoService;
        this.messages = messages;
        this.milestoneService = milestoneService;
        this.taskGroupService = taskGroupService;
        this.questListService = questListService;
        this.activityService = activityService;
        this.asActivityService = asActivityService;
    }

    @Transactional
    public Result index() {
        // some of the auth logic relies on redirecting to index.
        // We dont care, so just leave it returning a 200  .
        return ok();
    }

    @JwtSessionLogin
    @Transactional
    public Result whoAmI() {
        noCache(response());
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return ok(Json.newObject());
        } else {
            if (user.getId() == null || user.getEmail() == null) {
                throw new IllegalStateException();
            }
            return ok(Json.toJson(new WhoIAmDTO(user.getId(), user.getEmail())));
        }
    }

    @Transactional
    public Result sitemap() {
        EntityManager em = this.jpaApi.em();
        this.sitemapUtil.sitemapGenerator(em);

        return ok(this.environment.getFile("public/sitemap/sitemap.xml"));
    }

    @JwtSessionLogin(required = true)
    public Result siteConfig() {
        final ObjectNode node = Json.newObject();
        node.put("stripe.publicKey", config.getString("stripe.pub.key"));

        return ok(node);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result shareQuest() {
        final Form<ShareQuestForm> form = formFactory.form(ShareQuestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final ShareQuestForm shareQuestForm = form.get();
        final Integer questId = shareQuestForm.getQuestId();
        final String bwCompatibilityEmail = shareQuestForm.getEmail();
        final List<String> emails = shareQuestForm.getEmails();
        final String message = shareQuestForm.getMessage();

        final List<String> allEmails = new ArrayList<>();
        if (isNotBlank(bwCompatibilityEmail)) {
            allEmails.add(bwCompatibilityEmail);
        }
        if (isNotEmpty(emails)) {
            allEmails.addAll(emails);
        }
        if (isEmpty(allEmails) || allEmails.size() > MAX_SHARE_QUEST_EMAILS) {
            return badRequest();
        }

        //Ensure we have no empty emails
        allEmails.removeAll(Arrays.asList("", null));

        final EntityManager em = this.jpaApi.em();
        Quests quest = findById(questId, em);

        try {
            allEmails.forEach(email ->
                    emailService.sendShareQuestEmail(request(), user, quest, email, message, UserService.getByEmail(email, em).getFirstName())
            );

            // increment the share count on the quest
            QuestsDAO.incrementShareCountByQuestId(questId, em);

            return ok(ResponseUtility.getSuccessMessageForResponse("successfully shared quest"));
        } catch (final Exception ex) {
            Logger.error(format("Error sharing quest with ID [%s]", questId), ex);
            return internalServerError();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result addShareCount() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String questId = form.get("questId");

        if (questId != null) {
            Logger.info("Shareing quest");
            // increment the share count on the quest
            QuestsDAO.incrementShareCountByQuestId(valueOf(questId), em);

            return ok(ResponseUtility.getSuccessMessageForResponse("successfully added to share count"));
        }

        return ok(ResponseUtility.getErrorMessageForResponse("error adding to share count"));
    }

    @Transactional
    @JwtSessionLogin
    public Result urlForSharing(@NotNull final Integer questId) {
        EntityManager em = this.jpaApi.em();
        DynamicForm form = formFactory.form().bindFromRequest();

        String userId = form.get("userId");
        User user = null;

        if (userId != null) {
            user = UserService.getById(valueOf(userId), em);
        }
        final Quests quest = findById(questId, em);
        if (quest != null && userId == null) {
            String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
            return ok(Json.toJson(envUrl + URLUtils.seoFriendlyPublicQuestPath(quest, quest.getUser())));
        } else if (quest != null && user != null) {

            String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
            if (quest.getMode().equals(QuestMode.SUPPORT_ONLY) || quest.getMode().equals(TEAM)) {
                return ok(Json.toJson(envUrl + URLUtils.seoFriendlyPublicQuestPath(quest, quest.getUser())));
            }

            return ok(Json.toJson(envUrl + URLUtils.seoFriendlyPublicQuestPath(quest, user)));

        } else {
            return badRequest();
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result mainFeed() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();
        List<Quests> quests;

        if (user != null) {
            quests = QuestActivityHome.getQuestsNotInProgressForUser(user, null, em).getList(user);

            Logger.info("QUESTS FOR USER = " + Arrays.toString(quests.toArray()));

        } else {
            quests = QuestsDAO.getAllQuestsWithACL(em).getList(null);
        }
        return ok(Json.toJson(quests));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getQuestsCreatedByUser() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final ArrayNode result = Json.newArray();
        jpaApi.withTransaction(em -> {
            final PaymentTransactionDAO dao = new PaymentTransactionDAO(em);
            QuestsDAO.createdBy(user, em).forEach(quest -> {
                quest.getAdmins().forEach(em::detach);
                em.detach(quest.getUser());
                em.detach(quest);
                final JsonNode node = Json.toJson(quest);
                if (node instanceof ObjectNode && quest.getId() != null) {
                    ((ObjectNode) node).put(
                            "timesQuestBacked",
                            dao.getQuestBackingTransactionsCountForQuest(quest.getId())
                    );
                }
                result.add(node);
            });
            return result;
        });
        return ok(result);
    }

    @Transactional
    @JwtSessionLogin()
    public Result getMyQuestsForUser(final @NotNull Integer userId) {
        final EntityManager em = jpaApi.em();
        final StopWatch timer = new StopWatch("Loading my Quests for user with ID " + userId);

        timer.start("Loading current user");

        final User loggedInUser = this.userProvider.getUser(session());
        final User requestedUser = loggedInUser != null && loggedInUser.getId().equals(userId)
                ? loggedInUser
                : UserHome.findById(userId, em);
        if (requestedUser == null) {
            return notFound();
        }

        timer.stop();

        timer.start("Loading Quests of user with ID " + userId);

        final List<QuestDTO> myQuests = questListService.loadMyQuestsForUser(loggedInUser, requestedUser);

        timer.stop();

        timer.start("Converting " + myQuests.size() + " Quests to JSON");

        final JsonNode result = Json.toJson(myQuests);

        timer.stop();

        Logger.info(timer.prettyPrint());

        return ok(result);
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin(required = true)
    public Result getQuestsActiveForUser() {
        User user = this.userProvider.getUser(session());
        Logger.info(format("fetching active quests for user [%s]", user != null ? user.getId() : null));
        EntityManager em = this.jpaApi.em();

        List<Quests> quests = QuestActivityHome.getInProgressQuestsForUser(user, em).getList(user);

        return ok(Json.toJson(quests));
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsActiveForUserByUserId() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String userId = form.get("userId");
        User loggedInUser = this.userProvider.getUser(session());

        if (userId != null && !userId.isEmpty()) {
            User user = UserHome.findById(Integer.parseInt(userId), em);
            List<Quests> quests = QuestActivityHome.getInProgressQuestsForUser(user, em).getList(loggedInUser);
            return ok(Json.toJson(quests));
        }

        return badRequest();
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin()
    public Result getQuestsActiveLiteForUserByUserId(final @NotNull Integer userId) {
        return getQuestsLiteForUserByUserId(IN_PROGRESS, userId).withHeader(CACHE_CONTROL, "max-age=86400");
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsActiveLiteForUser() {
        return getQuestsLiteForUser(IN_PROGRESS);
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsCompletedLiteForUserByUserId(final @NotNull Integer userId) {
        return getQuestsLiteForUserByUserId(COMPLETE, userId);
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsCompletedLiteForUser() {
        return getQuestsLiteForUser(COMPLETE);
    }

    @Deprecated
    private Result getQuestsLiteForUserByUserId(final QuestActivityStatus status, final Integer userId) {
        final User loggedInUser = this.userProvider.getUser(session());
        final ArrayNode result = Json.newArray();

        Logger.debug(format("getQuestsLiteForUserByUserId - fetching %s quests for user %d by user %d", status.name(), userId, ((loggedInUser != null) ? loggedInUser.getId() : null)));

        jpaApi.withTransaction(em -> {
            final User user = UserHome.findById(userId, em);
            switch (status) {
                case IN_PROGRESS:
                    populateActiveQuestsResponse(user, loggedInUser, result, em);
                    return em;
                case COMPLETE:
                    populateCompleteQuestsResponse(user, loggedInUser, result, em);
                    return em;
                default:
                    return em;
            }
        });
        return ok(result);
    }

    @Deprecated
    private Result getQuestsLiteForUser(final QuestActivityStatus status) {
        final User loggedInUser = this.userProvider.getUser(session());
        StopWatch watch = new StopWatch();
        watch.start();
        Logger.debug(format("fetching [%s] quests for user [%s]", status.name(), loggedInUser != null ? loggedInUser.getId() : null));

        final ArrayNode result = Json.newArray();
        if (loggedInUser == null) {
            return ok(result);
        }

        jpaApi.withTransaction(em -> {
            switch (status) {
                case IN_PROGRESS:
                    populateActiveQuestsResponse(loggedInUser, loggedInUser, result, em);
                    return em;
                case COMPLETE:
                    populateCompleteQuestsResponse(loggedInUser, loggedInUser, result, em);
                    return em;
                default:
                    return em;
            }
        });
        watch.stop();
        Logger.debug(format("fetched [%s] quests for user [%s] in [%s]ms", status.name(), loggedInUser.getId(), watch.getTotalTimeMillis()));
        return ok(result);
    }

    @Deprecated
    private void populateActiveQuestsResponse(final User requestedUser,
                                              final User currentUser,
                                              final ArrayNode result,
                                              final EntityManager em) {
        final Map<Integer, QuestListDetailDTO> progressMap = QuestTasksDAO.getQuestsCompletionPercentage(requestedUser, em)
                .stream()
                .map(dto -> QuestMode.PACE_YOURSELF.equals(dto.activityMode)
                        ? new QuestListDetailDTO(dto.questId, dto.activityMode, dto.completeTasks, dto.totalTasks, dto.creatorCompleteTasks, dto.creatorTotalTasks)
                        : new QuestListDetailDTO(dto.questId, dto.activityMode, dto.creatorCompleteTasks, dto.creatorTotalTasks, dto.creatorCompleteTasks, dto.creatorTotalTasks))
                .collect(toMap(dto -> dto.questId, dto -> dto));
        final Map<Integer, QuestActivityDTO> repeatableMap = QuestActivityHome.getRepeatableInfoForDoer(requestedUser, em)
                .stream()
                .collect(toMap(dto -> dto.questId, dto -> dto));
        final Set<Integer> followedSet = new HashSet<>(new QuestUserFlagDAO(em).getQuestsBeingFollowedForUser(requestedUser));
        final Set<Integer> starredSet = new HashSet<>(new QuestUserFlagDAO(em).retrieveStarredQuests(requestedUser));
        final List<QuestTeam> requestedUserTeams = new QuestTeamDAO(em).listActiveTeamsForUser(requestedUser);
        final List<FundraisingLinkDTO> requestedUserFundraisers = fundraisingService.getUserFundraisingLinks(requestedUser.getId());
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        final Brand company = UserHome.getCompanyForUser(currentUser,jpaApi);
        final FundraisingLinkDAO fundraisingDao = new FundraisingLinkDAO(jpaApi);
        // Grab quests for desired user, filters the ACL for current user
        QuestActivityHome.getInProgressQuestsForUser(requestedUser, em).getList(currentUser)
                .stream()
                // Sets if quest is editable by current user
                .map(quest -> QuestListDTO.toDTO(quest, currentUser))
                .filter(Objects::nonNull)
                .map(dto -> dto.withDetail(progressMap.get(dto.id))
                        .withUserDoing(true))
                .map(dto -> dto.withUserFlags(QuestUserFlagsDTO.builder()
                        .withFollowing(followedSet.contains(dto.id))
                        .withSaved(false)
                        .withStarred(starredSet.contains(dto.id))
                        .build()))
                .map(dto -> dto.withActivityData(repeatableMap.get(dto.id)))

                // Note: this will strip requested user off of the quest if needed
                // If we have an anonymous user, assume that we want the requested User's version of the link
                .map(dto -> dto.withSEOSlugs(mapSeoSlugs(dto, ((currentUser == null) ? requestedUser : currentUser), requestedUserTeams, requestedUserFundraisers, envUrl)))
                // check if it was ever fundraised by checking the link
                .map(dto -> dto.withIsQuestFundraiser(fundraisingDao.existsWithQuestAndFundraiserUser(dto, requestedUser)))
                .sorted()
                .map(Json::toJson)
                .map(response-> ResponseUtility.getBrandForUser((ObjectNode)response,company))
                .forEach(result::add);
    }

    @Deprecated
    private void populateCompleteQuestsResponse(final User requestedUser,
                                                final User currentUser,
                                                final ArrayNode result,
                                                final EntityManager em) {
        final Map<Integer, QuestActivityDTO> repeatableMap = QuestActivityHome.getRepeatableInfoForDoer(requestedUser, em)
                .stream()
                .collect(toMap(dto -> dto.questId, dto -> dto));
        final List<QuestTeam> requestedUserTeams = new QuestTeamDAO(em).listActiveTeamsForUser(requestedUser);
        final List<FundraisingLinkDTO> requestedUserFundraisers = fundraisingService.getUserFundraisingLinks(requestedUser.getId());
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        final FundraisingLinkDAO fundraisingDao = new FundraisingLinkDAO(jpaApi);
        final Brand company = UserHome.getCompanyForUser(currentUser,jpaApi);
        QuestActivityHome.getCompletedQuestsForUser(requestedUser, em).getList(currentUser)
                .stream()
                .map(quest -> Optional.ofNullable(QuestListDTO.toDTO(quest, currentUser)).map(dto -> dto.withUserDoing(false)).orElse(null))
                .filter(Objects::nonNull)
                .map(dto -> dto.withActivityData(repeatableMap.get(dto.id)))
                .map(dto -> dto.withSEOSlugs(mapSeoSlugs(dto, ((currentUser == null) ? requestedUser : currentUser), requestedUserTeams, requestedUserFundraisers, envUrl)))
                // check if it was ever fundraised by checking the link
                .map(dto -> dto.withIsQuestFundraiser(fundraisingDao.existsWithQuestAndFundraiserUser(dto, requestedUser)))
                .sorted()
                .map(Json::toJson)
                .map(response-> ResponseUtility.getBrandForUser((ObjectNode)response,company))
                .forEach(result::add);
    }

    @Deprecated
    private QuestSEOSlugs mapSeoSlugs(final QuestDTO quest,
                                      final User currentUser,
                                      final List<QuestTeam> requestedUserTeams,
                                      final List<FundraisingLinkDTO> requestedUserFundraisers,
                                      final String environmentUrl) {
        final Optional<FundraisingLinkDTO> requestedUserFundraiser = requestedUserFundraisers.stream()
                .filter(link -> link.quest.getId().equals(quest.id))
                .findFirst();
        if (requestedUserFundraiser.isPresent()) {
            return publicQuestSEOSlugs(quest, currentUser, environmentUrl);
        } else if (TEAM.getKey().equals(quest.getActivityMode())) {
            final Function<Integer, QuestTeam> teamMapper = (questId) -> requestedUserTeams.stream()
                    .filter(team -> team.getQuest().getId().equals(questId))
                    .findFirst()
                    .orElse(null);
            final QuestTeam team = teamMapper.apply(quest.id);
            if (team == null || team.isDefaultTeam()) {
                return publicQuestSEOSlugs(quest, quest.user, environmentUrl);
            } else {
                return publicTeamQuestSEOSlugs(team, environmentUrl);
            }
        } else if (SUPPORT_ONLY.getKey().equals(quest.getActivityMode())) {
            return publicQuestSEOSlugs(quest, quest.user, environmentUrl);
        } else if (PACE_YOURSELF.getKey().equals(quest.getActivityMode())) {
            // Previous code here was overwriting the quest.user with the currentUser for PACE_YOURSELF case.  This caused the quest urls to get changed to
            // the current user's version, regardless if they had started it or not.  I think what we want is to check the activity table and only overwrite
            // the link if the currentUser has done something with this quest before.
            UserSEO slugUser = quest.user;
            if (currentUser != null) {
                try (Connection c = dbRo.getConnection()) {
                    if (QuestActivityHome.doesQuestActivityExistForUserIdAndQuestId(c, (long) quest.id, (long) currentUser.getId())) {
                        slugUser = currentUser;
                    }
                } catch (SQLException e) {
                    Logger.error("mapSeoSlugs - can't fetch quest activity", e);
                }
            }
            return publicQuestSEOSlugs(quest, slugUser, environmentUrl);
        } else {
            return publicQuestSEOSlugs(quest, quest.user, environmentUrl);
        }
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsSavedForUser() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();
        ArrayNode result = Json.newArray();

        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        QuestSavedDAO.getSavedQuestsForUser(user, em).getList(user)
                .stream()
                .map(quest -> QuestListDTO.toDTO(quest, user))
                .filter(Objects::nonNull)
                .map(dto -> dto.withSEOSlugs(mapSeoSlugs(dto, user, emptyList(), emptyList(), envUrl)))
                .sorted()
                .map(Json::toJson)
                .forEach(result::add);

        return ok(result);
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsSavedByUserId() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String userId = form.get("userId");
        User loggedInUser = this.userProvider.getUser(session());

        if (userId != null && !userId.isEmpty()) {
            User user = UserHome.findById(Integer.parseInt(userId), em);
            List<Quests> quests = QuestSavedDAO.getSavedQuestsForUser(user, em).getList(loggedInUser);

            if (quests != null && !quests.isEmpty()) {
                return ok(Json.toJson(quests));
            } else {
                ObjectNode wrapper = Json.newObject();
                ObjectNode msg = Json.newObject();
                msg.put("message", "no saved quests for user id = " + userId);
                wrapper.set("error", msg);
                return ok(wrapper);
            }
        }
        return ok();
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsCompletedForUser() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();
        List<Quests> quests = QuestActivityHome.getCompletedQuestsForUser(user, em).getList(user);
        return ok(Json.toJson(quests));
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getQuestsCompletedByUserId() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String userId = form.get("userId");
        User loggedInUser = this.userProvider.getUser(session());

        User user = UserHome.findById(Integer.parseInt(userId), em);

        List<Quests> quests = QuestActivityHome.getCompletedQuestsForUser(user, em).getList(loggedInUser);
        return ok(Json.toJson(quests));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getProfileStatsForUser() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final UserProfileStatsDTO result = this.jpaApi.withTransaction(em -> {
            final UserProfileStatsDTO stats = new UserProfileStatsDTO();
            populateQuestsStatsForUser(stats, user, em);
            stats.backed = new PaymentTransactionDAO(em).getOutgoingTransactionsCount(user.getId());
            stats.friends = getFriendsCount(user, em);
            return stats;
        });
        return ok(Json.toJson(result));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getQuestsStatsForUser() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final UserQuestsStatsDTO result = this.jpaApi.withTransaction(em -> {
            final UserQuestsStatsDTO stats = new UserQuestsStatsDTO();
            populateQuestsStatsForUser(stats, user, em);
            return stats;
        });
        return ok(Json.toJson(result));
    }

    @Transactional
    @JwtSessionLogin
    public Result getQuestSearchResult(final Integer questId) {
        final EntityManager em = this.jpaApi.em();
        final Quests quest = em.find(Quests.class, questId);
        if (quest == null) {
            return notFound();
        }
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        final QuestLiteDTO dto = QuestLiteDTO.toDTO(quest).withSeoSlugs(publicQuestSEOSlugs(quest, quest.getUser(), envUrl));
        return ok(Json.toJson(dto));
    }

    @Transactional
    @JwtSessionLogin
    public Result getRecommendedQuests() {
        final String limitParam = request().getQueryString("limit");
        final int limit;
        if (isNumeric(limitParam)) {
            limit = Integer.parseInt(limitParam);
        } else {
            limit = config.getInt("application.landing.carouselSize");
        }
        return ok(Json.toJson(questService.getRecommendedQuests(limit)));
    }

    private void populateQuestsStatsForUser(final UserQuestsStatsDTO stats, final User user, final EntityManager em) {
        stats.active = QuestActivityHome.getInProgressQuestsForUser(user, em).getCount(user);
        stats.saved = QuestSavedDAO.getSavedQuestsForUser(user, em).getCount(user);
        stats.completed = QuestActivityHome.getCompletedQuestsForUser(user, em).getCount(user);
    }

    private int getFriendsCount(final @NotNull User user, final EntityManager em) {
        final List<Integer> friendIds = UserRelationshipDAO.getCurrentFriendsByUserId(user.getId(), em);
        return isEmpty(friendIds) ? 0 : friendIds.size();
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getActiveQuestCountForUser() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();
        long quests = QuestActivityHome.getInProgressQuestsForUser(user, em).getCount(user);
        return ok(Json.toJson(quests));
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getCompleteQuestCountForUser() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();
        long quests = QuestActivityHome.getCompletedQuestsForUser(user, em).getCount(user);
        return ok(Json.toJson(quests));
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getSavedQuestCountForUser() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();
        long count = QuestSavedDAO.getSavedQuestsForUser(user, em).getCount(user);
        return ok(Json.toJson(count));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result updateTeamInfo(final Long teamId) {
        final Form<QuestTeamInfoForm> formBinding = formFactory.form(QuestTeamInfoForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final QuestTeamInfoForm form = formBinding.get();
        if (!TeamAction.Update.equals(form.getQuestTeamAction())) {
            return badRequest();
        }
        final QuestTeamDAO dao = new QuestTeamDAO(jpaApi.em());
        final QuestTeam team = dao.load(teamId, QuestTeam.class);
        if (team == null) {
            return notFound();
        }

        // get the owner instead of the logged in user. as admin and owner can change the name
        // final User user = this.userProvider.getUser(session());
        final User user = team.getCreator();
//        if (user == null) {
//            return unauthorized();
//        }
//        if (team.getCreator().getId().equals(user.getId())) {
            final String newTeamName = form.getQuestTeamName();
            final String oldTeamName = team.getName();
            team.setName(newTeamName);
            team.setLogoUrl(form.getQuestTeamLogoUrl());
            team.setTeamCoverUrl(form.getQuestTeamCoverUrl());
            final QuestTeam result = dao.updateTeam(team);

            Quests quest = team.getQuest();
            // Rename campaign_name from fundraising_link table in case of team rename
            if(quest.isFundraising() && !oldTeamName.equals(newTeamName)){
                Logger.info("Renaming fundraising_link campaign");
                final FundraisingLinkDAO fundraisingDao = new FundraisingLinkDAO(jpaApi);
                FundraisingLink fundraisingLink = fundraisingDao.getFundraisingLink(quest, user);
                final String newQuestName = quest.isMultiSellerEnabled() ? format("%s raising for %s", newTeamName, quest.getTitle()) : newTeamName;
                fundraisingDao.updateFundraisingForQuest(fundraisingLink, fundraisingLink.targetAmountCents, newQuestName, fundraisingLink.coverImageUrl);
            }

            return ok(Json.toJson(QuestTeamDTO.from(result)));
//        } else {
//            return forbidden();
//        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result doQuest() {
        final Form<QuestStartForm> formBinding = formFactory.form(QuestStartForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final QuestStartForm form = formBinding.get();

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        final EntityManager em = this.jpaApi.em();
        final User referrer = form.getReferrerId() == null ? null : em.find(User.class, form.getReferrerId());

        try (Connection c = db.getConnection()) {
            final Quests quest = findById(form.getQuestId(), em);
            if (quest == null) {
                return notFound();
            }

            if (quest.isRealtime() && (QuestActivityHome.getInProgressRealtimeQuestForUser(user, em) != null)) {
                final Messages messages = this.messages.preferred(request());
                return badRequest(messages.at("error.realtime-exist"));
            }

            final QuestMode questMode = isBlank(form.getQuestMode()) ? null : QuestMode.fromKey(form.getQuestMode());

            final boolean started = questService.startQuest(c, quest, referrer, user, questMode, form, form.getPoint());
            if (started) {
                // send notification to quest creator that someone is doing the quest
                final List<Integer> usersDoing = notificationService.getUserIdsSubscribedToQuest(quest);
                AsyncUtils.processIdsAsync(config, usersDoing, userDoing -> notificationService.addQuestNotification(
                        userDoing,
                        QUEST_STARTED,
                        user.getId(),
                        quest.getId()));
            }
            return ok(BooleanNode.valueOf(started));
        } catch (Exception ex) {
            Logger.error("Application.doQuest() Error attempting to sign up for quest => " + ex, ex);
            return internalServerError();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result completeRealtimeQuest(Integer questId) {
        final Form<RealtimeQuestForm> formBinding = formFactory.form(RealtimeQuestForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }

        final RealtimeQuestForm form = formBinding.get();

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        final EntityManager em = this.jpaApi.em();

        try {
            final Quests quest = findById(questId, em);
            final Quests usersRealtimeQuest = QuestActivityHome.getInProgressRealtimeQuestForUser(user, em);
            if (quest == null || usersRealtimeQuest == null || !questId.equals(usersRealtimeQuest.getId())) {
                return badRequest();
            }

            Integer imageId;
            if (form.getImage() != null) {
                //final List<S3File> questImageList = imageService.getFileFromRequest(request(), "image", ImageType.QUEST_IMAGE);
                //if (questImageList == null) {
                  //  return internalServerError("Cannot process uploaded image for quest: " + questId);
               // }

                //final S3FileHome s3FileHome = new S3FileHome(config);
                //s3FileHome.saveUserMedia(questImageList, em);
               // final URL questImageUrl = s3FileHome.getUrl(questImageList.get(0), true);

                final QuestImage questImage = QuestImageDAO.addNewQuestImage(user.getId(), questId, form.getImage() , trimToNull(form.getComment()), em);
                Logger.debug(format("successfully added a new Quest photo with ID [%s]", questImage.getId()));

                imageId = questImage.getId();
            } else {
                imageId = null;
            }

            QuestActionPointForm actionPoint = form.getCoordinates();
            Double lat;
            Double lon;
            if (actionPoint == null) {
                lat = null;
                lon = null;
            } else {
                Float _lat = actionPoint.getLatitude();
                Float _lon = actionPoint.getLongitude();
                lat = (_lat == null) ? null : _lat.doubleValue(); 
                lon = (_lon == null) ? null : _lon.doubleValue();
            }

            if (isNotEmpty(form.getAttributes())) {
                AttributesDAO attDao = AttributesDAO.getInstance();

                // Save realtime quest results
                for (AttributeValueForm attributeValueForm : form.getAttributes()) {
                    if (attributeValueForm != null) {
                        Long attributeId = attributeValueForm.getAttributeId();
                        String attributeValue = attributeValueForm.getAttributeValue();

                        if ((attributeId != null) && isNotBlank(attributeValue)) {
                            String attributeTag = attributeValueForm.getAttributeTag();
                            String username = user.getUserName();
                            long userId = user.getId();

                            try (Connection c = db.getConnection(false)) {
                                if (attDao.insertAttributeValue(c, attributeId, attributeValue, userId, attributeTag)) {
                                    Logger.debug("completeRealtimeQuest - user " + userId + " saved attribute " + attributeId + " with value: " + attributeValue +
                                            ", tag: " + attributeTag);
                                   
                                    // TOOD: the Leaderboard features have score baked into them as a Double. From the client-server transport to the
                                    // data layer.  It might behove us at some point to convert score to a string

                                    Double value;

                                    // Is the attribute value of the form hh:mm:ss?
                                    if (attributeValue.contains(":")) {
                                        String[] parts = attributeValue.split(":");
                                        value = (double) (Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]));
                                    } else {
                                        value = Double.parseDouble(attributeValue);
                                    }

                                    // If the quest has a leaderboard, update that too
                                    try (Connection cRo = dbRo.getConnection()) {
                                        Attribute attribute = attDao.getAttributeById(c, attributeId);

                                        // TODO: post realtime activity item to feed
                                        activityService.questRealtime(questId, username, /* teamId */ null, lat, lon, value, attribute.getUnit(), attributeTag);

                                        List<QuestLeaderboard> leaderboardSlugs = LeaderboardDAO.getLeaderboardSlugForRealtimeAttribute(cRo, questId, attributeId, attributeTag);
                                        if (!leaderboardSlugs.isEmpty()) {
                                            // We found a leaderboard mapping.  Find the user.
                                            Integer lmId = LeaderboardDAO.getLeaderboardMemberId(cRo, questId, userId);
                                            if (lmId != null) {
                                                Logger.debug("completeRealtimeQuest - updating leaderboard for user " + userId + " and quest " + questId);

                                                for (QuestLeaderboard slug : leaderboardSlugs) {
                                                    double score = value.doubleValue();
                                                    
                                                    // Find any existing score record, then update it with the attribute value (create if not exists)
                                                    Double conversion = slug.getConversion();
                                                    score = ((conversion == null) ? score : (score * conversion));
                                                    LeaderboardDAO.updateLeaderboardScore(c, lmId, slug.getLeaderboardSlug(), score, true);
                                                }
                                            }
                                        } else {
                                            Logger.debug("completeRealtimeQuest - no leaderboard mapping for realtime attribute " + attributeId + " and tag " + attributeTag);
                                        }
                                    }
                                }
                                c.commit();
                            }
                        }
                    }
                }
            }

            if (quest.getQuestMapViewId() != null && isNotBlank(quest.getDistanceAttributeName())) {
                final Optional<User> principalUser = QuestUserUtils.getPrincipleQuestUser(quest, user, em);
                if (principalUser.isPresent()) {
                    completeQuestTask(quest, principalUser.get(), actionPoint);
                } else {
                    Logger.warn(format("Not checking tasks for Quest %s with no principal user", questId));
                }
            } else {
                Logger.info(format("Not checking tasks for Quest %s with no map or distance attribute configured", questId));
            }

            try (Connection c = db.getConnection(); Connection cRo = dbRo.getConnection()) {
                if (isNotBlank(form.getComment())) {
                    QuestComments newComment = QuestCommentsDAO.addCommentsToQuestByUserIdAndQuestId(user.getId(), questId, null, form.getComment(), em);

                    if (imageId != null) {
                        QuestCommentsDAO.setCommentImage(c, newComment.getId(), imageId);
                    }
                }

                boolean completed = questService.completeQuest(c, cRo, quest, user, false, actionPoint, false);
                if (completed) {
                    return ok();
                }
            }
            return internalServerError("Cannot complete quest " + questId);
        } catch (Exception e) {
            Logger.error("Error attempting to start realtime Quest :: Application.doRealtimeQuest() => " + e, e);
            return internalServerError();
        }
    }

    private void completeQuestTask(final Quests quest, final User user, final QuestActionPointForm togglePoint) {
        final EntityManager em = jpaApi.em();
        final Integer questId = quest.getId();
        final QuestMapRouteWaypointDAO questMapRouteWaypointDAO = new QuestMapRouteWaypointDAO(em);
        final List<QuestMapRouteWaypoint> questMapRouteWaypoints = questMapRouteWaypointDAO.findAllQuestMapRouteWaypointsByQuestId(questId);

        Logger.info(format("Retrieved %s waypoints for Quest %s", questMapRouteWaypoints.size(), questId));

        if (validateDistanceWaypoint(questMapRouteWaypoints, questId)) {
            final List<QuestTasks> questTasks = QuestTasksDAO.getQuestTasksByQuestIdAndWaypointIsNotNull(questId, user.getId(), em);

            Logger.info(format("Retrieved %s tasks for Quest  %s and User %s", questTasks.size(), questId, user.getId()));

            // Switched to leaderboard score
            //
            // final Double distance = AttributesDAO.getDistanceAttributeValueByQuestId(questId, user.getId(), distanceAttributeName, em);
            //
            // Note: including hidden users here since this is part of a write operation
            //
            final List<LeaderboardScoreDTO> scores = leaderboardService.getLeaderboardLocal(quest, quest.getDistanceAttributeName(), true, false, true, true);
            final Double distance = scores.stream()
                    .filter(score -> user.getId().equals(score.getUserId()))
                    .findFirst()
                    .map(LeaderboardScoreDTO::getScore)
                    .orElse(null);

            Logger.info(format("Total distance for Quest %s and User %s is %s", questId, user.getId(), distance));

            if (distance != null && distance > 0) {
                mappingQuestTaskByWaypoints(questMapRouteWaypoints, questTasks).forEach(pair -> {
                    final Integer waypointDistance = pair.getValue().getDistance();

                    Logger.info(format("Checking waypoint %s distance of %s", pair.getValue().getId(), waypointDistance));

                    if (waypointDistance != null && distance >= waypointDistance) {
                        Logger.info(format("Checking task %s", pair.getKey().getId()));

                        milestoneService.checkMilestone(pair.getKey(), user, true, togglePoint);
                    }
                });
            }
        }
    }

    private List<Pair<QuestTasks, QuestMapRouteWaypoint>> mappingQuestTaskByWaypoints(final List<QuestMapRouteWaypoint> questMapRouteWaypoints,
                                                                                      final List<QuestTasks> questTasks) {
       final Map<Long, QuestMapRouteWaypoint> mappingWaypointsByIds = questMapRouteWaypoints.stream().collect(toMap(l -> l.id, k -> k));

        Logger.info(format("Mapped %s waypoints", mappingWaypointsByIds.size()));

       return questTasks.stream()
               .filter(task -> mappingWaypointsByIds.containsKey(task.getQuestMapRouteWaypointId()))
               .map(task -> Pair.of(task, mappingWaypointsByIds.get(task.getQuestMapRouteWaypointId())))
               .sorted(Comparator.comparing(pair -> pair.getValue().getSequence()))
               .collect(toList());
    }

    private boolean validateDistanceWaypoint(final List<QuestMapRouteWaypoint> questMapRouteWaypoints, final Integer questId) {
        boolean isValidate = true;
        for (QuestMapRouteWaypoint waypoint : questMapRouteWaypoints) {
            if (waypoint.getDistance() == null) {
                Logger.warn("Distance waypoint is null, waypoint_id = " + waypoint.id);
                isValidate = false;
            }
        }

        if (isValidate) {
            int sumWaypointsDistance = questMapRouteWaypoints.stream()
                    .map(QuestMapRouteWaypoint::getDistance)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);
            if (sumWaypointsDistance == 0) {
                isValidate = false;
                Logger.warn("Total waypoint distance is 0, waypoint_id = " + questId);
            }
        }
        return isValidate;
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result linkQuestViaMilestone(final Integer megaQuestId, final Integer linkedQuestId) {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = jpaApi.em();
        final Quests megaQuest = findById(megaQuestId, em);
        final Quests linkedQuest = findById(linkedQuestId, em);
        if (megaQuest == null || linkedQuest == null) {
            return notFound();
        }
        QuestTasks linkMilestone;
        try (Connection cRo = dbRo.getConnection()) {
            linkMilestone = questService.addMilestoneToMegaQuest(cRo, megaQuest, linkedQuest, user);
        } catch (SQLException e) {
            Logger.error("linkQuestViaMilestone - can't add milestone to quest", e);
            linkMilestone = null;
        }
        if (linkMilestone != null) {
            final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
            return ok(Json.toJson(milestoneService.convertToDto(linkMilestone, envUrl, linkPreviewService)));
        } else {
            return notAcceptable();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result completeQuest() {
        final Form<QuestCompletionForm> formBinding = formFactory.form(QuestCompletionForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final QuestCompletionForm form = formBinding.get();
        final EntityManager em = this.jpaApi.em();
        final Integer questId = form.getQuestId();
        final boolean completeMilestones = BooleanUtils.toBoolean(form.getCompleteMilestones());

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final Quests quest = findById(questId, em);
        if (quest == null) {
            return notFound();
        }

        try (Connection c = db.getConnection(); Connection cRo = dbRo.getConnection()) {
            final boolean completed = questService.completeQuest(c, cRo, quest, user, completeMilestones, form.getPoint());
            if (!completed) {
                return badRequest();
            }
        } catch (SQLException e) {
            Logger.error("completeQuest - error", e);
        }

        final PaymentTransactionDAO transactionDao = new PaymentTransactionDAO(em);
        final List<RecurringQuestBackingTransaction> subscriptions = transactionDao
                .getSubscriptionsForQuestAndBeneficiary(quest.getId(), user.getId());
        if (isNotEmpty(subscriptions)) {
            subscriptions.forEach(subscriptionTransaction -> {
                final String id = subscriptionTransaction.stripeTransactionId;
                final StripeAccount beneficiary = subscriptionTransaction.to;
                if (beneficiary != null) {
                    final Subscription subscription = stripeService.cancelQuestBackingSubscription(id, beneficiary);
                    if (subscription != null) {
                        subscriptionTransaction.valid = false;
                        transactionDao.save(subscriptionTransaction, RecurringQuestBackingTransaction.class);
                    }
                }
            });
        }

        //on success send notification to quest creator that user has completed the quest
        final List<Integer> usersDoing = notificationService.getUserIdsSubscribedToQuest(quest);
        AsyncUtils.processIdsAsync(config, usersDoing, userDoing -> notificationService.addQuestNotification(
                userDoing,
                QUEST_ACHIEVED,
                user.getId(),
                quest.getId()
        ));

        return ok();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result repeatQuest(final @NotNull Integer questId) {
        final Form<QuestRepeatForm> formBinding = formFactory.form(QuestRepeatForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final QuestRepeatForm form = formBinding.get();
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findQuestForDoer(questId, user, em);
        if (quest == null) {
            return notFound();
        }
        if (quest.isRealtime() && QuestActivityHome.getInProgressRealtimeQuestForUser(user, em) != null) {
            final Messages messages = this.messages.preferred(request());
            return badRequest(messages.at("error.realtime-exist"));
        }

        Logger.info(format("About to restart (repeat) Quest [%s] for user [%s] with tasks reset [%s]", quest.getId(), user.getId(), isNotFalse(form.getResetTasks())));

        final boolean repeated = questService.repeatQuest(quest, user, form.getPoint(), isNotFalse(form.getResetTasks()));
        if (repeated) {
            return ok();
        } else {
            return badRequest();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result copyQuest(final @NotNull Integer questId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(questId, em);
        if (quest == null) {
            return notFound();
        } else if (quest.isCopyProtectionEnabled() && !quest.getCreatedBy().equals(user.getId())) {
            return forbidden();
        }

        final Integer id = jpaApi.withTransaction(tem -> {
            final Quests copy = questService.copyQuestForUser(quest, user, false);

            QuestSavedDAO.saveQuestForUser(copy, user, tem);

            return copy.getId();
        });

        return ok(IntNode.valueOf(id));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result saveQuest() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String questId = form.get("questId");
        Logger.info("questId to be saved = " + questId);
        User user = this.userProvider.getUser(session());

        try {
            if (user != null && questId != null && !questId.isEmpty()) {
                Quests quest = findById(Integer.parseInt(questId), em);

                boolean isQuestSaved = QuestSavedDAO.doesQuestSavedExistForUser(Integer.parseInt(questId), user.getId(), em);

                boolean isQuestActive;
                try (Connection c = dbRo.getConnection()) {
                    isQuestActive = QuestActivityHome.doesQuestActivityExistForUserIdAndQuestId(c, Long.parseLong(questId), (long) user.getId());
                }

                if (!isQuestSaved && !isQuestActive) {
                    QuestSavedDAO.saveQuestForUser(quest, user, em);

                    // on success add event history
                    Logger.info("Saving event history now");
                    QuestEventHistoryDAO.addEventHistory(quest.getId(), user.getId(), QUEST_SAVE, null, em);

                    //on success create notification for quest creator
                    notificationService.addQuestNotification(
                            quest.getUser().getId(),
                            QUEST_SAVED,
                            user.getId(),
                            quest.getId()
                    );

                    // clear cache so it is removed from feed
                    cache.remove("user.quests");

                    ObjectNode wrapper = Json.newObject();
                    ObjectNode msg = Json.newObject();
                    msg.put("message", "successfully saved quest for user");
                    wrapper.set("success", msg);
                    return ok(wrapper);
                } else {
                    ObjectNode wrapper = Json.newObject();
                    ObjectNode msg = Json.newObject();
                    msg.put("message", "You already have this Quest saved.");
                    wrapper.set("error", msg);
                    return ok(wrapper);
                }
            }
        } catch (Exception ex) {
            Logger.error("Application :: saveQuest : error saving quest for user => " + ex, ex);
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "error saving quest for user");
            wrapper.set("error", msg);
            return ok(wrapper);
        }

        ObjectNode wrapper = Json.newObject();
        ObjectNode msg = Json.newObject();
        msg.put("message", "error saving quest for user");
        wrapper.set("error", msg);
        return ok(wrapper);

    }

    @Transactional
    @JwtSessionLogin
    public Result getCurrentFriends() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        if (user != null) {
            List<Integer> currentFriendRelationships = UserRelationshipDAO.getCurrentFriendsByUserId(user.getId(), em);
            List<User> currentFriends = UserHome.getUsersByIds(currentFriendRelationships, em);
            Logger.info("found [{}] current friends for user [{}]", currentFriends.size(), user.getId());
            return ok(Json.toJson(UserDTO.listToDTO(currentFriends)));
        }
        return ok(Json.newArray());
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getPendingFriendsToAccept() {
        EntityManager em = this.jpaApi.em();
        List<User> pendingFriendRequestUsersToAccept = null;
        List<Integer> pendingFriendRequestNeedsActionIds = new ArrayList<>();

        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        Logger.info("user.getId = " + user.getId());
        List<UserRelationship> pendingFriendRequestsNeedsAction = UserRelationshipDAO.getPendingFriendRequestsByUserIdNeedingAction(user.getId(), em);
        if (!pendingFriendRequestsNeedsAction.isEmpty()) {
            for (UserRelationship pendingFriendNeedsAction : pendingFriendRequestsNeedsAction) {
                if (pendingFriendNeedsAction.getUserOneId() != user.getId()) {
                    pendingFriendRequestNeedsActionIds.add(pendingFriendNeedsAction.getUserOneId());
                } else {
                    pendingFriendRequestNeedsActionIds.add(pendingFriendNeedsAction.getUserTwoId());
                }
            }
        }

        if (!pendingFriendRequestNeedsActionIds.isEmpty()) {
            pendingFriendRequestUsersToAccept = UserHome.getUsersByIds(pendingFriendRequestNeedsActionIds, em);
        }

        if (pendingFriendRequestUsersToAccept != null && !pendingFriendRequestUsersToAccept.isEmpty()) {
            return ok(Json.toJson(UserDTO.listToDTO(pendingFriendRequestUsersToAccept)));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "No pending friends for user");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getPendingFriendsNoAction() {
        EntityManager em = this.jpaApi.em();
        List<User> pendingFriendRequestUsersNoAction = null;
        List<Integer> pendingFriendRequestNoActionIds = new ArrayList<>();

        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        List<UserRelationship> pendingFriendRequestsNoAction = UserRelationshipDAO.getPendingFriendRequestsByUserIdNoAction(user.getId(), em);
        if (!pendingFriendRequestsNoAction.isEmpty()) {
            for (UserRelationship pendingFriendNoAction : pendingFriendRequestsNoAction) {
                if (pendingFriendNoAction.getUserOneId() != user.getId()) {
                    pendingFriendRequestNoActionIds.add(pendingFriendNoAction.getUserOneId());
                } else {
                    pendingFriendRequestNoActionIds.add(pendingFriendNoAction.getUserTwoId());
                }
            }
        }

        if (!pendingFriendRequestNoActionIds.isEmpty()) {
            pendingFriendRequestUsersNoAction = UserHome.getUsersByIds(pendingFriendRequestNoActionIds, em);
        }

        if (pendingFriendRequestUsersNoAction != null && !pendingFriendRequestUsersNoAction.isEmpty()) {
            return ok(Json.toJson(UserDTO.listToDTO(pendingFriendRequestUsersNoAction)));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "No pending friends for user");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    public Result getCurrentFriendsByUserId() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String userId = form.get("userId");
        Logger.info(format("fetching current friends for userId [%s]", userId));
        if (userId != null && !userId.isEmpty()) {
            User user = UserHome.findById(Integer.parseInt(userId), em);

            List<Integer> currentFriendRelationships = UserRelationshipDAO.getCurrentFriendsByUserId(user.getId(), em);
            if (!currentFriendRelationships.isEmpty()) {
                List<User> currentFriends = UserHome.getUsersByIds(currentFriendRelationships, em);
                return ok(Json.toJson(UserDTO.listToDTO(currentFriends)));
            }
        }
        return noContent();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result sendFriendRequest() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String friendId = form.get("friendId");
        Integer friendshipStatus = null;
        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        //Check if they are friends already
        try {
            friendshipStatus = UserRelationshipDAO.checkForFriendshipStatus(user.getId(), valueOf(friendId), em);
        } catch (NoResultException nre) {//Do nothing, we are jut checking if they are friends or not.

        }

        Logger.info("Friendship status = " + friendshipStatus);
        if (friendId != null && friendshipStatus == null) {
            UserRelationshipDAO.addFriendsByUserId(user.getId(), friendId, em);

            //get friend information for email notification
            User friend = UserService.getById(valueOf(friendId), em);
            String friendName = friend.getFirstName();
            Brand friendCompany = UserHome.getCompanyForUser(friend, this.jpaApi);
            if (friendCompany != null) {
                friendName = friendCompany.getName();
            }

            String userName = user.getFirstName() + " " + user.getLastName();
            Brand company = UserHome.getCompanyForUser(user, this.jpaApi);
            if (company != null) {
                userName = company.getName();
            }
            // send a friend request notification
            AmazonSESService.sendFriendRequestEmail(
                    request(),
                    config.getString("aws.ses.username"),
                    config.getString("aws.ses.password"),
                    userName,
                    friend.getEmail(),
                    friendName
            );
            notificationService.addUserNotification(
                    friend.getId(),
                    NotificationType.FRIEND_REQUEST,
                    user.getId()
            );
        }

        return ok(ResponseUtility.getSuccessMessageForResponse("successfully added friend"));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result checkFriendStatus(final Integer friendId) {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        if (user != null) {
            final Integer status = UserRelationshipDAO.checkForFriendshipStatus(user.getId(), friendId, em);
            if (status != null) {
                return ok(Json.toJson(status));
            } else {
                return ok();
            }
        } else {
            return unauthorized();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result acceptFriendRequest() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String friendId = form.get("friendId");

        User user = this.userProvider.getUser(session());

        if (user != null && friendId != null && !friendId.isEmpty()) {
            UserRelationshipDAO.updateUserRelationshipStatus(user.getId(), valueOf(friendId), UserRelationshipStatus.ACCEPTED, em);

            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "successfullt added friend");
            wrapper.set("success", msg);
            return ok(wrapper);
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "error added friend");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result declineFriendRequest() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        final DynamicForm form = formFactory.form().bindFromRequest();
        final EntityManager em = this.jpaApi.em();
        final String friendId = form.get("friendId");
        if (isBlank(friendId) || !isNumeric(friendId)) {
            return badRequest();
        }

        final UserRelationship relationship = UserRelationshipDAO.findAnyUsersRelationship(user.getId(), valueOf(friendId), em);
        if (relationship == null) {
            return notFound();
        }

        if (user.getId().equals(relationship.getActionUserId())) {
            UserRelationshipDAO.remove(relationship, em);
        } else {
            UserRelationshipDAO.updateUserRelationshipStatus(relationship, UserRelationshipStatus.DECLINED, em);
        }

        ObjectNode wrapper = Json.newObject();
        ObjectNode msg = Json.newObject();
        msg.put("message", "Successfully cancelled friends request");
        wrapper.set("success", msg);
        return ok(wrapper);
    }

    @Transactional
    @JwtSessionLogin
    public Result getUsersForSearch() {
        DynamicForm form = formFactory.form().bindFromRequest();
        User user = this.userProvider.getUser(session());
        String userName = form.get("userName");
        List<User> users = new ArrayList<>();
        if (user != null && userName != null) {
            users = this.userProvider.getUsersForSearch(user.getId(), userName);
        }

        if (users != null && !users.isEmpty()) {
            Logger.info(Arrays.toString(users.toArray()));
            return ok(Json.toJson(users));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "no users found for search");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getUsersNotFriends() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();
        List<User> users = new ArrayList<>();
        if (user != null) {
            users = UserHome.getUsersNotFriendsByUserId(user.getId(), em);
        }

        if (users != null && !users.isEmpty()) {
            return ok(Json.toJson(UserDTO.listToDTO(users)));
        } else {
            return ok(Json.newArray());
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getBackingQuestNotifications() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();

        if (user != null) {
            JsonNode jsonNode = NotificationFeed.getUserBackingNotificationsByUserId(user.getId(), em);
            return ok(Json.toJson(jsonNode));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "no backing users found for search");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getPendingQuestNotifications() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();

        if (user != null) {
            JsonNode jsonNode = NotificationFeed.getUserPendingNotificationsByUserId(user.getId(), em);
            return ok(Json.toJson(jsonNode));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "no pending quests for user found for search");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getCompletedQuestNotifications() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();

        if (user != null) {
            JsonNode jsonNode = NotificationFeed.getUserCompletedNotificationsByUserId(user.getId(), em);
            return ok(Json.toJson(jsonNode));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "no completed quests for user found for search");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getRecentQuests() {
        User user = this.userProvider.getUser(session());
        EntityManager em = this.jpaApi.em();

        if (user != null) {
            List<Quests> quests = QuestActivityHome.getQuestsNotInProgressForUser(user, 2, em).getList(user);

            return ok(Json.toJson(quests));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "no quests for user found for search");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Deprecated
    @Transactional
    @JwtSessionLogin
    public Result getCommentsForQuest() {
        EntityManager em = this.jpaApi.em();
        JsonNode json = request().body().asJson();
        Logger.info("JSON NODE = " + json);
        DynamicForm form = formFactory.form().bindFromRequest();
        User user = this.userProvider.getUser(session());
        if (user == null && form.get("userId") != null) {
            String userId = form.get("userId");
            user = UserService.getById(valueOf(userId), em);
        }
        String questId = form.get("questId");

        if (user != null && isNumeric(questId)) {
            try (Connection c = dbRo.getConnection()) {
                final Quests quest = QuestsDAO.findById(Integer.parseInt(questId), em);
                final List<CommentsDTO> questComments = commentsService.getQuestCommentsVisibleToUser(c, quest, user);
                return ok(Json.toJson(questComments));
            } catch (SQLException e) {
                Logger.error("getCommentsForQuest - error", e);
                return ok(Json.toJson(new LinkedList<CommentsDTO>()));
            }
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "no comments for user and quest found");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getAllCommentsForQuest() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        DynamicForm form = formFactory.form().bindFromRequest();
        String questId = form.get("questId");

        if (isNumeric(questId)) {
            try (Connection c = dbRo.getConnection()) {
                final Quests quest = QuestsDAO.findById(Integer.parseInt(questId), em);
                final List<CommentsDTO> questComments = commentsService.getQuestCommentsVisibleToUser(c, quest, user);
                return ok(Json.toJson(questComments));
            } catch (SQLException e) {
                Logger.error("getAllCommentsForQuest - error", e);
                return ok(Json.toJson(new LinkedList<CommentsDTO>()));
            }
        } else {
            return ok(Json.newArray());
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getUserInfo() {
        EntityManager em = this.jpaApi.em();
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        final ObjectNode response = (ObjectNode) Json.toJson(user);
        final Quests realtimeQuest = QuestActivityHome.getInProgressRealtimeQuestForUser(user, em);
        if (realtimeQuest != null) {
            final Optional<QuestTeam> team = Optional.of(realtimeQuest)
                    .map(quest -> new QuestTeamDAO(em).getTeamMember(quest, user))
                    .map(QuestTeamMember::getTeam);
            final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
            final QuestSEOSlugs seoSlugs;
            if (team.isPresent() && !team.get().isDefaultTeam()) {
                seoSlugs = publicTeamQuestSEOSlugs(team.get(), envUrl);
            } else {
                seoSlugs = publicQuestSEOSlugs(realtimeQuest, user, envUrl);
            }
            response.put("realtimeQuestId", seoSlugs.getQuestId());
            response.put("realtimeUserId", seoSlugs.getUserId());
        }

        // check if user is associated to company, if yes get company id
        Brand company = UserHome.getCompanyForUser(user, this.jpaApi);

        return ok( ResponseUtility.getBrandForUser(response,company) );
    }

    @Transactional
    public Result getUserInfoByUserId() {
        EntityManager em = this.jpaApi.em();
        DynamicForm form = formFactory.form().bindFromRequest();
        String userId = form.get("userId");

        if (isBlank(userId)) {
            return badRequest();
        }

        Logger.debug("getUserInfoByUserId - " + userId);

        User user = UserHome.findById(valueOf(userId), em);
        if (user == null) {
            return notFound();
        } else {
            Brand company = UserHome.getCompanyForUser(user, this.jpaApi);
            return ok(ResponseUtility.getBrandForUser((ObjectNode) Json.toJson(user),company));
        }
    }

    @Transactional
    public Result getUserInfoByUsername() {
        EntityManager em = this.jpaApi.em();
        DynamicForm form = formFactory.form().bindFromRequest();
        String username = form.get("username");

        if (isBlank(username)) {
            return badRequest();
        }

        Logger.debug("getUserInfoByUsername - " + username);

        User user = UserHome.findByName(username, em);
        if (user == null) {
            return notFound();
        } else {
            Brand company = UserHome.getCompanyForUser(user, this.jpaApi);
            return ok(ResponseUtility.getBrandForUser((ObjectNode) Json.toJson(user),company));
        }
    }

    @Transactional
    public Result uploadProfilePicture() {
        Logger.debug("uploadProfilePicture");

        final EntityManager em = this.jpaApi.em();
        final S3FileHome s3FileHome = new S3FileHome(config);
        final DynamicForm requestData = formFactory.form().bindFromRequest();
        final String email = requestData.get("email");
        final User user = UserService.getByEmail(email, em);
        if (user == null) {
            return notFound();
        }

        String contentType = requestData.get("contentType");
        
        final String profileImage = requestData.get("profPic");
        final String coverImage = requestData.get("coverPic");
        
        
       /* List<S3File> profPicDimensionsList = imageService.getFileFromRequest(request(), "profPic", ImageType.AVATAR, contentType);
        List<S3File> coverPicDimensionsList = imageService.getFileFromRequest(request(), "coverPic", ImageType.COVER_PHOTO, contentType);*/
        if ((profileImage == null || profileImage.length()==0) && (coverImage == null || coverImage.length()==0)) {
            return internalServerError("Unable to upload image.");
        }

        boolean updated = false;
        if ((profileImage != null && profileImage.length()>0)) {
            try {
              // s3FileHome.saveUserMedia(profPicDimensionsList, em);
               UserHome.updateUserProfilePicture(user.getId(), profileImage, em);
                updated = true;
            } catch (Exception e) {
                Logger.error("uploadProfilePicture - failed to upload profile picture for user => " + email + " => " + e, e);
            }
        }

        if ((coverImage != null && coverImage.length()>0)) {
            try {
                //s3FileHome.saveUserMedia(coverPicDimensionsList, em);
                UserHome.uploadUserCoverPicture(user.getId(), coverImage, em);
                updated = true;
            } catch (Exception e) {
                Logger.error("uploadCoverPicture - failed to upload cover picture for user => " + email + " => " + e, e);
            }
        }

        if (updated) {
            this.seoService.capturePageBackground(URLUtils.seoFriendlyUserProfilePath(user));
            this.seoService.capturePageBackground(URLUtils.seoFriendlierUserProfilePath(user));
        }

        final User profile = UserService.getByEmail(email, em);
        if (profile == null) {
            return notFound();
        } else {
            final ObjectNode result = Json.newObject();
            result.set("profilePictureURL", TextNode.valueOf(profile.getProfilePictureURL()));
            result.set("coverPictureURL", TextNode.valueOf(profile.getCoverPictureURL()));
            return ok(result);
        }
    }

    @Transactional
    public Result getProfilePictureForUser() {
        EntityManager em = this.jpaApi.em();
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String userId = requestData.get("userId");

        if (userId != null && !userId.isEmpty()) {
            String url = UserHome.getUserProfilePictureByUserId(valueOf(userId), em);
            if (url != null && !url.isEmpty()) {
                return ok(Json.toJson(url));
            } else {
                ObjectNode wrapper = Json.newObject();
                ObjectNode msg = Json.newObject();
                msg.put("message", "profile picture url not found for given user");
                wrapper.set("error", msg);
                return ok(wrapper);
            }
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "profile picture url not found for given user");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getQuestByQuestId(@Nonnull final Integer questId) {
        EntityManager em = this.jpaApi.em();
        Logger.info("Quest ID = " + questId);

        final QuestEntityWithACL questACL = new QuestEntityWithACL(() -> findById(questId, em), em);
        final User currentUser = userProvider.getUser(session());
        final VotingResult aclResult = questACL.eligible(currentUser);
        if (For.equals(aclResult)) {
            return ok(Json.toJson(questACL.getEntity(currentUser)));
        } else if (Abstain.equals(aclResult)) {
            return unauthorized();
        } else {
            return forbidden();
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result isQuestACL(@Nonnull Integer questId) {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());

        final QuestEntityWithACL questACL = new QuestEntityWithACL(() -> findById(questId, em), em);

        return ok(Json.toJson(questACL.eligible(user).getResult()));
    }

    @Transactional
    @JwtSessionLogin
    public Result getUserFavorites() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());

        if (user != null) {
            List<UserFavorites> userFavorites = UserHome.getUserFavoritesByUserId(user.getId(), em);
            return ok(Json.toJson(userFavorites));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "favorites not found");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    public Result getUserFavoritesById() {
        EntityManager em = this.jpaApi.em();
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String userId = requestData.get("userId");

        if (userId != null && !userId.isEmpty()) {
            User user = UserHome.findById(Integer.parseInt(userId), em);

            List<UserFavorites> userFavorites = UserHome.getUserFavoritesByUserId(user.getId(), em);
            return ok(Json.toJson(userFavorites));
        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "favorites not found for user");
            wrapper.set("error", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result addCommentsToQuest() {
        final Form<CommentAddForm> form = formFactory.form(CommentAddForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final CommentAddForm commentForm = form.get();
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        Logger.info("Quest, comment and reply ID : " + commentForm.getQuestId() + ", " + commentForm.getComments() + ", " + commentForm.getInReplyToId());

        final EntityManager em = this.jpaApi.em();
        final QuestComments addedComment = QuestCommentsDAO
                .addCommentsToQuestByUserIdAndQuestId(user.getId(), commentForm.getQuestId(), commentForm.getInReplyToId(), commentForm.getComments(), em);

        if (addedComment == null) {
            return internalServerError();
        }

        QuestImage questImage = null;
        Integer imageId;
        if ((imageId = commentForm.getImageId()) != null) {
            if ((questImage = QuestImageDAO.findById(imageId, em)) != null) {
                try (Connection c = db.getConnection()) {
                    QuestCommentsDAO.setCommentImage(c, addedComment.getId(), imageId.intValue());
                } catch (SQLException e) {
                    Logger.error("addCommentsToQuest - error", e);
                }
            }
        }

        // clear cache so it is removed from feed
        cache.remove("user.quests");

        final Quests quest = findById(addedComment.getQuestId(), em);
        if (addedComment.getInReplyTo() != null) {
            notificationService.addCommentReplyNotification(addedComment.getInReplyTo().getUserId(), user.getId(), quest.getId(), addedComment.getId());
        } else {
            final List<Integer> usersDoing = notificationService.getUserIdsSubscribedToQuest(quest);
            usersDoing.forEach(userDoing -> notificationService.addCommentNotification(
                    userDoing,
                    NotificationType.COMMENT,
                    user.getId(),
                    quest.getId(),
                    addedComment.getId()
            ));
        }

        final CommentsDTO result = CommentsDTO.toDTO(addedComment)
                .withMentions(commentsService.getCommentMentions(addedComment, user))
                .withReplies(commentsService.getCommentReplies(addedComment, user))
                .withImage((questImage == null) ? null : QuestImageDTO.toDTO(questImage));

        if (isNotEmpty(result.mentions)) {
            result.mentions.forEach(mention -> notificationService.addMentionNotification(
                    mention.id,
                    user.getId(),
                    quest.getId(),
                    addedComment.getId()
            ));
        }
        Brand company = UserHome.getCompanyForUser(user,jpaApi);
        if(company!=null){
            result.userFirstName = company.getName();
            result.userLastName = "";
        }

        return ok(Json.toJson(result));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result deleteComment(@NotNull Integer commentId) {
        EntityManager em = this.jpaApi.em();

        QuestCommentsDAO.deleteComment(commentId, em);

        try (Connection c = db.getConnection()) {
            QuestCommentsDAO.removeCommentImage(c, commentId);
        } catch (SQLException e) {
            Logger.error("deleteComment - error", e);
        }

        Logger.info(format("Comment removed  by Id - [%s] by user with ID [%s]", commentId, this.userProvider.getUser(session()).getId()));
        return ok(ResponseUtility.getSuccessMessageForResponse("removed comment"));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result editComment(final @NotNull Integer commentId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        final DynamicForm requestData = formFactory.form().bindFromRequest();
        final String commentString = requestData.get("comment");
        final String imageIdString = requestData.get("imageId");
        if (isBlank(commentString) && isBlank(imageIdString)) {
            return badRequest();
        }
        final Long imageId = (isNumeric(imageIdString) ? Long.parseLong(imageIdString) : null);

        final EntityManager em = this.jpaApi.em();

        final QuestComments existingComment = em.find(QuestComments.class, commentId);
        if (existingComment == null) {
            return notFound();
        }
        final String comment = isBlank(commentString) ? existingComment.getComments() : trim(commentString);
        final List<UserWithFiendStatusDTO> existingMentions = commentsService.getCommentMentions(existingComment, user);

        QuestCommentsDAO.editComment(commentId, comment, em);
        final QuestComments editedComment = em.find(QuestComments.class, commentId);

        final QuestImage questImage = imageId == null ? null : QuestImageDAO.findById(imageId.intValue(), em);
        try (Connection c = db.getConnection()) {
            Long currentImageId = QuestCommentsDAO.getCommentImage(c, commentId);
            if ((imageId == null) && (currentImageId != null)) {
                QuestCommentsDAO.removeCommentImage(c, commentId);
                QuestImageDAO.removeQuestPhotoById(currentImageId.intValue(), user.getId(), em);
            } else if ((imageId != null) && !imageId.equals(currentImageId)) {
                QuestCommentsDAO.setCommentImage(c, commentId, imageId);
                if (currentImageId != null) {
                    QuestImageDAO.removeQuestPhotoById(currentImageId.intValue(), user.getId(), em);
                }
            }
            // else: nothing to change
        } catch (SQLException e) {
            Logger.error("Cannot edit comment with ID " + commentId, e);
        }

        Logger.info(format("Comment edited  by Id - [%s] by user with ID [%s]", commentId, user.getId()));
        final CommentsDTO result = CommentsDTO.toDTO(editedComment)
                .withMentions(commentsService.getCommentMentions(editedComment, user))
                .withReplies(commentsService.getCommentReplies(editedComment, user))
                .withImage(((questImage == null) ? null : QuestImageDTO.toDTO(questImage)));

        final List<UserWithFiendStatusDTO> addedMentions = isEmpty(result.mentions)
                ? emptyList()
                : result.mentions
                .stream()
                .filter(mention -> existingMentions
                        .stream()
                        .noneMatch(existingMention -> existingMention.id.equals(mention.id)))
                .collect(toList());

        if (isNotEmpty(addedMentions)) {
            addedMentions.forEach(addedMention -> notificationService.addMentionNotification(
                    addedMention.id,
                    user.getId(),
                    existingComment.getQuestId(),
                    editedComment.getId()
            ));
        }

        return ok(Json.toJson(result));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result likeComment(final @NotNull Integer commentId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        return jpaApi.withTransaction(em -> {
            final QuestComments comment = em.find(QuestComments.class, commentId);
            if (comment == null) {
                return notFound();
            } else {
                final boolean liked = QuestCommentsDAO.toggleCommentLike(comment, user, em);
                notificationService.addCommentNotification(
                        comment.getUserId(),
                        NotificationType.COMMENT_LIKED,
                        user.getId(),
                        comment.getQuestId(),
                        comment.getId()
                );
                return ok(BooleanNode.valueOf(liked));
            }
        });
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result hideComment(final Integer commentId) {
        final User user = this.userProvider.getUser(session());
        final QuestComments comment = this.commentsService.getById(commentId);
        if (comment == null) {
            return notFound();
        }
        contentReportingService.hideCommentForUser(comment, user);
        return ok();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result reportComment(final Integer commentId) {
        final User user = this.userProvider.getUser(session());
        final QuestComments comment = this.commentsService.getById(commentId);
        if (comment == null) {
            return notFound();
        }
        contentReportingService.reportComment(comment, user);
        emailService.sendContentReportEmail(request(), user, QuestComments.class.getSimpleName(), comment.getId());
        return ok();
    }

    @Transactional
    @JwtSessionLogin
    public Result searchFeedByCategory() {
        EntityManager em = this.jpaApi.em();
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String category = requestData.get("category");
        Logger.info("request data category = " + category);
        User user = this.userProvider.getUser(session());

        if (user != null && category != null) {
            List<Quests> quests = QuestsDAO.findByCategory(category, user, em);
            return ok(Json.toJson(quests));
        } else {
            List<Quests> questsAll = QuestsDAO.all(em);
            Logger.info("QUESTS = " + Arrays.toString(questsAll.toArray()));
            return ok(Json.toJson(questsAll));
        }

    }

    @Transactional
    @JwtSessionLogin
    public Result getQuestCompletionPercentagesInProgress() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());

        if (user != null) {
            List<Quests> quests = QuestActivityHome.getInProgressQuestsForUser(user, em).getList(user);
            List<QuestTasksDAO.CompletionPercentage> completionPercentages = getQuestsCompletionPercentages(user, quests);

            return ok(Json.toJson(completionPercentages));
        } else {
            return ok(Json.newArray());
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getQuestCompletionPercentagesNotActive() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());

        if (user != null) {

            List<Quests> quests = QuestActivityHome.getQuestsNotInProgressForUser(user, null, em).getList(user);
            List<QuestTasksDAO.CompletionPercentage> completionPercentages = getQuestsCompletionPercentages(user, quests);
            return ok(Json.toJson(completionPercentages));
        } else {
            return ok(Json.newArray());
        }
    }

    private List<QuestTasksDAO.CompletionPercentage> getQuestsCompletionPercentages(User user, List<Quests> quests) {
        List<QuestTasksDAO.CompletionPercentage> completionPercentages = new ArrayList<>();
        if (quests != null) {
            for (Quests quest : quests) {
                String completionPercentage = questService.getQuestCompletionPercentage(quest, user);
                QuestTasksDAO.CompletionPercentage completionPercentageToAdd = new QuestTasksDAO.CompletionPercentage();
                completionPercentageToAdd.questId = quest.getId();
                completionPercentageToAdd.completionPercentage = completionPercentage;
                completionPercentages.add(completionPercentageToAdd);
            }
        }
        return completionPercentages;
    }

    @Transactional
    @JwtSessionLogin
    public Result getQuestCompletionPercentagesByUserId() {
        DynamicForm form = formFactory.form().bindFromRequest();
        EntityManager em = this.jpaApi.em();
        String userId = form.get("userId");
        User loggedInUser = this.userProvider.getUser(session());

        if (userId != null) {
            User user = UserHome.findById(Integer.parseInt(userId), em);

            List<Quests> quests = QuestActivityHome.getInProgressQuestsForUser(user, em).getList(loggedInUser);
            List<QuestTasksDAO.CompletionPercentage> completionPercentages = getQuestsCompletionPercentages(user, quests);
            return ok(Json.toJson(completionPercentages));
        } else {
            return ok(Json.newArray());
        }
    }

    @Transactional
    public Result getQuestCompletionPercent(@Nonnull final Integer questId, @Nonnull final Integer userId) {
        EntityManager em = this.jpaApi.em();

        final Quests quest = QuestsDAO.findById(questId, em);
        final User doer = UserHome.findById(userId, em);
        if (quest == null || doer == null) {
            return notFound();
        }
        final String completionPercentage = questService.getQuestCompletionPercentage(quest, doer);
        QuestTasksDAO.CompletionPercentage completionPercentageToAdd = new QuestTasksDAO.CompletionPercentage();
        completionPercentageToAdd.questId = questId;
        completionPercentageToAdd.completionPercentage = completionPercentage;

        return ok(Json.toJson(completionPercentageToAdd));
    }

    @Transactional
    @JwtSessionLogin
    public Result getQuestCompletionPercentageByQuest() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String questId = requestData.get("questId");

        if (questId != null && user != null) {
            final Quests quest = QuestsDAO.findById(valueOf(questId), em);
            if (quest == null) {
                return notFound();
            }
            final String completionPercentage = questService.getQuestCompletionPercentage(quest, user);

            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("questId", questId);
            msg.put("completionPercentage", completionPercentage);
            wrapper.set("success", msg);
            return ok(wrapper);
        } else {
            return ok(ResponseUtility.getErrorMessageForResponse("error getting completion percentages"));
        }
    }

    @Transactional
    public Result getQuestCompletionPercentageByUserAndQuest(@NotNull Integer userId, @NotNull Integer questId) {
        EntityManager em = this.jpaApi.em();

        User user = UserHome.findById(userId, em);
        if (user != null) {
            final Quests quest = QuestsDAO.findById(questId, em);
            if (quest == null) {
                return notFound();
            }
            String completionPercentage = questService.getQuestCompletionPercentage(quest, user);

            QuestTasksDAO.CompletionPercentage completionPercentageToAdd = new QuestTasksDAO.CompletionPercentage();
            completionPercentageToAdd.questId = questId;
            completionPercentageToAdd.completionPercentage = completionPercentage;

            return ok(Json.toJson(completionPercentageToAdd));
        } else {
            return ok(ResponseUtility.getErrorMessageForResponse("error getting completion percentages"));
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getQuestTasksForUser() {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final DynamicForm requestData = formFactory.form().bindFromRequest();
        final String questId = requestData.get("questId");
        if (!isNumeric(questId)) {
            return badRequest();
        }
        final EntityManager em = this.jpaApi.em();
        final Quests quest = em.find(Quests.class, valueOf(questId));
        if (quest == null) {
            return notFound();
        }
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        final List<MilestoneDTO> milestones = questService.listMilestonesForQuest(quest, user).stream()
                .map(task -> milestoneService.convertToDto(task, envUrl, linkPreviewService))
                .collect(toList());
        return ok(Json.toJson(milestones));
    }


    @Transactional
    @JwtSessionLogin(required = true)
    public Result hideQuest(final Integer questId) {
        final User user = this.userProvider.getUser(session());
        final Quests quest = QuestsDAO.findById(questId, jpaApi.em());
        if (quest == null) {
            return notFound();
        }
        contentReportingService.hideQuestForUser(quest, user);
        return ok();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result reportQuest(Integer questId) {
        final User user = this.userProvider.getUser(session());
        final Quests quest = QuestsDAO.findById(questId, jpaApi.em());
        if (quest == null) {
            return notFound();
        }
        contentReportingService.reportQuest(quest, user);
        emailService.sendContentReportEmail(request(), user, Quests.class.getSimpleName(), quest.getId());
        return ok();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result updateQuestTasks() {
        User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final Form<MilestoneToggleForm> formBinding = formFactory.form(MilestoneToggleForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final EntityManager em = this.jpaApi.em();
        final MilestoneToggleForm form = formBinding.get();
        final QuestTasks milestone = QuestTasksDAO.findById(form.getTaskId(), em);
        if (milestone == null) {
            return notFound();
        }
        final QuestInviteDAO questInviteDAO = new QuestInviteDAO(em);
        Quests questForInvite = new Quests(); 
        questForInvite.setId(milestone.getQuestId());
        List<QuestInvite> questInvites = questInviteDAO.getInvitesForQuest(questForInvite);
        for(QuestInvite questInvite : questInvites) {
        	if(questInvite.invitedUser.getId() == user.getId()) {
        		user = questInvite.user;
        		break;
        	}
        }
        		
       try (Connection c = db.getConnection(); Connection cRo = dbRo.getConnection()) {
          return ok(BooleanNode.valueOf(questService.toggleMilestoneCompletion(c, cRo, milestone, user, form.getPoint())));  
       } catch (SQLException e) {
           Logger.error("updateQuestTasks - unable to perform action", e);
           return internalServerError();
       }
    }

    @Transactional
    @JwtSessionLogin
    public Result editMilestone() {
        final Form<TaskEditForm> formBinding = formFactory.form(TaskEditForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final TaskEditForm form = formBinding.get();
        if (form.getId() == null) {
            return badRequest();
        }
        final EntityManager em = this.jpaApi.em();
        return manageMilestone(form.getId(), (milestone, user) -> {
            milestone.setTask(form.getTask());
            if (form.getVideo() != null) {
                milestone.setVideo(QuestTasksDAO.createEmbeddedVideo(form.getVideo(), em));
            }
            em.merge(milestone);
            Logger.info("MILESTONE EDITED " + Json.toJson(milestone));
        });
    }

    @Transactional
    @JwtSessionLogin
    public Result addMilestone(final Integer questId) {
        final Form<TaskCreateForm> formBinding = formFactory.form(TaskCreateForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final TaskCreateForm form = formBinding.get();
        final User currentUser = userProvider.getUser(session());
        if (currentUser == null) {
            return unauthorized();
        }
        final EntityManager em = this.jpaApi.em();
        final Quests quest = findById(questId, em);
        if (quest == null) {
            return notFound();
        }
        try {
            if (form.getGroupIndex() == null || form.getGroupIndex() < 0) {
                form.setGroupIndex(0);
            }
            patchTaskImageUrl(form, questId);

            final QuestTasks milestone = taskGroupService.createTask(currentUser, form.getQuestOwnerId(), quest.getId(), form);
            if (milestone == null) {
                return internalServerError();
            } else {
                final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
                return ok(Json.toJson(milestoneService.convertToDto(milestone, envUrl, linkPreviewService)));
            }
        } catch (final QuestOperationForbiddenException e) {
            return forbidden();
        }
    }

    private URL saveDataUrlImage(final String dataUrl) throws IOException {
        final String contentType = removeEnd(removeStart(substringBefore(dataUrl, ","), "data:"), ";base64");
        final String extension = substringAfterLast(contentType, "/");
        final byte[] imageData = Base64.getDecoder().decode(substringAfter(dataUrl, ","));
        try (final ByteArrayInputStream imageStream = new ByteArrayInputStream(imageData);
             final ByteArrayInputStream dataStream = new ByteArrayInputStream(imageData)) {
            final BufferedImage bufferedImage = ImageIO.read(imageStream);
            final S3File s3File = new S3File();

            s3File.setName("__task." + extension);
            s3File.setContentData(dataStream);
            s3File.setContentLength(imageData.length);
            s3File.setImgWidth(bufferedImage.getWidth());
            s3File.setImgHeight(bufferedImage.getHeight());
            s3File.setContentType(contentType);

            final S3FileHome s3FileHome = new S3FileHome(config);

            s3FileHome.saveUserMedia(singletonList(s3File), jpaApi.em());

            return s3FileHome.getUrl(s3File, true);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result removeMilestone() {
        final DynamicForm requestData = formFactory.form().bindFromRequest();
        final String id = requestData.get("id");
        if (isNumeric(id)) {
            final EntityManager em = this.jpaApi.em();
            return manageMilestone(valueOf(id), (milestone, user) -> {
                final Quests quest = QuestsDAO.findById(milestone.getQuestId(), em);
                if (quest == null) {
                    Logger.warn(format("Not allowed to remove milestone with ID [%s] from Quest with ID [%s]", id, milestone.getQuestId()));
                } else {
                    taskGroupService.removeTask(milestone);
                }
            });
        } else {
            return badRequest();
        }
    }


    @Transactional
    @JwtSessionLogin
    public Result reorderMilestones() {
        final JsonNode jsonNode = request().body().asJson();
        if (jsonNode == null || !ArrayNode.class.isAssignableFrom(jsonNode.getClass())) {
            return badRequest();
        }
        final ArrayNode milestoneIds = (ArrayNode) jsonNode;
        final List<Integer> ids = StreamSupport.stream(milestoneIds.spliterator(), false)
                .map(JsonNode::asInt)
                .collect(toList());
        final AtomicInteger order = new AtomicInteger(0);
        return jpaApi.withTransaction(em -> {
            ids.forEach(id -> manageMilestone(id, (milestone, user) -> {
                milestone.setOrder(order.getAndIncrement());
                em.merge(milestone);
            }));
            return ok();
        });
    }

    private Result manageMilestone(final Integer id, final BiConsumer<QuestTasks, User> milestoneConsumer) {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = jpaApi.em();
        final QuestTasks milestone = QuestTasksDAO.findById(id, em);
        if (milestone == null) {
            return notFound();
        }
        if (questService.canEditMilestone(milestone, user)) {

            milestoneConsumer.accept(milestone, user);

            return ok();
        } else {
            return forbidden();
        }
    }


    private static PrivacyLevel getPrivacyLevel(final String privacyLevelString) {
        return Arrays.stream(PrivacyLevel.values())
                .filter(level -> equalsIgnoreCase(level.name(), privacyLevelString))
                .findFirst()
                .orElse(PUBLIC);
    }

    private static Interests getInterest(final String interestString) {
        return Arrays.stream(Interests.values())
                .filter(interest -> equalsIgnoreCase(interest.getValue(), interestString))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public Result doSignup() {
        noCache(response());
        Form<MyUsernamePasswordAuthProvider.MySignup> filledForm = this.provider.getSignupForm()
                .bindFromRequest();
        if (filledForm.hasErrors()) {
            // User did not fill everything properly
            return badRequest(filledForm.errorsAsJson());
        } else {
            // Everything was filled
            // do something with your part of the form before handling the user
            // signup
            int status = this.provider.handleSignup(ctx()).asScala().header().status();
            if (status == 200 || status == 303) {
                ObjectNode wrapper = Json.newObject();
                ObjectNode msg = Json.newObject();
                msg.put("message", "signed up successfully");
                wrapper.set("success", msg);
                return ok(wrapper);
            } else {
                ObjectNode wrapper = Json.newObject();
                ObjectNode msg = Json.newObject();
                msg.put("message", "Signup failed");
                wrapper.set("error", msg);
                return ok(wrapper);
            }
        }
    }

    @JwtSessionLogin(required = true)
    public Result restricted() {
        return ok();
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listPlatformUsersExcludingCurrent() {
        final User currentUser = this.userProvider.getUser(session());
        if (currentUser == null) {
            return unauthorized();
        }
        final ArrayNode array = Json.newArray();
        UserHome.getAllUsersToInviteDTOs(currentUser.getId(), jpaApi.em()).forEach(array::addPOJO);
        return ok(array);
    }

    @Transactional
    public Result listPlatformBrands() {
        final ArrayNode array = Json.newArray();
        new BrandConfigDAO(jpaApi).getLandingBrands().stream()
                .map(brand -> new UserSearchDTO(
                        brand.getUserId(),
                        Optional.ofNullable(brand.getFullName()).orElse(brand.getUser().getName()),
                        null,
                        brand.getUser().getUserName(),
                        brand.getUser().getMissionStatement(),
                        Optional.ofNullable(brand.getLogoUrl()).orElse(brand.getUser().getProfilePictureURL())))
                .forEach(array::addPOJO);
        return ok(array);
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listInvitesForQuest(final String questId) {
        return listInvitedUsersForQuest(questId, quest -> new QuestInviteDAO(jpaApi.em())
                .getInvitesForQuest(quest)
                .stream()
                .map(invite -> new UserToInviteDTO(invite.invitedUser.getEmail(), invite.invitedUser.getName())));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result listAdminsForQuest(final String questId) {
        return listInvitedUsersForQuest(questId, quest -> quest.getAdmins().stream()
                .map(admin -> new UserToInviteDTO(admin.getEmail(), admin.getName())));
    }

    private Result listInvitedUsersForQuest(final String questId, final Function<Quests, Stream<UserToInviteDTO>> invitedUsersProvider) {
        if (!isNumeric(questId)) {
            return badRequest();
        }
        final EntityManager em = this.jpaApi.em();
        final ArrayNode result = Json.newArray();
        final Quests quest = findById(Integer.parseInt(questId), em);
        if (quest != null) {
            invitedUsersProvider.apply(quest).forEach(result::addPOJO);
        }
        return ok(result);
    }

    private EmbeddedVideo saveOrUpdateQuestVideo(final String url) {
        final EmbeddedVideoDAO dao = new EmbeddedVideoDAO(jpaApi.em());
        final EmbeddedVideo existingVideo = dao.findByURL(url);
        if (existingVideo == null) {
            final EmbeddedVideo video = new EmbeddedVideo();
            video.url = url;
            video.provider = VideoProvider.YOUTUBE;
            return dao.save(video, EmbeddedVideo.class);
        } else {
            return existingVideo;
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result editQuest() {
        final User user = this.userProvider.getUser(session());
        final EntityManager em = this.jpaApi.em();
    	
        if (user == null) {
            return unauthorized();
        }
        final Form<QuestEditForm> questEditFormData = formFactory.form(QuestEditForm.class).bindFromRequest(request());
        if (questEditFormData.hasErrors()) {
            return badRequest(questEditFormData.errorsAsJson());
        }

        final QuestEditForm form = questEditFormData.get();
        if (isTrue(form.getFundraising()) && form.getFundraisingConfig() == null) {
            return badRequest();
        }

        List<TaskEditForm> taskEditForms = form.getQuestTasks();

        final PrivacyLevel privacyLevel = getPrivacyLevel(form.getPrivacy());

        final Quests quest = findById(form.getQuestId(), em);

        if (quest == null) {
            return notFound();
        }

        final QuestPermissionsDTO permissions = new QuestPermissionsDTO(quest, user);
        if (permissions.editable) {

            final Date date = new Date();
            quest.setTitle(form.getQuestName());
            
            if (isNotBlank(form.getQuestDescription())) {
                quest.setQuestFeed(form.getQuestDescription());
            }
            if (isNotBlank(form.getQuestShortDescription())) {
                quest.setShortDescription(form.getQuestShortDescription());
            }
            quest.setCopyAllowed(isTrue(form.getCopyAllowed()));
            quest.setMultiTeamsEnabled(isTrue(form.getMultiTeamsAllowed()));
            quest.setEditableMilestones(form.getEditableMilestones() == null
                    ? quest.isEditableMilestones()
                    : isTrue(form.getEditableMilestones()));
            quest.setMilestoneControlsDisabled(form.getMilestoneControlsDisabled());
            quest.setTaskViewDisabled(form.getTaskViewDisabled());
            quest.setPrivacyLevel(privacyLevel);
            if (form.getBackBtnDisabled() != null) {
                quest.setBackBtnDisabled(isTrue(form.getBackBtnDisabled()));
            }
            if (form.getFundraising() != null) {
                quest.setFundraising(isTrue(form.getFundraising()));
            }
            if (isBlank(form.getQuestVideoUrl())) {
                quest.setVideo(null);
            } else {
                quest.setVideo(saveOrUpdateQuestVideo(form.getQuestVideoUrl()));
            }
            
            quest.setPhoto(form.getPhoto());
            quest.setDateModified(date);
            quest.setModifiedBy(user.getId());

            QuestsDAO.update(quest, em);

            Logger.info(format("Existing Quest [%s] edited by user [%s]", quest.getId(), user.getId()));
            
             if(form.getTasksGroups()!=null) {
            	addUpdateTaskGroup(form.getTasksGroups(), quest, user);
             }

            if(taskEditForms!=null && taskEditForms.size()>0) {
            	updateMilstoneTask(quest, taskEditForms, user);
            }
            updateQuestAdmins(quest, quest.getUser(), form);
            updateQuestLeaderboards(quest, form);

            if (INVITE.equals(privacyLevel)) {
                updateQuestInvites(quest, quest.getUser(), form.getInvites());

                Logger.info(format("Invites updated for Quest [%s] edited by user [%s]", quest.getId(), user.getId()));
            }
            if (quest.isFundraising() && fundraisingService.getFundraisingLink(quest, quest.getUser()) == null) {
                Logger.info(format("Starting fundraising for Quest [%s] created by [%s]", quest.getId(), quest.getCreatedBy()));
                final FundraisingLinkDTO fundraisingLink = fundraisingService.startFundraising(
                        quest,
                        quest.getUser(),
                        form.getFundraisingConfig().getTargetAmount(),
                        form.getFundraisingConfig().getCurrency(),
                        null,
                        null,
                        form.getFundraisingConfig().getCampaignName(),
                        form.getFundraisingConfig().getCoverImageUrl()
                );
                if (fundraisingLink != null) {
                    emailService.sendFundraisingStartCreatorEmail(request(), quest.getUser(), quest);
                }
                quest.setShowBackingAmount(true);
            }

            QuestEventHistoryDAO.addEventHistory(quest.getId(), user.getId(), QUEST_EDIT, null, em);

            // Trigger an seo page recapture
            String seoUpdatePath = null;
            if (quest.getMode() == QuestMode.TEAM) {
                QuestTeamDAO qtDao = new QuestTeamDAO(em);
                QuestTeamMember member = qtDao.getTeamMember(quest, quest.getUser());
                if (member != null) {
                    seoUpdatePath = URLUtils.relativeTeamUrl(member.getTeam());
                }
            } else {
                seoUpdatePath = URLUtils.seoFriendlyPublicQuestPath(quest, quest.getUser());
            }
            if (seoUpdatePath != null) {
                seoService.capturePageBackground(seoUpdatePath);
                Logger.debug("editQuest - pushed path for seo update: " + seoUpdatePath);
            }

            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "created new quest successfully");
            msg.put("questId", quest.getId());
            wrapper.set("success", msg);
            return ok(wrapper);

        } else {
            final Integer createdByUserId = quest.getCreatedBy();

            Logger.info("originalUser = " + createdByUserId);

            final Quests newQuest = QuestsDAO.createQuest(form.getQuestName(), form.getQuestShortDescription(), form.getQuestDescription(),
                    Interests.valueOf(quest.getPillar().toUpperCase()), user.getId(), createdByUserId, isTrue(form.getCopyAllowed()),
                    isNotFalse(form.getBackBtnDisabled()), isTrue(form.getEditableMilestones()), isTrue(form.getMilestoneControlsDisabled()), isTrue(form.getTaskViewDisabled()), isTrue(form.getMultiTeamsAllowed()), quest.getVersion(), privacyLevel,
                    QuestCreatorTypes.valueOf(quest.getType().toUpperCase()), isTrue(form.getFundraising()), null, quest.getMode(), em);

            QuestsDAO.addQuestPhotoByQuestId(newQuest.getId(), user.getId(), quest.getPhoto(), em);

            //save already exiting milestones then add from request
            List<QuestTasks> existingTasks = QuestTasksDAO.getQuestTasksByQuestIdAndUserId(form.getQuestId(), createdByUserId, em);
            if (!existingTasks.isEmpty()) {
                for (QuestTasks existingTask : existingTasks) {
                    Logger.info("Adding existing tasks = " + existingTask.getTask());
                    QuestTasksDAO.copyTaskWithoutGroupToUser(existingTask, user, newQuest, em);
                }

                List<QuestTasks> usersTasks = QuestTasksDAO.getQuestTasksByQuestIdAndUserId(form.getQuestId(), user.getId(), em);
                for (QuestTasks userTask : usersTasks) {
                    // removing original tasks
                    QuestTasksDAO.remove(userTask, em);
                }
            }

            //now check if user has this saved or doing - update to new quest accordingly.

            //saved
            boolean isQuestSaved = QuestSavedDAO.doesQuestSavedExistForUser(form.getQuestId(), user.getId(), em);
            if (isQuestSaved) {
                // remove old quest if they choose
                if ("N".equalsIgnoreCase(form.getStayInOldQuest())) {
                    QuestSavedDAO.removeQuestForUser(form.getQuestId(), user.getId(), em);
                } else {
                    Logger.info("User has requested to stay in the old quest, continuing...");
                }
                // replace with new
                QuestSavedDAO.saveQuestForUser(newQuest, user, em);
            }
            //in progress
            QuestActivity questActivity = QuestActivityHome.getQuestActivityForQuestIdAndUser(quest, user, em);
            if (questActivity != null && IN_PROGRESS.equals(questActivity.getStatus())) {
                // remove old if they choose
                if ("N".equalsIgnoreCase(form.getStayInOldQuest())) {
                    QuestActivityHome.removeAllQuestActivity(quest.getId(), user.getId(), em);
                }
                // replace new
                QuestActivityHome.startQuestForUser(newQuest.getId(), user.getId(), newQuest.getMode(), em);
            }

            // on success add event history
            QuestEventHistoryDAO.addEventHistory(newQuest.getId(), user.getId(), QUEST_EDIT_NEW, quest.getId(), em);

            // Don't trigger an seo page capture update here; this code seemed like it was about creating quests so there will presumably be a startQuest call.

            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "created new quest successfully");
            msg.put("questId", newQuest.getId());
            wrapper.set("success", msg);
            return ok(wrapper);
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result newQuest() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final Form<QuestCreateForm> formData = formFactory.form(QuestCreateForm.class).bindFromRequest(request());
        if (formData.hasErrors()) {
            return badRequest(formData.errorsAsJson());
        }

        final QuestCreateForm form = formData.get();
        if (isTrue(form.getFundraising()) && form.getFundraisingConfig() == null) {
            return badRequest();
        }
        if (isBlank(form.getQuestShortDescription()) && isBlank(form.getQuestDescription())) {
            return badRequest();
        }
        final EntityManager em = this.jpaApi.em();
        final PrivacyLevel privacyLevel = getPrivacyLevel(form.getPrivacy());
        final Quests quest = createNewQuestForUserOrBrand(form, user);
        if (quest == null) {
            throw new IllegalStateException(format("Quest '%s' not created for user '%s'", form.getQuestName(), user.getEmail()));
        }
        Logger.info(format("New Quest [%s] created by user [%s]", quest.getId(), user.getId()));

        if (isNotEmpty(form.getQuestTasks())) {
            createQuestTasksGroups(quest, user, form.getTasksGroups(), form.getQuestTasksGroupName());
            createQuestTasks(quest, user, form.getQuestTasks());
        }
        updateQuestAdmins(quest, user, form);
        updateQuestLeaderboards(quest, form);

        if (INVITE.equals(privacyLevel)) {
            updateQuestInvites(quest, user, form.getInvites());

            Logger.info(format("Invites updated for Quest [%s] created by user [%s]", quest.getId(), user.getId()));
        }
        if (quest.isFundraising()) {
            Logger.info(format("Starting fundraising for Quest [%s] created by [%s]", quest.getId(), user.getId()));
//            get the brand config
            BrandConfig brandId = null;
            if (quest.getMode().equals(TEAM)) {
                brandId = em.find(BrandConfig.class, quest.getCreatedBy());
            }
            final FundraisingLinkDTO fundraisingLink = fundraisingService.startFundraising(
                    quest,
                    user,
                    form.getFundraisingConfig().getTargetAmount(),
                    form.getFundraisingConfig().getCurrency(),
                    brandId,
                    null,
                    form.getFundraisingConfig().getCampaignName(),
                    form.getFundraisingConfig().getCoverImageUrl()
            );
            if (fundraisingLink != null) {
                emailService.sendFundraisingStartCreatorEmail(request(), user, quest);
            }
            quest.setShowBackingAmount(true);
        }

        if (form.getQuestVideoUrl() != null && hasText(form.getQuestVideoUrl())) {
            quest.setVideo(saveOrUpdateQuestVideo(form.getQuestVideoUrl()));
        }

        QuestEventHistoryDAO.addEventHistory(quest.getId(), user.getId(), QUEST_CREATE, null, em);
        new QuestUserFlagDAO(em).followQuestForUser(quest, user);

        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        final QuestDTO result = QuestDTO.toDTO(quest).withSEOSlugs(publicQuestSEOSlugs(quest, user, envUrl));

        return ok(Json.toJson(result));
    }

    private Quests createNewQuestForUserOrBrand(final QuestCreateForm form, final User user) {
        final PrivacyLevel privacyLevel = getPrivacyLevel(form.getPrivacy());
        final Interests interest = getInterest(form.getPillar());
        final EntityManager em = this.jpaApi.em();
        final boolean isUserBrand = "Y".equalsIgnoreCase(user.getIsUserBrand());
        return QuestsDAO.createQuest(
                form.getQuestName(),
                trimToEmpty(form.getQuestDescription()),
                trimToNull(form.getQuestShortDescription()),
                interest,
                user.getId(),
                null,
                isTrue(form.getCopyAllowed()),
                isNotFalse(form.getBackBtnDisabled()),
                isTrue(form.getEditableMilestones()),
                isTrue(form.getMilestoneControlsDisabled()),
                isTrue(form.getTaskViewDisabled()),
                isTrue(form.getMultiTeamsAllowed()),
                0,
                privacyLevel,
                isUserBrand ? BRAND : MEMBER,
                isTrue(form.getFundraising()),
                form.getPhoto(),
                QuestMode.fromKey(form.getMode()),
                em
        );
    }

    private void updateQuestInvites(final Quests quest, final User user, final List<String> invitedEmails) {
        final Integer questId = quest.getId();
        final EntityManager em = this.jpaApi.em();
        final QuestInviteDAO questInviteDAO = new QuestInviteDAO(em);
        final List<QuestInvite> existingInvites = questInviteDAO.getInvitesForQuest(quest);
        emptyIfNull(invitedEmails).forEach(email -> {
            final boolean exists = existingInvites.removeIf(invite -> equalsIgnoreCase(invite.invitedUser.getEmail(), email));
            if (exists) {
                Logger.info(format("Keeping existing invited user with email '%s' to Quest with ID [%s]", email, questId));
            } else {
                final QuestInvite questInvite = new QuestInvite();
                questInvite.quest = quest;
                questInvite.user = user;
                questInvite.invitedUser = UserHome.findByEmail(email, em);
                if (questInvite.invitedUser == null) {
                    Logger.warn(format("User with email '%s' cannot be invited to Quest with ID [%s] - email doesn't not exist", email, questId));
                } else {
                    final QuestInvite savedInvite = questInviteDAO.save(questInvite, QuestInvite.class);
                    Logger.info(format("User with email '%s' has been invited to Quest with ID [%s] - invite created with ID [%s]", email, questId, savedInvite.id));

                    emailService.sendQuestInvitationEmail(request(), savedInvite.invitedUser, savedInvite.user, savedInvite.quest);
                }
            }
        });
        existingInvites.forEach(questInviteDAO::delete);
    }

    private void updateQuestLeaderboards(final Quests quest, final QuestForm form) {
        final EntityManager em = this.jpaApi.em();
        final HappeningDAO dao = new HappeningDAO(em);
        final boolean updateValue = form.getLeaderboardEnabled() == null
                ? quest.isLeaderboardEnabled()
                : isTrue(form.getLeaderboardEnabled());
        if (quest.isLeaderboardEnabled() != updateValue) {
            quest.setLeaderboardEnabled(updateValue);
            if (updateValue) {
                final List<LeaderboardMember> members = leaderboardService.updateLeaderboardMembers(dao.getHappeningByQuestId(quest.getId()));
                if (members.isEmpty()) {
                    Logger.info("No leaderboard members to import for Quest with ID " + quest.getId());
                } else {
                    Logger.info(members.size() + " leaderboard members imported for Quest with ID " + quest.getId());
                }
            }
        }
    }

    private void createQuestTasks(final Quests quest, final User user, final List<TaskCreateForm> milestones) {
        emptyIfNull(milestones).forEach(milestone -> {
            if (milestone != null) {
              //  patchTaskImageUrl(milestone, quest.getId());
                taskGroupService.createTask(user, null, quest.getId(), milestone);
                Logger.info(format("Milestone '%s' added to Quest with ID [%s] by user with ID [%s]", milestone, quest.getId(), user.getId()));
            }
        });
    }

    private void createQuestTasksGroups(final Quests quest, final User user, final List<TasksGroupManageForm> groups, final String defaultTaskGroupName) {
        //taskGroupService.createDefaultGroup(user, null, quest.getId(), defaultTaskGroupName);
    	if(groups==null || groups.size()==0) {
        	taskGroupService.createDefaultGroup(user, null, quest.getId(), defaultTaskGroupName);
        }else {
	    	emptyIfNull(groups).forEach(group -> {
	            if (group != null && isNotBlank(trim(group.getGroupName()))) {
	                taskGroupService.createGroup(user, null, quest.getId(), group.getGroupName());
	                Logger.info(format("Milestone '%s' added to Quest with ID [%s] by user with ID [%s]", group, quest.getId(), user.getId()));
	            }
	        });
        }
        
    }

    private void updateQuestAdmins(final Quests quest, final User creator, final QuestForm form) {
        if (quest == null || form == null) {
            return;
        }
        final Set<String> questAdmins = emptyIfNull(quest.getAdmins()).stream()
                .map(User::getEmail)
                .filter(StringUtils::isNotBlank)
                .collect(toCollection(LinkedHashSet::new));
        final Set<String> formAdmins = new LinkedHashSet<>(emptyIfNull(form.getAdmins()));

        final EntityManager em = jpaApi.em();

        Logger.info("Setting Quest up creator for " + quest.getId());
        quest.setUser(creator);

        Logger.info("quest Admins = " + Arrays.toString(questAdmins.toArray()));
        Logger.info("form Admins = " + Arrays.toString(formAdmins.toArray()));
        final Set<String> addedAdmins = new LinkedHashSet<>(formAdmins);
        addedAdmins.removeAll(questAdmins);
        addedAdmins.forEach(email -> {
            if (hasText(email)) {
                Logger.info(format("New admin '%s' added to Quest with ID [%s]", email, quest.getId()));
                emailService.sendAdminAddedEmail(request(), UserHome.findByEmail(email, em), creator, quest);
            }
        });

        final Set<String> removedAdmins = new LinkedHashSet<>(questAdmins);
        removedAdmins.removeAll(formAdmins);
        removedAdmins.forEach(email -> {
            Logger.info(format("Previous admin '%s' removed from Quest with ID [%s]", email, quest.getId()));
            emailService.sendAdminRemovedEmail(request(), UserHome.findByEmail(email, em), creator, quest);
        });


        quest.setAdmins(formAdmins.stream()
                .map(email -> UserHome.findByEmail(email, em))
                .filter(Objects::nonNull)
                .collect(toList()));

        QuestsDAO.update(quest, em);

        // Don't trigger an seo page capture update here; an admin change doesn't sound like it warrants it.
    }

    private static <T> Collection<T> emptyIfNull(final Collection<T> nullable) {
        return nullable == null ? emptyList() : nullable;
    }

    private void patchTaskImageUrl(final TaskCreateForm milestone, final Integer questId) {
        if (startsWith(milestone.getImageUrl(), "data:")) {
            try {
                milestone.setImageUrl(saveDataUrlImage(milestone.getImageUrl()).toExternalForm());
            } catch (final IOException e) {
                Logger.error("Unable to save image from Data URL for a new task of Quest " + questId, e);
                milestone.setImageUrl(null);
            }
        }
    }

     private String patchTaskImageUrl(final TaskEditForm milestone, final Integer questId) {
        if (startsWith(milestone.getImageUrl(), "data:")) {
            try {
                milestone.setImageUrl(saveDataUrlImage(milestone.getImageUrl()).toExternalForm());
            } catch (final IOException e) {
                Logger.error("Unable to save image from Data URL for a new task of Quest " + questId, e);
                milestone.setImageUrl(null);
            }
            
            return milestone.getImageUrl();
        }else {
        	return milestone.getImageUrl();
        }
    }
    
    @Transactional
    @JwtSessionLogin
    public Result newQuestImage() {
        Logger.debug("newQuestImage");

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        try {
            DynamicForm requestData = formFactory.form().bindFromRequest();
            String contentType = requestData.get("contentType");

            final List<S3File> questImageList = imageService.getFileFromRequest(request(), "questImage", ImageType.QUEST_IMAGE, contentType);
            if (questImageList == null) {
                return internalServerError("Cannot process uploaded image for quest.");
            }
            final S3FileHome s3FileHome = new S3FileHome(config);
            final EntityManager em = this.jpaApi.em();

            s3FileHome.saveUserMedia(questImageList, em);
            final String questImageUrl = s3FileHome.getUrl(questImageList.get(0), true).toString();

            final String queryQuestId = requestData.get("questId");
            if (isNumeric(queryQuestId)) {
                final Integer questId = QuestsDAO.addQuestPhotoByQuestId(valueOf(queryQuestId), user.getId(), questImageUrl, em);

                Logger.debug(format("successfully added a new Quest photo for [%s]", questId));
            }

            final ObjectNode result = Json.newObject();
            result.set("questImageURL", TextNode.valueOf(questImageUrl));
            return ok(result);
        } catch (final Exception ex) {
            Logger.error(ex.getMessage(), ex);
            return internalServerError();
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result newQuestImageWithUrl() {
        Logger.debug("newQuestImage");

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        try {
            DynamicForm requestData = formFactory.form().bindFromRequest();
            final EntityManager em = this.jpaApi.em();
            final String queryQuestId = requestData.get("questId");
            final String questImageUrl = requestData.get("photoUrl");
            if (isNumeric(queryQuestId)) {
                final Integer questId = QuestsDAO.addQuestPhotoByQuestId(valueOf(queryQuestId), user.getId(), questImageUrl, em);

                Logger.debug(format("successfully added a new Quest photo for [%s]", questId));
            }

            final ObjectNode result = Json.newObject();
            result.set("questImageURL", TextNode.valueOf(questImageUrl));
            return ok(result);
        } catch (final Exception ex) {
            Logger.error(ex.getMessage(), ex);
            return internalServerError();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result addPhotoToQuest() {
        Logger.debug("addPhotoToQuest");

        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        DynamicForm requestData = formFactory.form().bindFromRequest();
        String contentType = requestData.get("contentType");

        //List<S3File> questImageList = imageService.getFileFromRequest(request(), "questImage", ImageType.QUEST_IMAGE, contentType);
        
        final String questImage = requestData.get("questImage");
        
        if (questImage == null || questImage.length()==0) {
            return internalServerError("Unable to upload image.");
        }

        String questIdReq = requestData.get("questId");
        if (isBlank(questIdReq) || !isNumeric(questIdReq)) {
            return badRequest();
        }

        final String imageCaption = requestData.get("caption");
        S3FileHome s3FileHome = new S3FileHome(config);

        try {
           // s3FileHome.saveUserMedia(questImageList, em);
            Logger.info(format("addPhotoToQuest - saving new quest image for user [%d] for quest [%s] and url value [%s]", user.getId(), questIdReq, questImage));

            final QuestImage image = QuestImageDAO.addNewQuestImage(user.getId(), Integer.valueOf(questIdReq), questImage, imageCaption, em);
            //on success send a notification to quest creator that a new photo has been added to a quest
            final List<Integer> usersDoing = notificationService.getUserIdsSubscribedToQuest(QuestsDAO.findById(image.getQuestId(), em));
            AsyncUtils.processIdsAsync(config, usersDoing, userDoing -> notificationService.addQuestNotification(
                    userDoing,
                    PHOTO_VIDEO_ADDED,
                    user.getId(),
                    image.getQuestId()
            ));
            return ok(Json.toJson(QuestImageDTO.toDTO(image).withCreator(user)));
        } catch (final Exception e) {
            Logger.error("addPhotoToQuest - Quest photo saving failed", e);

            return internalServerError();
        }
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result removePhotoFromQuest(@NotNull Integer questPhotoId) {
        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        EntityManager em = this.jpaApi.em();

        QuestImageDAO.removeQuestPhotoById(questPhotoId, user.getId(), em);

        return ok(ResponseUtility.getSuccessMessageForResponse("removed quest photo"));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result addPhotoToTask() {
        Logger.debug("addPhotoToTask");

        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }

        DynamicForm requestData = formFactory.form().bindFromRequest();
        String contentType = requestData.get("contentType");

        List<S3File> imageDimensionsList = imageService.getFileFromRequest(request(), "taskImage", ImageType.TASK_IMAGE, contentType);
        if ((imageDimensionsList == null) || imageDimensionsList.isEmpty()) {
            return internalServerError("Unable to upload image.");
        }
        String taskId = requestData.get("taskId");
        if (isBlank(taskId) || !isNumeric(taskId)) {
            return badRequest();
        }

        S3FileHome s3FileHome = new S3FileHome(config);

        final QuestTasks questTask = QuestTasksDAO.findById(Integer.valueOf(taskId), em);

        if (questTask != null) {
            if (questService.canEditMilestone(questTask, user)) {
                try {
                    s3FileHome.saveUserMedia(imageDimensionsList, em);
                    Logger.info(format("addPhotoToTask - saving new task image for user [%d] for quest [%s] and url value [%s]", user.getId(), taskId, s3FileHome.getUrl(imageDimensionsList.get(0), true).toString()));

                    questTask.setImageUrl(String.valueOf(s3FileHome.getUrl(imageDimensionsList.get(0), true)));
                    em.persist(questTask);

                    final List<Integer> usersDoing = notificationService.getUserIdsSubscribedToQuest(QuestsDAO.findById(questTask.getQuestId(), em));
                    AsyncUtils.processIdsAsync(config, usersDoing, userDoing -> notificationService.addQuestNotification(
                            userDoing,
                            PHOTO_VIDEO_ADDED,
                            user.getId(),
                            questTask.getQuestId()
                    ));
                    return ok(s3FileHome.getUrl(imageDimensionsList.get(0), true).toString());
                } catch (final Exception e) {
                    Logger.error(e.getMessage(), e);

                    return internalServerError();
                }
            }
            return forbidden();
        }
        return badRequest();
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result removeMediaFromTask(@NotNull Integer taskId) {
        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        EntityManager em = this.jpaApi.em();

        final QuestTasks questTask = QuestTasksDAO.findById(taskId, em);
        if (questTask != null) {
            if (questService.canEditMilestone(questTask, user)) {
                questTask.setImageUrl(null);
                questTask.setLinkUrl(null);
                questTask.setLinkedQuestId(null);
                questTask.setVideo(null);
                em.merge(questTask);
                return ok(ResponseUtility.getSuccessMessageForResponse("removed task photo"));
            } else {
                return forbidden();
            }
        }
        return notFound();
    }


    @Transactional
    @JwtSessionLogin
    public Result searchResults() {
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());

        final StopWatch stopWatch = new StopWatch("Global search");

        if (user != null) {

            final List<SearchResponse> searchResponseList = new ArrayList<>();

            stopWatch.start("Global search :: Prepare users search");

            final List<UserSearchDTO> users = UserService.getAllUsersForSearch(em);

            stopWatch.stop();

            stopWatch.start("Global search :: Prepare current friends IDs");

            final List<Integer> friendIds = UserRelationshipDAO.getCurrentFriendsByUserId(user.getId(), em);

            stopWatch.stop();

            stopWatch.start("Global search :: Prepare pending friends IDs");

            final List<Integer> pendingFriendIds = UserRelationshipDAO.getPendingFriendsByUserId(user.getId(), em);

            stopWatch.stop();

            stopWatch.start("Global search :: Transform users");

            if (users != null && !users.isEmpty()) {
                // we dont want to show self
                users.removeIf(dto -> user.getId().equals(dto.getId()));
                for (UserSearchDTO userFromSearch : users) {
                    SearchResponse searchResponse = new SearchResponse();
                    searchResponse.type = "user";
                    searchResponse.userId = userFromSearch.getId();
                    searchResponse.userFullName = userFromSearch.getFirstName() + " " + userFromSearch.getLastName();
                    searchResponse.userName = userFromSearch.getUserName();
                    searchResponse.goals = userFromSearch.getMissionStatement();
                    searchResponse.profilePictureURL = userFromSearch.getAvatarUrl();
                    searchResponse.status = friendIds.contains(userFromSearch.getId()) ? "isAccepted" : pendingFriendIds.contains(userFromSearch.getId()) ? "isRequested" : null;

                    searchResponseList.add(searchResponse);
                }
            }

            stopWatch.stop();

            stopWatch.start("Global search :: Prepare Quests search");

            final QuestsListWithACL questsListAcl = QuestsDAO.getAllQuestsWithACL(em);

            stopWatch.stop();

            stopWatch.start("Global search :: Prepare Quests ACL");

            final List<Quests> quests = questsListAcl.getList(user);

            stopWatch.stop();

            stopWatch.start("Global search :: Transform Quests");

            if (quests != null) {
                for (Quests quest : quests) {
                    SearchResponse searchResponse = new SearchResponse();
                    searchResponse.type = "quest";
                    searchResponse.questId = quest.getId();
                    searchResponse.questName = quest.getTitle();
                    searchResponse.description = quest.getQuestFeed();
                    searchResponse.questImageURL = quest.getPhoto();
                    searchResponse.userId = quest.getCreatedBy();
                    searchResponse.userName = quest.getUser().getName();
                    searchResponse.pillar = upperCase(quest.getPillar());

                    searchResponseList.add(searchResponse);
                }
            }

            stopWatch.stop();

            try {
                stopWatch.start("Global search :: Convert to JSON");

                if (!searchResponseList.isEmpty()) {
                    return ok(Json.toJson(searchResponseList));
                } else {
                    return ok(Json.newArray());
                }
            } finally {
                stopWatch.stop();

                Logger.debug(stopWatch.prettyPrint());
            }
        }

        return ok(Json.newArray());
    }

    @Transactional
    public Result checkEmail() {
        final DynamicForm form = formFactory.form().bindFromRequest();
        final String email = form.get("email");
        if (isBlank(email)) {
            return badRequest();
        }
        final User user = UserService.getByEmail(lowerCase(email), jpaApi.em());

        Map<String, BooleanNode> response = new HashMap<>();
        response.put("exists", BooleanNode.getFalse());
        response.put("validated", BooleanNode.getFalse());
        response.put("active", BooleanNode.getFalse());
        if (user != null) {
        	LinkedAccount linkedAccount = user.getLinkedAccounts().stream().findFirst().get();
        	
        	for(LinkedAccount linkedAccountItr:user.getLinkedAccounts()) {
        		if("password".equalsIgnoreCase(linkedAccountItr.getProviderKey())) {
        			linkedAccount = linkedAccountItr;
        			break;
        		}
        	}
        	if("password".equalsIgnoreCase(linkedAccount.getProviderKey())){
            		response.put("exists", BooleanNode.getTrue());
            	if (user.getEmailValidated()) {
            		response.put("validated", BooleanNode.getTrue());
            	}

            	if (user.getActive()) {
            		response.put("active", BooleanNode.getTrue());
            	}
            }
        }

        return ok(Json.toJson(response));
    }

    @Transactional
    public Result getUserNameForUrl(@Nonnull Integer userId) {
        EntityManager em = this.jpaApi.em();
        User user = UserService.getById(userId, em);

        try {
            if (user.getUserName() != null && user.getUserName().contains("@")) {
                int emailIndex = user.getUserName().indexOf('@');
                String userNameUrl = URLEncoder.encode(user.getUserName().substring(0, emailIndex), "UTF-8");
                return ok(Json.toJson(userNameUrl));
            } else if (user.getUserName() != null) {
                String userNameUrl = URLEncoder.encode(user.getUserName(), "UTF-8");
                return ok(Json.toJson(userNameUrl));
            } else {
                return ok(NullNode.getInstance());
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.info("Application :: getUserNameForUrl : unable to encode user name => " + ex, ex);
            return forbidden();
        }
    }

    @Transactional
    public Result getUserIdByUsername(String username) {
        EntityManager em = this.jpaApi.em();
        Integer userId = UserService.getByUsername(decodeUsername(username), em);
        if (userId != null) {
            return ok(Json.toJson(userId));
        } else {
            return notFound();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result removeFriend() {

        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String friendId = requestData.get("friendId");

        if (friendId != null) {
            //check to ensure current friendship status (need to = 1)
            Integer status = UserRelationshipDAO.checkForFriendshipStatus(user.getId(), valueOf(friendId), em);

            Logger.info("status for friend is = " + status);

            if (status != null && status.equals(1)) {
                UserRelationship userRelationship = UserRelationshipDAO.getUserRelationshipByUserIdAndFriendId(user.getId(), valueOf(friendId), em);
                if (userRelationship != null) {
                    UserRelationshipDAO.remove(userRelationship, em);

                    return ok();
                } else {
                    ObjectNode wrapper = Json.newObject();
                    ObjectNode msg = Json.newObject();
                    msg.put("message", "error removing user relationship");
                    wrapper.set("error", msg);
                    return ok(wrapper);
                }
            }
        }

        ObjectNode wrapper = Json.newObject();
        ObjectNode msg = Json.newObject();
        msg.put("message", "error getting quests and users for search");
        wrapper.set("error", msg);
        return ok(wrapper);

    }

    @Transactional
    @JwtSessionLogin
    public Result isUserQuestSaved() {
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String questId = requestData.get("questId");
        EntityManager em = this.jpaApi.em();
        User user = this.userProvider.getUser(session());

        // check if current user is doing quest
        if (user != null && questId != null) {
            boolean isQuestSaved = QuestSavedDAO.doesQuestSavedExistForUser(valueOf(questId), user.getId(), em);

            if (isQuestSaved) {
                ObjectNode wrapper = Json.newObject();
                ObjectNode msg = Json.newObject();
                msg.put("message", "quest is saved for current user");
                msg.put("isQuestSaved", "true");
                wrapper.set("success", msg);
                return ok(wrapper);
            } else {
                ObjectNode wrapper = Json.newObject();
                ObjectNode msg = Json.newObject();
                msg.put("message", "quest is not saved for current user");
                msg.put("isQuestSaved", "false");
                wrapper.set("success", msg);
                return ok(wrapper);
            }

        } else {
            ObjectNode wrapper = Json.newObject();
            ObjectNode msg = Json.newObject();
            msg.put("message", "error finding user or quest for given id");
            wrapper.set("error", msg);
            return ok(wrapper);
        }

    }

    private static String decodeUsername(String username) {
        String removeSpecialChars = username.replaceAll("[+.^:,]", "");
        String removeMultipleWhiteSpaces = removeSpecialChars.replaceAll(" +", " ");
        try {
            return URLDecoder.decode(removeMultipleWhiteSpaces.trim(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new UnsupportedOperationException("cannot decode username: " + username);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getExploreFilters() {
        EntityManager em = this.jpaApi.em();

        List<ExploreCategories> exploreCategories = ExploreCategoriesDAO.findEnabledExploreCategories(em);
        List<ExplorePlaces> explorePlaces = ExplorePlacesDAO.findAllExplorePlaces(em);
        final List<String> pillars = config.getList("application.pillars").unwrapped().stream().map(Object::toString).collect(toList());

        ExploreDTO exploreDTO = new ExploreDTO();
        exploreDTO.setExploreCategories(exploreCategories);
        exploreDTO.setExplorePlaces(explorePlaces);
        exploreDTO.setExplorePillars(pillars);

        return ok(Json.toJson(exploreDTO));
    }

    // TODO: what is the point of this method?  I think it obsolete with us saving images in S3.  It could have been useful if users had image urls that were
    // internet sources.  Maybe they did at one point.
    @Deprecated
    @JwtSessionLogin(required = true)
    public Result reuploadUserProfilePictures() throws IOException {
        Logger.warn("reuploadUserProfilePictures");

        EntityManager em = this.jpaApi.em();
        final S3FileHome s3FileHome = new S3FileHome(config);
        ImageType imageType;
        S3File s3File;

        List<User> usersWithAvatars = userProvider.getUsersWithAvatars(em);
        for (User user : usersWithAvatars) {
            String filename = user.getProfilePictureOriginal();
            URL awsImageUrl = new URL(filename);
            byte[] imageByteArray = downloadFile(awsImageUrl);

            if (imageByteArray != null) {
                List<S3File> differentDimensionsImages = new ArrayList<>();
                for (ImageDimensions.Dimensions dimension : ImageDimensions.Dimensions.values()) {
                    s3File = new S3File();
                    imageType = ImageType.AVATAR;
                    ImageDimensions imageDimension = ImageDimensions.valueOf(imageType.name().concat(dimension.name()));

                    ImageProcessingService.ImageResult imgRes;
                    if ((imgRes = imageService.resizeByteArray(imageByteArray, imageDimension, null)) == null) {
                        break;
                    }
                    imageByteArray = imgRes.getData();

                    String dimensionName = dimension.isAddSuffix() ? dimension.name().toLowerCase() : "";
                    s3File.setName(dimensionName);
                    s3File.setContentData(new ByteArrayInputStream(imageByteArray));
                    s3File.setContentLength(imageByteArray.length);
                    s3File.setImgWidth(imgRes.getWidth());
                    s3File.setImgHeight(imgRes.getHeight());

                    differentDimensionsImages.add(s3File);
                }

                boolean updated = false;
                if (!differentDimensionsImages.isEmpty()) {
                    try {
                        s3FileHome.resaveUserMedia(differentDimensionsImages, filename, em);
                        UserHome.updateUserProfilePicture(user.getId(), s3FileHome.getUrl(differentDimensionsImages.get(0), true).toString(), em);
                        updated = true;
                    } catch (Exception e) {
                        Logger.error("reuploadUserProfilePictures - failed to re-upload dimensional profile picture for user => " + user.getEmail() + " => " + e, e);
                    }
                }

                if (updated) {
                    this.seoService.capturePageBackground(URLUtils.seoFriendlyUserProfilePath(user));
                    this.seoService.capturePageBackground(URLUtils.seoFriendlierUserProfilePath(user));
                }
            }
        }
        return ok();
    }

    // TODO: what is the point of this method?  I think it obsolete with us saving images in S3.  It could have been useful if users had image urls that were
    // internet sources.  Maybe they did at one point.
    @Deprecated
    @Transactional
    @JwtSessionLogin(required = true)
    public Result reuploadUserCoverPhotos() throws IOException {
        Logger.warn("reuploadUseCoverPhotos");

        EntityManager em = this.jpaApi.em();
        final S3FileHome s3FileHome = new S3FileHome(config);
        ImageType imageType;
        S3File s3File;

        List<User> usersWithCovers = userProvider.getUsersWithCover(em);
        for (User user : usersWithCovers) {
            String filename = user.getCoverPictureOriginal();
            URL awsImageUrl = new URL(filename);
            byte[] imageByteArray = downloadFile(awsImageUrl);

            List<S3File> differentDimensionsImages = new ArrayList<>();
            for (ImageDimensions.Dimensions dimension : ImageDimensions.Dimensions.values()) {
                s3File = new S3File();
                imageType = ImageType.AVATAR;
                ImageDimensions imageDimension = ImageDimensions.valueOf(imageType.name().concat(dimension.name()));

                ImageProcessingService.ImageResult imgRes;
                if ((imgRes = imageService.resizeByteArray(imageByteArray, imageDimension, null)) == null) {
                    break;
                }
                imageByteArray = imgRes.getData();

                String dimensionName = dimension.isAddSuffix() ? dimension.name().toLowerCase() : "";
                s3File.setName(dimensionName);
                s3File.setContentData(new ByteArrayInputStream(imageByteArray));
                s3File.setContentLength(imageByteArray.length);
                s3File.setImgWidth(imgRes.getWidth());
                s3File.setImgHeight(imgRes.getHeight());

                differentDimensionsImages.add(s3File);
            }

            boolean updated = false;
            if (!differentDimensionsImages.isEmpty() && imageByteArray != null) {
                try {
                    s3FileHome.resaveUserMedia(differentDimensionsImages, filename, em);
                    UserHome.uploadUserCoverPicture(user.getId(), s3FileHome.getUrl(differentDimensionsImages.get(0), true).toString(), em);
                    updated = true;
                } catch (Exception e) {
                    Logger.error("reuploadUserCoverPhotos - failed to re-upload dimensional cover picture for user => " + user.getEmail() + " => " + e, e);
                }
            }

            if (updated) {
                this.seoService.capturePageBackground(URLUtils.seoFriendlyUserProfilePath(user));
                this.seoService.capturePageBackground(URLUtils.seoFriendlierUserProfilePath(user));
            }
        }
        return ok();
    }

    private static byte[] downloadFile(URL url) {
        try {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            CloseableHttpResponse response = client.execute(new HttpGet(url.toExternalForm()));
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(inputStream, baos);
                return baos.toByteArray();
            } else {
                Logger.error("Response entity is null");
            }
        } catch (IOException | IllegalArgumentException e) {
            Logger.error("Application :: downloadFile : failed to download original file.", e.getCause());
        }
        return null;
    }
    
    @JwtSessionLogin(required = true)
    @Transactional
    public Result getActivityByQuestForAllTasks(final @NotNull Integer questId) {
    	
    	EntityManager em = this.jpaApi.em();
    	List<AsActivity> activites = activityService.getActivityByQuestForAllTasks(questId,em);
    	
    	return ok(Json.toJson(activites));
    }

    @JwtSessionLogin(required = true)
    @Transactional
    public Result getQuestActivitiesByQuest(final @NotNull Integer questId) {
    	EntityManager em = this.jpaApi.em();
    	List<QuestActivity> groups =  QuestActivityHome.getQuestActivitiesByQuest(questId,em);
    	
    	return ok(Json.toJson(groups));
    }

     private void updateMilstoneTask(final Quests quest,final List<TaskEditForm> taskeditforms, User user) {

		Date now  = new Date();
		final EntityManager em = this.jpaApi.em();
		Boolean activitiesWithARLId = true;
		
		List<TaskCreateForm> taskCreateFormList = new ArrayList();
		for (final TaskEditForm taskEditForm : taskeditforms) {
			if (taskEditForm.getId() == null) {
				
				TaskCreateForm questTask = new TaskCreateForm();
		           
					questTask.setVideo(taskEditForm.getVideo());
		            questTask.setTask(taskEditForm.getTask());
		            questTask.setLinkedQuestId(taskEditForm.getLinkedQuestId());
		            questTask.setPoint(taskEditForm.getPoint());
		            questTask.setRadiusInKm(taskEditForm.getRadiusInKm());
		           // questTask.setQuestTasksGroup(task.getGroupId() );
		            questTask.setImageUrl(taskEditForm.getImageUrl());
		            questTask.setLinkUrl(taskEditForm.getLinkUrl());
		            questTask.setTitle(taskEditForm.getTitle());
		            questTask.setGroupIndex(taskEditForm.getGroupIndex());
		            questTask.setActivitiesIds(taskEditForm.getActivitiesIds());
		            taskCreateFormList.add(questTask);
			}else {
			
			
			final QuestTasks milestone = QuestTasksDAO.findById(taskEditForm.getId(), em);
			activitiesWithARLId = true;
			if(milestone.getActivityRecordListId()==null && taskEditForm.getActivitiesIds()!=null && taskEditForm.getActivitiesIds().size()>0) {
				activitiesWithARLId = false;
				QuestTasksDAO questTaskDAO = new QuestTasksDAO();
				ActivityRecordList activityRecordList = QuestTasksDAO.newActivityRecordList(user.getId(), taskEditForm.getActivitiesIds(), em);
				milestone.setActivityRecordListId(activityRecordList.getId());		
			}
			
			milestone.setTask(taskEditForm.getTask());
			if (taskEditForm.getVideo() !=null) {
				milestone.setVideo(QuestTasksDAO.createEmbeddedVideo(taskEditForm.getVideo(), em));
			}else {
				milestone.setVideo(null);
			}
			
			//milestone.setTaskCompleted(taskEditForm.getTaskCompleted());
			milestone.setTaskCompletionDate(new Date());
			//milestone.setTimeHorizonEndDate(null);
			//milestone.setTimeHorizonStartDate(null);
			
			milestone.setLastModifiedBy(user.getId());
			milestone.setLastModifiedDate(now);
		//	milestone.setImageUrl(patchTaskImageUrl(taskEditForm,quest.getId()));
			milestone.setImageUrl(taskEditForm.getImageUrl());
			milestone.setLinkUrl(taskEditForm.getLinkUrl());
			milestone.setTitle(taskEditForm.getTitle());
			
			em.merge(milestone);
			Logger.info("MILESTONE EDITED " + Json.toJson(milestone));
		
			if(activitiesWithARLId) {
					if(taskEditForm.getActivitiesIds()!=null && taskEditForm.getActivitiesIds().size()>0) {
						updateTaskActivites(taskEditForm.getActivitiesIds(),milestone.getActivityRecordListId());
					}else {
						List<Integer> actvityRecordListIds = new ArrayList();
						actvityRecordListIds.add(milestone.getActivityRecordListId());
						List<ActivityRecord> activityRecords = 	activityService.getActivityIdsByActivityRecordList(actvityRecordListIds, em);
						if(activityRecords!=null && activityRecords.size()>0) {
							 List<Integer> actvivityRecordIds = new ArrayList();
							for(ActivityRecord activityRecord:activityRecords) {
									 actvivityRecordIds.add(activityRecord.getId());
							 } 
							activityService.deleteActivitiesById(actvivityRecordIds, em);
						}
					}
					
				}
			}
	}
		
		if(taskCreateFormList!=null && taskCreateFormList.size()>0) {
			Logger.info("CREATING NEW TASK WHILE UPDATING QUEST ");
				createQuestTasks(quest, user,taskCreateFormList);
		}
	}
    
	private void updateTaskActivites(List<Integer> updatedActivitityIdsList, Integer activityRecordListId) {
		
		List<Integer> activityRecordListIds = new ArrayList<Integer>();
		List<Integer> taskActvities = new Vector<Integer>();
		List<Integer> updatedActivitityIds = new Vector<Integer>();
		
		updatedActivitityIds.addAll(updatedActivitityIdsList);
		activityRecordListIds.add(activityRecordListId);
		
		final EntityManager em = this.jpaApi.em();
		List<ActivityRecord> activityRecords = 	activityService.getActivityIdsByActivityRecordList(activityRecordListIds, em);
		
		for(ActivityRecord activityRecord:activityRecords) {
			taskActvities.add(activityRecord.getActivityId());
		}
		
		if(updatedActivitityIds!=null && updatedActivitityIds.size()>0 && taskActvities!=null && taskActvities.size()>0) {
				
			final List<Integer> commonActivities  = updatedActivitityIds.stream().filter(taskActvities::contains).collect(Collectors.toList());
			  if(commonActivities!=null && commonActivities.size()>0) {
			     updatedActivitityIds.removeIf(x -> commonActivities.contains(x)); 
			     taskActvities.removeIf(x -> commonActivities.contains(x));
			  }
			 
			 updatedActivitityIds.forEach(s -> System.out.println("addedActivities>"+s));
			 
			 taskActvities.forEach(s -> System.out.println("removedActivities>"+s));
		}
		
		 if(updatedActivitityIds!=null && updatedActivitityIds.size()>0) {
			 activityService.createActivitiesByIdAndListId(updatedActivitityIds, activityRecordListId, em);
		 }    
		
		 if(taskActvities!=null && taskActvities.size()>0) {
			 List<Integer> actvivityRecordIds = new ArrayList();
			 for(ActivityRecord activityRecord:activityRecords) {
				 if(taskActvities.contains(activityRecord.getActivityId())) {
					 actvivityRecordIds.add(activityRecord.getId());
				 }
			 }
			 
			 activityService.deleteActivitiesById(actvivityRecordIds, em);
		 }
	
	}

public void addUpdateTaskGroup(List<TasksGroupManageEditForm> tasksGroups, Quests quest, User user) {
		
		List<TasksGroupManageEditForm> taskGroupsWithIds= new ArrayList();
		List<TasksGroupManageEditForm> taskGroupsWithoutIds= new ArrayList();
		if(tasksGroups!=null) {
			taskGroupsWithIds = new ArrayList();
			for(TasksGroupManageEditForm tasksGroupManageEditForm:tasksGroups) {
				if(tasksGroupManageEditForm.getId()!=null) {
					taskGroupsWithIds.add(tasksGroupManageEditForm);
				}else {
					taskGroupsWithoutIds.add(tasksGroupManageEditForm);
				}
			}
		}
		
		if(taskGroupsWithIds!=null && taskGroupsWithIds.size()>0) {
			taskGroupService.updateGroupNameById(taskGroupsWithIds);	
		}
		
		if(taskGroupsWithoutIds!=null && taskGroupsWithoutIds.size()>0) {
			taskGroupsWithoutIds.forEach(group->{
				 taskGroupService.createGroupWithGroupOrder(user, null, quest, group);
			});
			
		}
	}    

@Transactional
public Result activitiesForPillarsByUserId(final @NotNull Integer userId) {

   Map<String, List<AllPillarsCount>> pillars= taskGroupService.getActivityCountForPillarsByUserId(userId);
    
    return ok(Json.toJson(pillars));

 }

@Transactional
@JwtSessionLogin(required = true)
public Result getAttibutesForLogActivities(final @NotNull Integer activityId) {
   
	AsActivityAttributesDTO asActivityAttributesDTO = 	asActivityService.getAttributesAndUnitsByCategoryId(activityId);
    
	return ok(Json.toJson(asActivityAttributesDTO));		
 }




}
