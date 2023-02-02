package com.diemlife.controller;

import com.diemlife.acl.QuestEntityWithACL;
import com.diemlife.acl.VoterPredicate.VotingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.constants.LeaderboardMemberStatus;
import com.diemlife.constants.QuestEdgeType;
import com.diemlife.dao.*;
import com.diemlife.dto.*;
import forms.LeaderboardScoreForm;
import models.*;
import org.springframework.util.StopWatch;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import com.diemlife.security.JwtSessionLogin;
import com.diemlife.services.*;
import com.diemlife.utils.ResponseUtility;
import com.diemlife.utils.StreamUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.diemlife.acl.VoterPredicate.VotingResult.Abstain;
import static com.diemlife.acl.VoterPredicate.VotingResult.For;
import static com.diemlife.constants.QuestMode.SUPPORT_ONLY;
import static com.diemlife.constants.QuestMode.TEAM;
import static com.diemlife.dao.FundraisingSupplementDAO.getFundraisingSupplement;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static com.diemlife.utils.QuestSecurityUtils.canEditQuest;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;
import static com.diemlife.utils.URLUtils.seoFriendlyPublicQuestPath;

@JwtSessionLogin
public class QuestPageController extends Controller {

    private final JPAApi jpaApi;
    private final UserProvider userProvider;
    private final Config config;
    private final FundraisingLinkDAO fundraisingLinkDao;
    private final QuestMemberService questMemberService;
    private final LeaderboardService leaderboardService;
    private final LinkPreviewService linkPreviewService;
    private final QuestService questService;
    private final FormFactory formFactory;
    private final MilestoneService milestoneService;
    private final FundraisingService fundraisingService;
    private final PaymentTransactionFacade paymentTransactionFacade;
    // private final QuestCategoryService categoryService;
    
    private Database db;
    private Database dbRo;

    @Inject
    public QuestPageController(
                               Database db,
                               @NamedDatabase("ro") Database dbRo,
                               final JPAApi api,
                               final UserProvider provider,
                               final Config config,
                               final FundraisingLinkDAO fundraisingLinkDao,
                               final QuestMemberService questMemberService,
                               final LeaderboardService leaderboardService,
                               final LinkPreviewService linkPreviewService,
                               final QuestService questService,
                               final FormFactory formFactory,
                               MilestoneService milestoneService,
                               final FundraisingService fundraisingService,
                               final PaymentTransactionFacade paymentTransactionFacade
                               //,
                               // final QuestCategoryService categoryService
    ) {
        this.db = db;
        this.dbRo = dbRo;
        this.jpaApi = api;
        this.userProvider = provider;
        this.config = config;
        this.fundraisingLinkDao = fundraisingLinkDao;
        this.questMemberService = questMemberService;
        this.leaderboardService = leaderboardService;
        this.linkPreviewService = linkPreviewService;
        this.questService = questService;
        this.formFactory = formFactory;
        this.milestoneService = milestoneService;
        //this.categoryService = categoryService;
        this.fundraisingService = fundraisingService;
        this.paymentTransactionFacade = paymentTransactionFacade;
    }

    @Transactional
    @JwtSessionLogin
    public Result questPageDetails(final @Nonnull Integer questId, final Integer userId) {
        final StopWatch watch = new StopWatch();

        watch.start();

        if (userId == null) {
            return badRequest();
        }

        final EntityManager em = this.jpaApi.em();
        final User doer = UserService.getById(userId, em);
        if (doer == null) {
            return notFound();
        }
        Brand company = UserHome.getCompanyForUser(doer, this.jpaApi);

        return doIfEligibleToView(questId, (quest, viewer) -> {
            final QuestPageDetailDTO dto = buildQuestPageData(viewer, doer, quest);
            final JsonNode result = Json.toJson(dto);
            watch.stop();

            Logger.debug(format("successfully retrieved all details for quest [%s] in %sms", quest.getId(), watch.getTotalTimeMillis()));
            return ok(ResponseUtility.getBrandForUser((ObjectNode) result,company));
        });
    }

    public QuestPageDetailDTO buildQuestPageData(final User viewer, final User doer, final Quests quest) {
        final EntityManager em = this.jpaApi.em();
        final Integer questId = quest.getId();
        {
            // Quest Detail
            boolean isQuestSaved = false;
            boolean isFollowedByViewer = false;

            final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());

            // TODO: factor this out into some shared thing
            // Check if parent quest
            long _questId = (long) questId.intValue();
            List<QuestEdge> children;
            QuestEdge edgeChild = null;
            QuestTeam questDefaultTeam = null;
            try (Connection c = dbRo.getConnection()) {
                QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
                children = qeDao.getEdgesByType(c, _questId, QuestEdgeType.CHILD.toString());
                edgeChild = qeDao.getQuestForEdge(c, _questId, QuestEdgeType.CHILD.toString());
                questDefaultTeam = new QuestTeamDAO(em).getDefaultTeam(quest);
            } catch (Exception e) {
                Logger.error("buildQuestPageData - error with edges", e);
                children = new LinkedList<QuestEdge>();
            } 
            boolean isMegaQuest = !children.isEmpty();
            boolean isParentQuest = (edgeChild == null) ? false : true;
            boolean isParentTeamQuest = false;

            // Mega-Quest
            final List<QuestTasks> linkedQuestTasks = Optional.ofNullable(viewer)
                    .map(user -> QuestTasksDAO.getLinkedQuestTasks(questId, em))
                    .orElse(emptyList());

            final List<QuestLiteDTO> megaQuests = new ArrayList<>();

            if (isParentQuest) {
                QuestLiteDTO edgeParentQuest = null;
                Quests edgeSrcQuests = QuestsDAO.findById((int) edgeChild.getQuestSrc(), em);
                User questCreator = UserService.getById(edgeSrcQuests.getCreatedBy(), em);
                edgeParentQuest = QuestLiteDTO.toDTO(edgeSrcQuests).withSeoSlugs(publicQuestSEOSlugs(edgeSrcQuests, questCreator, envUrl));
                if (!(edgeParentQuest == null)) {
                    megaQuests.add(edgeParentQuest);
                }

                // check if parent quest is a default team parent or a child parent.CHILD PARENT is when team is_default = false
                final QuestTeamDTO team = questMemberService.getQuestTeam(quest, doer, true);
                if (team != null) {
                    isParentTeamQuest = true;
                }
            }
            final QuestEdge _edChild = edgeChild;
            megaQuests.addAll(linkedQuestTasks.stream()
                .map(task -> QuestsDAO.findById(task.getQuestId(), em))
                .filter(Objects::nonNull)
                .filter(StreamUtils.distinctByKey(Quests::getId))
                .filter(mq -> {
                    if (isParentQuest && _edChild != null) {
                        return !(mq.getId() == _edChild.getQuestSrc());
                    }

                    return  true;
                })
                .map(megaQuest -> QuestLiteDTO.toDTO(megaQuest).withSeoSlugs(publicQuestSEOSlugs(megaQuest, viewer, envUrl)))
                .collect(toList()));

            // Quest Photos
            final List<QuestImageDTO> questImages = QuestImageDAO.getQuestImagesForQuest(questId, em)
                    .stream()
                    .map(image -> QuestImageDTO.toDTO(image).withCreator(UserService.getById(image.getUserId(), em)))
                    .collect(toList());
            //Milestones
            final List<MilestoneDTO> questTasks = questService.listMilestonesForQuest(quest, doer).stream()
                    .map(task -> milestoneService.convertToDto(task, envUrl, linkPreviewService))
                    .collect(toList());
            //Repeatable info
            final QuestActivityDTO doerActivityInfo = QuestActivityHome.getRepeatableInfoForQuestAndDoer(quest, doer, em);
            final QuestActivityDTO viewerActivityInfo = QuestActivityHome.getRepeatableInfoForQuestAndDoer(quest, viewer, em);

            List<Attribute> attributes;
            try (Connection c = dbRo.getConnection()) {
                AttributesDAO attDao = AttributesDAO.getInstance();
                attributes = attDao.getAttributesByQuestId(c, _questId);
            } catch (Exception e) {
                Logger.error("buildQuestPageData - error with attributes", e);
                attributes = new LinkedList<Attribute>();
            }

            // populate leaderboard attributes
            try (Connection c = dbRo.getConnection()) {
                LeaderboardDAO lDao = new LeaderboardDAO(em);

                // Grab attributes
                List<LeaderboardAttribute> leaderboardAttributes = lDao.getLeaderboardAttributeByQuest(c, _questId);
                Set<String> hiddenSlugs = lDao.getHiddenLeaderboardSlugsForQuest(c, _questId);
                Map<String, Integer> slugOrder = lDao.getLeaderboardSlugOrderForQuest(c, _questId);

                // Use string -- num appear lexigraphically before text anyway
                TreeMap<String, LeaderboardAttribute> _leaderboardAttributes = new TreeMap<String, LeaderboardAttribute>();

                for (LeaderboardAttribute attribute : leaderboardAttributes) {
                    String slug = attribute.getId();

                    // Strip out hidden attrbutes
                    if (!hiddenSlugs.contains(slug)) {
                        // Add them according to ordering, if any
                        Integer ordinal = slugOrder.get(slug);
                        _leaderboardAttributes.put(((ordinal == null) ? slug : ordinal.toString()), attribute);
                    }
                }

                quest.setLeaderboardAttributes(new LinkedList<LeaderboardAttribute>(_leaderboardAttributes.values()));
            } catch (SQLException e) {
                Logger.error("buildQuestPageData - unbale to fetch leaderboard for quest: " + questId, e);
            }

            final Happening event = new HappeningDAO(em).getHappeningByQuestId(questId);
            final QuestTeamMember doerTeamMember = new QuestTeamDAO(em).getTeamMember(quest, doer);
            final QuestTeamMember viewerTeamMember = new QuestTeamDAO(em).getTeamMember(quest, viewer);

            QuestMapView questMapView = (quest.getQuestMapViewId() != null) ? new QuestMapViewDAO(em).findQuestMapViewById(quest.getQuestMapViewId()) : null;

            //Shareable URLs
            final String questSeoUrl;
            final boolean hasDoerIndividualFundraiser = fundraisingLinkDao.existsWithQuestAndFundraiserUser(quest, doer);
            final boolean isViewerInDefaultTeam = Optional.ofNullable(viewerTeamMember).map(QuestTeamMember::getTeam).map(QuestTeam::isDefaultTeam).orElse(false);
            final boolean isDoerInDefaultTeam = Optional.ofNullable(doerTeamMember).map(QuestTeamMember::getTeam).map(QuestTeam::isDefaultTeam).orElse(false);
            if (hasDoerIndividualFundraiser) {
                questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, doer);
            } else if (quest.getMode().equals(SUPPORT_ONLY)) {
                questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, quest.getUser());
            } else if (quest.getMode().equals(TEAM)) {
                if (doerTeamMember == null || isDoerInDefaultTeam) {
                    questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, quest.getUser());
                } else {
                    questSeoUrl = seoFriendlyTeamQuestUrl(doerTeamMember.getTeam(), envUrl);
                }
            } else {
                questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, doer);
            }
            //check if quest is already saved for the user
            final Integer viewerId = Optional.ofNullable(viewer).map(User::getId).orElse(null);
            if (viewerId != null) {
                isQuestSaved = QuestSavedDAO.doesQuestSavedExistForUser(questId, viewerId, em);
                isFollowedByViewer = new QuestUserFlagDAO(em).isFollowedQuestForUser(questId, viewerId);
            }
            //TODO re-enable when categories get reintroduced
//            QuestCategory category = categoryService.findHighestRankedCategory(questId);
//            if (category == null) {
//                categoryService.classify(questId); //TODO make async
//                category = from(quest);
//            }

            final boolean hasCreatorStripeAccount = quest.getUser().getStripeEntity() instanceof StripeAccount;
            final boolean hasDoerStripeAccount = doer.getStripeEntity() instanceof StripeAccount;
            final boolean hasEvent = event != null;
            // call the function that returns true false for ticket purchase for viewer
            final HappeningDTO happening = new HappeningDTO(event);
            final boolean isDoerPurchasedTickets = hasEvent && new HappeningParticipantDAO(jpaApi.em()).hasEventRegistered(questId, viewerId);
            final boolean isRegisterInProgress = Optional.ofNullable(happening.getHappeningRegisterStatus())
                                                         .map(registerEvent -> registerEvent.getStatus().equals(HappeningRegisterStatusDTO.RegisterState.REGISTER_PROGRESS))
                                                         .orElse(true);
            final boolean isRegisterButtonEnabled = !isDoerPurchasedTickets && isRegisterInProgress;
            final boolean isBackingAllowed = quest.isFundraising() ? hasCreatorStripeAccount : hasDoerStripeAccount;
            final boolean hasMapRoute = !new QuestMapRouteDAO(jpaApi.em()).findAllQuestMapRoutesByQuest(questId).isEmpty();
            final boolean isTeamView = doerTeamMember != null
                    && !doerTeamMember.getTeam().isDefaultTeam()
                    && doerTeamMember.getMember().getId().equals(doerTeamMember.getTeam().getCreator().getId());
            final FundraisingLinkDAO fundraisingDao = new FundraisingLinkDAO(jpaApi);
            final Brand brand = UserHome.getCompanyForUser(doer, this.jpaApi);
            final QuestPageDetailDTO dto = new QuestPageDetailDTO(quest, doer,
                    doerActivityInfo, viewerActivityInfo,
                    questImages, questTasks,
                    questSeoUrl, (quest.getVideo() == null ? null : quest.getVideo().url),
                    isQuestSaved, isFollowedByViewer, isBackingAllowed, quest.isBackBtnDisabled(),
                    hasEvent,
                    isTeamView,
                    viewerTeamMember == null || isViewerInDefaultTeam || !viewerTeamMember.isActive() ? null : viewerTeamMember.getTeam().getId(),
                    doerTeamMember == null || isDoerInDefaultTeam || !doerTeamMember.isActive() ? null : doerTeamMember.getTeam().getId(),
                    happening,
                    QuestCategoryDTO.toDTO(null, envUrl),
                    megaQuests,
                    attributes, 
                    questMapView, 
                    hasMapRoute,
                    isDoerPurchasedTickets,
                    isRegisterButtonEnabled,
                    isMegaQuest,
                    isParentTeamQuest,
                    questDefaultTeam == null ? null : QuestTeamDTO.from(questDefaultTeam),
                    fundraisingDao.existsWithQuestAndFundraiserUser(quest, doer),
                    brand);

            // Ensures that the viewer sees the proper custom values when a creator is non-profit and/or absorbing fees
            boolean isTeamPage = doerTeamMember != null && doerTeamMember.getMember().getId().equals(doerTeamMember.getTeam().getCreator().getId());
            if (isTeamPage) {
                updateTeamsIfNonProfits(quest.getUser(), dto);
            }

            QuestsDAO.incrementViewCountByQuestId(quest, em);

            return dto;
        }
    }

    private void updateTeamsIfNonProfits(User creator, QuestPageDetailDTO dto) {
        if (creator.isUserNonProfit()) {
            dto.getUser().setUserNonProfit(true);
        }
        if (creator.isAbsorbFees()) {
            dto.getUser().setAbsorbFees(true);
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result questPageMembers(final @NotNull Integer questId, final @NotNull Integer doerId) {
        final EntityManager em = this.jpaApi.em();
        final User doer = UserService.getById(doerId, em);
        if (doer == null) {
            return notFound();
        }
        return doIfEligibleToView(questId, (quest, viewer) -> {
            final Collection<QuestMemberDTO> members = questMemberService.getQuestMembers(quest, doer);
            return ok(Json.toJson(members));
        });
    }

    @Transactional
    @JwtSessionLogin
    public Result questPageTeams(final @NotNull Integer questId) {
        return doIfEligibleToView(questId, (quest, viewer) -> {
            final Collection<QuestTeamDTO> teams = questMemberService.getQuestTeams(quest);
            return ok(Json.toJson(teams));
        });
    }


    private static class FundraisingSummary {
        private final Long amount;
        private final int count;

        private FundraisingSummary(final Long amount, final int count) {
            this.amount = amount;
            this.count = count;
        }

        private static FundraisingSummary empty() {
            return new FundraisingSummary(0L, 0);
        }

        private FundraisingSummary add(final FundraisingSummary delta) {
            Long amount = this.amount + delta.amount;
            return new FundraisingSummary(amount, this.count + delta.count);
        }
    }

    public FundraisingSummary calculateCurrentFundraisingAmount(final FundraisingLinkDTO dto, final StripeAccount beneficiary) {
        //TODO stripeSummary left commented out unless users are to absorb fees, if this is the case, remove tip from stripeSummary
        final List<FundraisingTotalDTO> totals = paymentTransactionFacade.getQuestBackingFundraisingTotals(dto);

        final FundraisingSummary backingSummary = totals.stream()
                .map(total -> new FundraisingSummary((long) total.getAmount(), 1))
                .reduce(FundraisingSummary.empty(), FundraisingSummary::add);

        return backingSummary;
    }

    // Helper function to try and capture logic that might need to be run multiple times to sum up values for a parent quest
    public boolean fundraisingLinkHelper(FundraisingLinkDTO originalDto, FundraisingLinkDTO dto, int questId, int doerId) {
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

    public QuestPageTeamDetailDTO buildQuestTeamPageData(final User viewer, final User doer, final Quests quest) {
        final EntityManager em = this.jpaApi.em();
        final Integer questId = quest.getId();
        {
            final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());

            // TODO: factor this out into some shared thing
            // Check if parent quest
            long _questId = (long) questId.intValue();
            QuestTeam questDefaultTeam = null;
            try (Connection c = dbRo.getConnection()) {
                QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
                questDefaultTeam = new QuestTeamDAO(em).getDefaultTeam(quest);
            } catch (Exception e) {
                Logger.error("buildQuestTeamPageData - error with edges", e);
            }

            final QuestTeamMember doerTeamMember = new QuestTeamDAO(em).getTeamMember(quest, doer);

            //Shareable URLs
            final String questSeoUrl;
            final boolean hasDoerIndividualFundraiser = fundraisingLinkDao.existsWithQuestAndFundraiserUser(quest, doer);
            final boolean isDoerInDefaultTeam = Optional.ofNullable(doerTeamMember).map(QuestTeamMember::getTeam).map(QuestTeam::isDefaultTeam).orElse(false);
            if (hasDoerIndividualFundraiser) {
                questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, doer);
            } else if (quest.getMode().equals(SUPPORT_ONLY)) {
                questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, quest.getUser());
            } else if (quest.getMode().equals(TEAM)) {
                if (doerTeamMember == null || isDoerInDefaultTeam) {
                    questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, quest.getUser());
                } else {
                    questSeoUrl = seoFriendlyTeamQuestUrl(doerTeamMember.getTeam(), envUrl);
                }
            } else {
                questSeoUrl = envUrl + seoFriendlyPublicQuestPath(quest, doer);
            }

            final boolean isTeamView = doerTeamMember != null
                    && !doerTeamMember.getTeam().isDefaultTeam()
                    && doerTeamMember.getMember().getId().equals(doerTeamMember.getTeam().getCreator().getId());

//            QuestPageTeamDetailDTO dto = new QuestPageTeamDetailDTO();
            FundraisingLinkDTO fundDto = fundraisingService.getFundraisingLinkDTO(questId, doer.getId());

            QuestPageTeamDetailDTO dto = new QuestPageTeamDetailDTO(quest.getTitle(), doer,
                    questSeoUrl,
                    isTeamView,
                    questDefaultTeam == null ? null : QuestTeamDTO.from(questDefaultTeam),
                    fundDto == null ? 0 : fundDto.currentAmount);

            if (fundDto == null || (fundDto != null && fundDto.id == null)) {
                return dto;
            }

            if (!fundraisingLinkHelper(fundDto, fundDto, questId, doer.getId())) {
                return dto;
            }

            dto.amountBacked = fundDto.currentAmount;

            return dto;
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result questPageParentQuestTeams(final @NotNull Integer questId, final Integer userId) {
        long _questId = (long) questId.intValue();
        List<QuestEdge> children;
        try (Connection c = dbRo.getConnection()) {
            QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
            children = qeDao.getEdgesByType(c, _questId, QuestEdgeType.CHILD.toString());
        } catch (Exception e) {
            Logger.error("questPageParentQuestTeams - error with edges", e);
            children = new LinkedList<QuestEdge>();
        }

        final EntityManager em = this.jpaApi.em();
        ArrayList<QuestPageTeamDetailDTO> tqList = new ArrayList<>();
        User user = UserService.getById(userId, em);
        for (QuestEdge edge : children) {
            Integer childQuestId = (int) edge.getQuestDst();
            final Quests quest = QuestsDAO.findById(childQuestId, em);
            final User doer = UserService.getById(quest.getCreatedBy(), em);
            if (!(quest == null) && !(user == null)) {
                tqList.add(buildQuestTeamPageData(user, doer, quest));
            }
        }

        tqList.sort((o1, o2) -> o1.amountBacked.compareTo(o2.amountBacked));
        Collections.reverse(tqList);

        final JsonNode result = Json.toJson(tqList);

        return ok(result);
    }

    @Transactional
    @JwtSessionLogin
    public Result questPageTeam(final @NotNull Integer questId) {
        final String teamName = request().getQueryString("team-name");
        final EntityManager em = this.jpaApi.em();
        return doIfEligibleToView(questId, (quest, viewer) -> {
            final QuestTeam questTeam = new QuestTeamDAO(em).getTeam(quest, teamName);
            if (questTeam == null) {
                return notFound();
            } else {
                return ok(Json.toJson(QuestTeamDTO.from(questTeam)));
            }
        });
    }

    @Transactional
    @JwtSessionLogin
    public Result questTeam(final @NotNull Long teamId) {
        final QuestTeamDTO team = questMemberService.getTeam(teamId);
        return ok(Json.toJson(team));
    }

    @Transactional(readOnly = true)
    @JwtSessionLogin
    public Result leaderboardScores(final @NotNull Integer questId, final @NotNull String attributeSlug) {
        // Note: this is stripping out hidden users
        return doIfEligibleToView(questId, (quest, viewer)
                -> ok(Json.toJson(leaderboardService.getLeaderboardLocal(quest, attributeSlug, true, false, false, false))));
    }

    @Transactional
    @JwtSessionLogin
    public Result patchLeaderboardScore(final @NotNull Integer questId, final @NotNull String attributeSlug) {
        final Form<LeaderboardScoreForm> formBinding = formFactory.form(LeaderboardScoreForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final LeaderboardScoreForm form = formBinding.get();
        return doIfEligibleToView(questId, (quest, viewer) -> {
            final EntityManager em = this.jpaApi.em();
            final LeaderboardDAO leaderboardDao = new LeaderboardDAO(em);

            LeaderboardAttribute attribute;
            try (Connection c = dbRo.getConnection()) {
                attribute = leaderboardDao.getLeaderboardAttribute(c, attributeSlug);
            } catch (SQLException e) {
                Logger.error("patchLeaderboardScore - unbale to fetch leaderboard for quest: " + questId, e);
                attribute = null;
            }

            final LeaderboardMember member = leaderboardDao.getMember(form.getMemberId());
            if (attribute == null || member == null) {
                return notFound();
            }
            if (canEditQuest(quest, viewer) || isViewerLeaderboardMember(member, viewer)) {
                final LeaderboardMemberStatus status = Stream.of(LeaderboardMemberStatus.values())
                        .filter(value -> value.name().equals(form.getStatus()))
                        .findFirst()
                        .orElse(null);

                LeaderboardScore patchedScore;
                try (Connection c = db.getConnection()) {
                    final Integer value = form.getValue();
                    patchedScore = leaderboardDao.setLeaderboardScore(c, attribute, member, value, status);
                    LeaderboardDAO.updateLeaderboardScore(c, member.id.intValue(), attributeSlug, value, false);
                } catch (SQLException e) {
                    Logger.error("patchLeaderboardScore - unbale to set leaderboard score for member: " + member.getId());
                    patchedScore = null;
                }

                return ok(Json.toJson(LeaderboardService.toDto(patchedScore, attribute, member)));
            } else {
                return forbidden();
            }
        });
    }

    @Transactional
    @JwtSessionLogin
    public Result updateLeaderboardMembers(final @NotNull Integer questId) {
        return doIfEligibleToView(questId, (quest, viewer) -> {
            if (canEditQuest(quest, viewer)) {
                final EntityManager em = this.jpaApi.em();
                final long membersUpdated = QuestActivityHome.getUsersAffiliatedWithQuest(questId, em)
                        .stream()
                        .map(leaderboardService::initializeLeaderboardMemberIfNotPresent)
                        .map(IdentifiedEntity::getId)
                        .distinct()
                        .peek(id -> Logger.debug(format("Updated leaderboard member %s for Quest %s", id, questId)))
                        .count();
                return ok(Json.toJson(membersUpdated));
            } else {
                return forbidden();
            }
        });
    }

    @Transactional(readOnly = true)
    public Result checkFundraiserCampaignName() {
        return ok(BooleanNode.valueOf(this.fundraisingLinkDao.existsWithCampaignName(request().body().asText())));
    }

    @Transactional(readOnly = true)
    public Result checkQuestTeamName(final @NotNull Integer questId) {
        final EntityManager em = this.jpaApi.em();
        return doIfEligibleToView(questId, (quest, viewer) -> {
            final QuestTeam team = new QuestTeamDAO(em).getTeam(quest, request().body().asText());
            return ok(BooleanNode.valueOf(team != null));
        });
    }

    private Result doIfEligibleToView(final Integer questId, final BiFunction<Quests, User, Result> function) {
        final User viewer = userProvider.getUser(session());
        final EntityManager em = this.jpaApi.em();
        final Quests quest = QuestsDAO.findById(questId, em);
        if (quest == null) {
            return notFound();
        }
        final AtomicReference<Quests> questSupplier = new AtomicReference<>(quest);
        final QuestEntityWithACL questAcl = new QuestEntityWithACL(questSupplier::get, em);

        final VotingResult eligible = questAcl.eligible(viewer);
        if (For.equals(eligible)) {
            return function.apply(quest, viewer);
        } else if (Abstain.equals(eligible)) {
            return unauthorized();
        } else {
            return forbidden();
        }
    }

    private boolean isViewerLeaderboardMember(final LeaderboardMember member, final User viewer) {
        return viewer != null
                && member.getPlatformUser() != null
                && member.getPlatformUser().getId().equals(viewer.getId());
    }

    /*
    private QuestCategory from(Quests quest) {
        QuestCategory questCategory = new QuestCategory();
        questCategory.setConfidence(100);
        questCategory.setCategory(quest.getPillar());
        questCategory.setQuestId(quest.getId());

        return questCategory;
    }
    */

}
