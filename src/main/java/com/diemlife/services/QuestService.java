package com.diemlife.services;

import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
importcom.diemlife.constants.Interests;
import com.diemlife.constants.PromptType;
import com.diemlife.constants.QuestCreatorTypes;
import com.diemlife.constants.QuestMode;
import com.diemlife.dao.AsActivityDAO;
import com.diemlife.dao.FundraisingLinkDAO;
import com.diemlife.dao.PromptDAO;
import com.diemlife.dao.QuestActivityHome;
import com.diemlife.dao.QuestImageDAO;
import com.diemlife.dao.QuestSavedDAO;
import com.diemlife.dao.QuestTasksDAO;
import com.diemlife.dao.QuestTasksGroupDAO;
import com.diemlife.dao.QuestTeamDAO;
import com.diemlife.dao.QuestUserFlagDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dto.ActivityExportDTO;
import com.diemlife.dto.AsActivityDTO;
import com.diemlife.dto.AsCommentsDTO;
import com.diemlife.dto.AsLikesDTO;
import com.diemlife.dto.LeaderboardMaxActivityDTO;
import com.diemlife.dto.LogActivityDTO;
import com.diemlife.dto.PaymentPersonalInfoDTO;
import com.diemlife.dto.QuestLiteDTO;
import com.diemlife.dto.QuestPermissionsDTO;
import com.diemlife.dto.TransactionExportDTO;
import com.diemlife.exceptions.QuestOperationForbiddenException;
import com.diemlife.exceptions.RequiredParameterMissingException;
import forms.CommentsForm;
import forms.LogActivityForm;
import forms.QuestActionPointForm;
import forms.QuestTeamInfoForm;
import forms.TaskCreateForm;
import forms.TasksGroupForm;
import com.diemlife.models.ActivityRecord;
import com.diemlife.models.AsActivity;
import com.diemlife.models.AsActivityRecordValue;
import com.diemlife.models.FundraisingLink;
import com.diemlife.models.LeaderboardMember;
import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestEvents;
import com.diemlife.models.QuestRecommendation;
import com.diemlife.models.QuestTaskCompletionHistory;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTasksGroup;
import com.diemlife.models.QuestTeam;
import com.diemlife.models.QuestTeam2;
import com.diemlife.models.QuestTeamMember;
import com.diemlife.models.QuestUserFlag;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;
import play.Logger;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import com.diemlife.utils.URLUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static com.diemlife.constants.PromptType.PromptEventType.AT_QUEST_COMPLETE;
import static com.diemlife.constants.PromptType.PromptEventType.AT_QUEST_START;
import static com.diemlife.constants.PromptType.PromptEventType.AT_QUEST_START_IMMEDIATE;
import static com.diemlife.constants.QuestActivityStatus.COMPLETE;
import static com.diemlife.constants.QuestActivityStatus.IN_PROGRESS;
import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.SUPPORT_ONLY;
import static com.diemlife.constants.QuestMode.TEAM;
import static com.diemlife.dao.QuestActivityHome.getQuestActivityForQuestIdAndUser;
import static com.diemlife.dao.QuestEventHistoryDAO.addEventHistory;
import static com.diemlife.dao.QuestSavedDAO.doesQuestSavedExistForUser;
import static com.diemlife.dao.QuestSavedDAO.saveQuestForUser;
import static com.diemlife.dao.QuestsDAO.findById;
import static forms.QuestTeamInfoForm.TeamAction.Create;
import static forms.QuestTeamInfoForm.TeamAction.Join;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static com.diemlife.models.QuestEvents.QUEST_CANCEL;
import static com.diemlife.models.QuestEvents.QUEST_COMPLETE;
import static com.diemlife.models.QuestEvents.QUEST_RESTART;
import static com.diemlife.models.QuestEvents.QUEST_SAVE;
import static com.diemlife.models.QuestEvents.QUEST_START;
import static com.diemlife.models.QuestEvents.QUEST_SUPPORT;
import static org.apache.commons.lang.StringUtils.upperCase;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static com.diemlife.utils.QuestSecurityUtils.canCheckMilestone;
import static com.diemlife.utils.QuestSecurityUtils.canEditQuest;
import static com.diemlife.utils.QuestSecurityUtils.canManageTasksInQuest;
import static com.diemlife.utils.URLUtils.getImageUrl;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;

@Singleton
public class QuestService {

    private static final String PARAM_QUEST = "quest";
    private static final String PARAM_DOER = "doer";

    private static final PromptDAO PROMPT_DAO = PromptDAO.getInstance();

    private final JPAApi jpaApi;
    private final Config config;
    private final SeoService seoService;
    private final FundraisingLinkDAO fundraisingLinkDao;
    private final MilestoneService milestoneService;
    private final LeaderboardService leaderboardService;
    private final ActivityService activityService;

    @Inject
    public QuestService(final JPAApi jpaApi, final Config config, final SeoService seoService, final FundraisingLinkDAO fundraisingLinkDao, MilestoneService milestoneService,
                        LeaderboardService leaderboardService, ActivityService activityService) {
        this.jpaApi = jpaApi;
        this.config = config;
        this.seoService = seoService;
        this.fundraisingLinkDao = fundraisingLinkDao;
        this.milestoneService = milestoneService;
        this.leaderboardService = leaderboardService;
        this.activityService = activityService;
    }

    @Transactional
    public boolean startQuest(final Connection c,
                              final Quests quest,
                              final User referrer,
                              final User doer,
                              final @Nullable QuestMode questMode,
                              final @Nullable QuestTeamInfoForm form,
                              final @Nullable QuestActionPointForm point) {
        if (quest == null) {
            throw new RequiredParameterMissingException(PARAM_QUEST);
        }
        if (doer == null) {
            throw new RequiredParameterMissingException(PARAM_DOER);
        }

        int questId = quest.getId();
        String username = doer.getUserName();

        final EntityManager em = jpaApi.em();
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, doer, em);
        final QuestMode requestedMode = questMode == null ? quest.getMode() : questMode;
        final QuestTeamDAO questTeamDao = new QuestTeamDAO(em);
        final QuestTeamMember currentTeamMember = questTeamDao.getTeamMember(quest, doer, false);
        final boolean isLeavingDefaultTeam = isLeavingDefaultTeam(currentTeamMember, requestedMode);
      
        // Grab the current team assigment for the doer if any and if it is not default or inactive
        QuestTeam currentAssignedTeam = ((currentTeamMember == null || !currentTeamMember.isActive()) ? null : currentTeamMember.getTeam());
        Long currentAssignedTeamId = ((currentAssignedTeam == null || currentAssignedTeam.isDefaultTeam()) ? null : currentAssignedTeam.getId());

        if (isAlreadyStarted(activity) && !isLeavingDefaultTeam) {
            Logger.warn(format("Quest with ID [%s] already has already been started for user '%s'", questId, doer.getEmail()));
            return false;
        }

        QuestSavedDAO.removeQuestForUser(questId, doer.getId(), em);

        final QuestUserFlagDAO questUserFlagDAO = new QuestUserFlagDAO(em);
        if (questUserFlagDAO.isFollowedQuestForUser(questId, doer.getId())) {
            Logger.debug(format("Quest with ID [%s] already is already followed by user '%s'", questId, doer.getEmail()));
        } else {
            final QuestUserFlag followFlag = questUserFlagDAO.followQuestForUser(quest, doer);
            if (followFlag != null && followFlag.flagValue) {
                Logger.info(format("Following Quest with ID [%s] by user '%s'", questId, doer.getEmail()));
            } else {
                Logger.warn(format("Failed to follow Quest with ID [%s] by user '%s'", questId, doer.getEmail()));
            }
        }

        // Ensure user is on leaderboard
        if (quest.isLeaderboardEnabled()) {
            LeaderboardMember lm = leaderboardService.initializeLeaderboardMemberIfNotPresent(quest, doer);
            Logger.debug("startQuest - added leaderboard member: " + lm.id);
        }

        switch (quest.getMode()) {
            case SUPPORT_ONLY:
                if (quest.getCreatedBy().equals(doer.getId())) {
                    createNewQuestActivity(quest, doer, PACE_YOURSELF, QUEST_START, point);

                    Logger.info(format("Quest with ID [%s] started in '%s' mode by its creator '%s'", questId, quest.getMode(), doer.getEmail()));
                } else {
                    createNewQuestActivity(quest, doer, SUPPORT_ONLY, QUEST_SUPPORT, point);

                    Logger.info(format("Quest with ID [%s] started in '%s' mode by new supporter '%s'", questId, SUPPORT_ONLY.getKey(), doer.getEmail()));
                }

                // Record event for activity feed
                activityService.questStarted(questId, username, currentAssignedTeamId);

                seoService.capturePageBackground(URLUtils.seoFriendlyPublicQuestPath(quest, doer));
                return true;

            case PACE_YOURSELF:
                if (quest.getCreatedBy().equals(doer.getId())) {
                    createNewQuestActivity(quest, doer, PACE_YOURSELF, QUEST_START, point);

                    Logger.info(format("Quest with ID [%s] started in '%s' mode by its creator '%s'", questId, quest.getMode(), doer.getEmail()));
                } else {
                    final QuestMode validMode = requestedMode != null && asList(PACE_YOURSELF, SUPPORT_ONLY).contains(requestedMode) ? requestedMode : PACE_YOURSELF;
                    final QuestEvents validEvent = SUPPORT_ONLY.equals(validMode) ? QUEST_SUPPORT : QUEST_START;

                    createNewQuestActivity(quest, doer, validMode, validEvent, point);

                    if (PACE_YOURSELF.equals(validMode)) {
                        cloneMilestonesForDoer(quest, doer);
                    }

                    Logger.info(format("Quest with ID [%s] started in '%s' mode by new doer '%s'", questId, validMode.getKey(), doer.getEmail()));
                }

                Logger.info(format("Start Quest :: Checking milestones from mega Quest [%s] for doer [%s]", questId, doer.getId()));
                QuestTasksDAO.getLinkedQuestTasks(questId, em).forEach(milestone -> {
                    final QuestTaskCompletionHistory completion = milestoneService.checkMilestone(milestone, doer, false, point);
                    if (completion.isGeoPointInArea()) {
                        if (PROMPT_DAO.pushPromptToUser(c, doer, quest, AT_QUEST_START_IMMEDIATE, "Ready for new adventures?", PromptType.YES_NO_ID)) {
                            Logger.info(format("Start Quest immediate prompt pushed to user %s", doer.getUserName()));
                        }
                        if (PROMPT_DAO.pushPromptToUser(c, doer, quest, AT_QUEST_START, "Still motivated?", PromptType.TRUE_FALSE_ID)) {
                            Logger.info(format("Start Quest postponed prompt pushed to user %s", doer.getUserName()));
                        }
                    }
                });

                // Record event for activity feed
                activityService.questStarted(questId, username, currentAssignedTeamId);

                seoService.capturePageBackground(URLUtils.seoFriendlyPublicQuestPath(quest, doer));
                return true;

            case TEAM:
                QuestTeam questTeam;
                if (quest.getCreatedBy().equals(doer.getId())) {
                    createNewQuestActivity(quest, doer, TEAM, QUEST_START, point);

                    Logger.info(format("Creating default team for Quest [%s] started by its creator '%s'", questId, doer.getEmail()));

                    final QuestTeam team = questTeamDao.createTeam(quest, doer, getDefaultTeamName(quest), null, true, false);
                    if (questTeamDao.joinTeam(team, doer)) {
                        Logger.info(format("Default team '%s' created for Quest [%s] ", team.getName(), questId));
                    }

                    Logger.info(format("Quest with ID [%s] started in '%s' mode by its creator '%s'", questId, quest.getMode(), doer.getEmail()));

                    seoService.capturePageBackground(URLUtils.seoFriendlyPublicQuestPath(quest, doer));
                } else {
                    final QuestMode validMode = TEAM;
                    final boolean requestedIndividual = PACE_YOURSELF.equals(requestedMode);
                    final QuestTeam questReferrerTeam = Optional.ofNullable(referrer).flatMap(user -> questTeamDao.listTeamsForQuest(quest, false, false).stream()
                            .filter(team -> user.getId().equals(team.getCreator().getId()))
                            .findFirst())
                            .orElse(patchNonExistingDefaultTeam(quest, validMode, requestedIndividual));
                    final QuestTeamInfoForm teamForm = rebuildForm(form, questReferrerTeam, requestedIndividual);
                    if (currentTeamMember != null && !currentTeamMember.isActive()
                            && currentTeamMember.getTeam().getId().equals(Optional.ofNullable(teamForm).map(QuestTeamInfoForm::getQuestTeamId).orElse(currentTeamMember.getTeam().getId()))) {

                        // already mapped to a team for this quest
                        questTeam = currentTeamMember.getTeam();

                        if (questTeam.isIndividualTeam() && !questTeam.getCreator().getId().equals(doer.getId())) {
                            Logger.warn(format("Not adding doer with ID [%s] to individual team with ID [%s]", doer.getId(), questTeam.getId()));
                        } else {
                            // Reactivate the membership
                            // TODO: this is not most efficient way of doing this since we already have QuestTeamMember object
                            questTeamDao.leaveAllTeams(doer, quest);
                            questTeamDao.joinTeam(questTeam, doer);

                            Logger.debug(format("Reactivated membership for doer with ID [%s] in a team with ID [%s]", doer.getId(), questTeam.getId()));
                        }
                    } else {
                        // not currently mapped to a team for this quest
                        if (teamForm != null) {
                            questTeamDao.leaveAllTeams(doer, quest);
                            questTeam = joinOrCreateTeam(quest, doer, teamForm, requestedIndividual);

                            Logger.debug(format("Created new membership for doer with ID [%s] in a team with ID [%s]", doer.getId(), questTeam.getId()));
                        } else {
                            questTeam = questTeamDao.listTeamsForQuest(quest, false, true).stream().filter(QuestTeam::isDefaultTeam).findFirst().get();
                        }
                    }
                    if (activity == null) {
                        createNewQuestActivity(quest, doer, TEAM, QUEST_START, point);
                        if (questTeam.getCreator().getId().equals(doer.getId())) {
                            cloneMilestonesForDoer(quest, doer);

                            Logger.info(format("Team Quest [%s] started by the team '%s' creator '%s' - copying milestones", questId, questTeam.getName(), questTeam.getCreator().getEmail()));
                        } else {
                            Logger.info(format("Team Quest [%s] started by the team '%s' creator '%s' - skipping milestones", questId, questTeam.getName(), questTeam.getCreator().getEmail()));
                        }
                    } else {
                        QuestActivityHome.changeQuestActivityMode(activity, TEAM, em);
                        Logger.info(format("Switching activity on Quest with ID [%s] to '%s' mode for doer '%s'", questId, validMode.getKey(), doer.getEmail()));
                    }

                    // Record event for activity feed
                    activityService.questStarted(questId, username, questTeam.getId());

                    Logger.info(format("Team Quest with ID [%s] started in '%s' mode by new doer '%s'", questId, validMode.getKey(), doer.getEmail()));

                    seoService.capturePageBackground(URLUtils.relativeTeamUrl(questTeam));
                }

                return true;

            default:
                return false;
        }
    }

    private QuestTeam patchNonExistingDefaultTeam(final Quests quest, final QuestMode requestedMode, final boolean individual) {
        final QuestTeamDAO dao = new QuestTeamDAO(jpaApi.em());
        if (dao.listTeamsForQuest(quest, false, false).isEmpty() && TEAM.equals(quest.getMode()) && TEAM.equals(requestedMode) && !quest.isFundraising()) {
            return dao.createTeam(quest, quest.getUser(), getDefaultTeamName(quest), null, true, individual);
        } else {
            return null;
        }
    }

    private static String getDefaultTeamName(final Quests quest) {
        return format("%s's team for %s", quest.getUser().getFirstName(), quest.getTitle());
    }

    private QuestTeam joinOrCreateTeam(final Quests quest, final User doer, final QuestTeamInfoForm team, final boolean individual) {
        final QuestTeam questTeam;
        final QuestTeamDAO questTeamDao = new QuestTeamDAO(jpaApi.em());
        int questId = quest.getId();
        Integer _questId = questId;
        String username = doer.getUserName();

        if (team.getQuestTeamId() == null) {
            if (isBlank(team.getQuestTeamName())) {
                Logger.warn(format("None of team ID or team name are specified when starting Quest [%s] by '%s'", questId, doer.getEmail()));
                questTeam = null;
            } else {
                Logger.info(format("Creating new team '%s' when starting Quest [%s] by '%s'", team.getQuestTeamName(), questId, doer.getEmail()));
                questTeam = questTeamDao.createTeam(quest, doer, team.getQuestTeamName(), team.getQuestTeamLogoUrl(), false, individual);

                // Record event for activity feed
                activityService.teamCreated(questId, username, questTeam.getId());
            }
        } else {
            questTeam = questTeamDao.load(team.getQuestTeamId(), QuestTeam.class);
            if (_questId.equals(questTeam.getQuest().getId())) {
                Logger.info(format("Found existing team '%s' for Quest [%s]", questTeam.getName(), questId));
            } else {
                throw new IllegalStateException(format("Team with ID [%s] doesn't belong to Quest with ID [%s]", team.getQuestTeamId(), questId));
            }
        }
        if (questTeam != null) {
            final boolean joined = questTeamDao.joinTeam(questTeam, doer);
            if (joined) {
                // Record event for activity feed
                activityService.teamJoined(questId, username, questTeam.getId());

                Logger.info(format("User '%s' joined the team '%s' by starting the Quest [%s]", doer.getEmail(), questTeam.getName(), questId));
            }
        }
        return questTeam;
    }

    private static boolean isAlreadyStarted(final QuestActivity activity) {
        return activity != null && asList(IN_PROGRESS, COMPLETE).contains(activity.getStatus());
    }

    private static boolean isLeavingDefaultTeam(final QuestTeamMember teamMember, final QuestMode requestedMode) {
        final boolean isCurrentlyInDefaultTeam = Optional.ofNullable(teamMember)
                .filter(QuestTeamMember::isActive)
                .map(QuestTeamMember::getTeam)
                .map(QuestTeam::isDefaultTeam)
                .orElse(false);
        return TEAM.equals(requestedMode) && isCurrentlyInDefaultTeam;
    }

    private static QuestTeamInfoForm rebuildForm(final QuestTeamInfoForm originalForm, final QuestTeam referrerTeam, final boolean individual) {
        if (originalForm == null) {
            return buildFormFromReferrerTeam(referrerTeam);
        } else {
            if (Create.equals(originalForm.getQuestTeamAction()) && isBlank(originalForm.getQuestTeamName())) {
                return buildFormFromReferrerTeam(referrerTeam);
            } else if (Join.equals(originalForm.getQuestTeamAction()) && originalForm.getQuestTeamId() == null) {
                return buildFormFromReferrerTeam(referrerTeam);
            } else if (originalForm.getQuestTeamAction() == null && originalForm.getQuestTeamId() == null && !individual) {
                return buildFormFromReferrerTeam(referrerTeam);
            } else if (originalForm.getQuestTeamAction() == null && individual) {
                return buildIndividualForm(originalForm);
            } else {
                return originalForm;
            }
        }
    }

    private static QuestTeamInfoForm buildFormFromReferrerTeam(final QuestTeam referrerTeam) {
        if (referrerTeam == null) {
            return null;
        } else {
            final QuestTeamInfoForm rebuildForm = new QuestTeamInfoForm();
            rebuildForm.setQuestTeamAction(Join);
            rebuildForm.setQuestTeamId(referrerTeam.getId());
            return rebuildForm;
        }
    }

    private static QuestTeamInfoForm buildIndividualForm(final QuestTeamInfoForm originalForm) {
        if (originalForm == null) {
            return null;
        } else {
            final QuestTeamInfoForm rebuiltForm = new QuestTeamInfoForm();
            rebuiltForm.setQuestTeamAction(Create);
            rebuiltForm.setQuestTeamName(originalForm.getQuestTeamName());
            return rebuiltForm;
        }
    }

    private void createNewQuestActivity(final Quests quest,
                                        final User doer,
                                        final QuestMode mode,
                                        final QuestEvents event,
                                        final QuestActionPointForm point) {
        final EntityManager em = jpaApi.em();
        QuestActivityHome.startQuestForUser(quest.getId(), doer.getId(), mode, em);
        addEventHistory(quest.getId(), doer.getId(), event, quest.getId(), point, em);
    }

    private QuestTasks createNewLinkMilestone(final Quests megaQuest,
                                              final Quests linkedQuest,
                                              final QuestTasks linkMilestone,
                                              final User user) {
        final EntityManager em = jpaApi.em();
        final QuestTasks newTask = QuestTasksDAO.addNewTask(user, user, megaQuest, new TaskCreateForm(linkMilestone.getTask()), em);
        if (newTask != null) {
            newTask.setLinkedQuestId(linkedQuest.getId());
            newTask.setPoint(linkMilestone.getPoint());
            newTask.setRadiusInKm(linkMilestone.getRadiusInKm());
            newTask.setPinUrl(linkMilestone.getPinUrl());
            newTask.setPinCompletedUrl(linkMilestone.getPinCompletedUrl());
            newTask.setCreatedBy(linkMilestone.getCreatedBy());
        }
        return em.merge(newTask);
    }

    private void saveQuest(final Quests quest, final User user) {
        final EntityManager em = jpaApi.em();
        saveQuestForUser(quest, user, em);
        addEventHistory(quest.getId(), user.getId(), QUEST_SAVE, quest.getId(), em);
    }

    public boolean completeQuest(final Connection c,
                                 final Connection cRo,
                                 final Quests quest,
                                 final User doer,
                                 final boolean completeMilestones,
                                 final QuestActionPointForm point) {
        return completeQuest(c, cRo, quest, doer, completeMilestones, point, true);
    }

    @Transactional
    public boolean completeQuest(final Connection c,
                                 final Connection cRo,
                                 final Quests quest,
                                 final User doer,
                                 final boolean completeMilestones,
                                 final QuestActionPointForm point, boolean logActivityFeed) {
        if (quest == null) {
            throw new RequiredParameterMissingException(PARAM_QUEST);
        }
        if (doer == null) {
            throw new RequiredParameterMissingException(PARAM_DOER);
        }
        int questId = quest.getId();
        String username = doer.getUserName();
        int userId = doer.getId();
        String email = doer.getEmail();

        final EntityManager em = jpaApi.em();
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, doer, em);
        if (activity == null || !IN_PROGRESS.equals(activity.getStatus())) {
            Logger.warn(format("Won't complete - Quest with ID [%s] not started by user '%s'", questId, email));

            return false;
        }

        QuestActivityHome.completeQuestForUser(quest, doer, em);

        if (completeMilestones) {
            if (SUPPORT_ONLY.equals(quest.getMode()) && !canEditQuest(quest, doer)) {
                Logger.warn(format("Complete milestones rejected - Support only Quest with ID [%s] not editable for user '%s'", questId, email));
            } else if (TEAM.equals(quest.getMode()) && !canEditQuest(quest, doer)) {
                Logger.warn(format("Complete milestones rejected - Team Quest with ID [%s] not editable for user '%s'", questId, email));
            } else {
                Logger.info(format("Completing Quest with ID [%s] started by user '%s' with closing unchecked milestones", questId, email));

                listMilestonesForQuest(quest, doer).forEach(milestone -> {
                    milestoneService.checkMilestone(milestone, doer, true, point, logActivityFeed);
                    Optional.ofNullable(milestone.getLinkedQuestId())
                            .map(linkedQuestId -> QuestsDAO.findById(linkedQuestId, em))
                            .ifPresent(linkedQuest -> completeQuest(c, cRo, linkedQuest, doer, true, point, logActivityFeed));
                });
            }
        } else {
            Logger.info(format("Completing Quest with ID [%s] started by user '%s'", questId, email));
        }

        addEventHistory(questId, doer.getId(), QUEST_COMPLETE, questId, point, em);

        Logger.info(format("Complete Quest :: Checking milestones from mega Quest [%s] for doer [%s]", questId, userId));
        QuestTasksDAO.getLinkedQuestTasks(questId, em).forEach(milestone -> {
            final QuestTaskCompletionHistory completion = milestoneService.checkMilestone(milestone, doer, true, point, logActivityFeed);
            if (completion.isGeoPointInArea()) {
                if (PROMPT_DAO.pushPromptToUser(c, doer, quest, AT_QUEST_COMPLETE, "Was it an exciting adventure?", PromptType.SCALE_OF_ONE_TO_FIVE_ID)) {
                    Logger.info(format("Complete Quest prompt pushed to user %s", username));
                }
            }
        });

        Double lat;
        Double lon;
        if (point == null) {
            lat = null;
            lon = null;
        } else {
            lat = point.getLatitude().doubleValue();
            lon = point.getLongitude().doubleValue();
        }

        if (logActivityFeed) {
            // Record event for activity feed
            QuestTeam2 questTeam = QuestTeamDAO.getActiveTeamByQuestAndUser(cRo, questId, userId);
            activityService.questCompleted(questId, username, ((questTeam == null) ? null : (long) questTeam.getId()), lat, lon);
        }

//        update quest with fundraise and back btn disable
//        quest.setFundraising(false);
//        quest.setBackBtnDisabled(true);
//        quest.setMultiTeamsEnabled(false);
//        QuestsDAO.update(quest, em);

        return true;
    }

    @Transactional
    public boolean cancelQuest(Connection cRo, final Quests quest, final User doer) {
        if (quest == null) {
            throw new RequiredParameterMissingException(PARAM_QUEST);
        }
        if (doer == null) {
            throw new RequiredParameterMissingException(PARAM_DOER);
        }
        int questId = quest.getId();
        String username = doer.getUserName();
        long userId = doer.getId();

        final EntityManager em = jpaApi.em();
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, doer, em);
        if (activity == null || !IN_PROGRESS.equals(activity.getStatus())) {
            Logger.warn(format("CancelQuest :: Quest [%s] not in progress for user [%s]", questId, userId));
            return false;
        }
        if (TEAM.equals(activity.getMode())) {
            final QuestTeamDAO dao = new QuestTeamDAO(em);
            final QuestTeamMember member = dao.getTeamMember(quest, doer);
            if (member == null) {
                Logger.warn(format("CancelQuest :: Doer '%s' is not member of a Quest [%s] team", userId, questId));
            } else {
                dao.leaveAllTeams(doer, quest);
            }
        }

        QuestActivityHome.removeAllQuestActivity(questId, doer.getId(), em);
        QuestTasksDAO.getQuestTasksByQuestIdAndUserId(questId, doer.getId(), em).forEach(milestone -> {
            if (quest.getCreatedBy().equals(milestone.getUserId())) {
                Logger.info(format("CancelQuest :: Uncheck milestone for Quest [%s] creator [%s]", milestone.getQuestId(), milestone.getUserId()));

                QuestTasksDAO.setTaskCompleted(milestone, doer, false, em);
            } else {
                if (PACE_YOURSELF.equals(activity.getMode())) {
                    QuestTasksDAO.remove(milestone, em);

                    Logger.info(format("CancelQuest :: Removing milestone copy for Quest [%s] and doer [%s]", questId, milestone.getUserId()));
                } else {
                    Logger.debug(format("CancelQuest :: Skipping milestone modification for Quest [%s] and doer [%s]", questId, milestone.getUserId()));
                }
            }
        });
        saveQuestForUser(quest, doer, em);

        QuestTasksDAO.getLinkedQuestTasks(questId, em).forEach(milestone -> {
            Logger.info(format("CancelQuest :: Removing milestone from mega Quest [%s] and doer [%s]", milestone.getQuestId(), milestone.getUserId()));

            QuestTasksDAO.remove(milestone, em);
        });

        if (quest.isFundraising()) {
            final FundraisingLink fundraisingLink = fundraisingLinkDao.stopFundraisingForQuest(quest, doer);
            if (fundraisingLink != null && !fundraisingLink.active) {
                Logger.info("User [%s] stopped fundraising for Quest [%s] via link [%s]", userId, questId, fundraisingLink.getId());
            }
        }

        addEventHistory(questId, doer.getId(), QUEST_CANCEL, questId, em);

        // Record event for activity feed
        QuestTeam2 questTeam = QuestTeamDAO.getActiveTeamByQuestAndUser(cRo, questId, userId);
        activityService.questCanceled(questId, username, ((questTeam == null) ? null : (long) questTeam.getId()));

        return true;
    }

    public List<QuestLiteDTO> getRecommendedQuests(final int limit) {
        final StopWatch timer = new StopWatch("Get recommended Quests");
        timer.start();

        final EntityManager em = jpaApi.em();
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        final List<QuestLiteDTO> result = em.createQuery("SELECT r FROM QuestRecommendations r WHERE r.active = TRUE", QuestRecommendation.class)
                .getResultList()
                .stream()
                .filter(recommendation -> recommendation.active)
                .map(recommendation -> recommendation.quest)
                .map(quest -> QuestLiteDTO.toDTO(quest).withSeoSlugs(publicQuestSEOSlugs(quest, quest.getUser(), envUrl)))
                .collect(toList());
        Collections.shuffle(result);

        timer.stop();
        Logger.debug(timer.toString());

        return result.size() > limit
                ? result.subList(0, limit)
                : result;
    }

    @Transactional
    public QuestTasks addMilestoneToMegaQuest(final Connection cRo, final Quests megaQuest, final Quests linkedQuest, final User user) {
        Logger.debug(format("Adding milestone to mega Quest :: Mega Quest [%s], linked Quest [%s], doer [%s]", megaQuest.getId(), linkedQuest.getId(), user.getId()));

        final EntityManager em = jpaApi.em();

        final QuestActivity megaQuestActivity = getQuestActivityForQuestIdAndUser(megaQuest, user, em);
        if (megaQuestActivity == null) {
            Logger.info(format("Adding milestone to mega Quest :: User [%s] doesn't have any activity on Mega Quest [%s] - Staring mega Quest", user.getId(), megaQuest.getId()));

            createNewQuestActivity(megaQuest, user, PACE_YOURSELF, QUEST_START, null);
        } else if (!IN_PROGRESS.equals(megaQuestActivity.getStatus())) {
            Logger.error(format("Adding milestone to mega Quest :: User [%s] status on Mega Quest [%s] is [%s]", user.getId(), megaQuest.getId(), megaQuestActivity.getStatus().name()));

            throw new IllegalStateException("Mega Quest is not in progress");
        }

        em.flush();

        final QuestActivity linkedQuestActivity = getQuestActivityForQuestIdAndUser(linkedQuest, user, em);
        final boolean isLinkedQuestSaved = doesQuestSavedExistForUser(linkedQuest.getId(), user.getId(), em);
        final QuestTasks creatorsLinkMilestone = QuestTasksDAO.getLinkedQuestTaskForMegaQuest(megaQuest.getId(), linkedQuest.getId(), megaQuest.getCreatedBy(), em);
        if (creatorsLinkMilestone == null) {
            throw new IllegalStateException(format("Trying to link Quest with ID %s that is not defined in mega-Quest with ID %s", linkedQuest.getId(), megaQuest.getId()));
        }
        final QuestTasks doersLinkMilestone = QuestTasksDAO.getLinkedQuestTaskForMegaQuest(megaQuest.getId(), linkedQuest.getId(), user.getId(), em);
        if (linkedQuestActivity == null && !isLinkedQuestSaved && doersLinkMilestone == null) {
            Logger.info(format("Adding milestone to mega Quest :: Saving linked Quest [%s] and creating new link milestone for user [%s]", linkedQuest.getId(), user.getId()));

            saveQuest(linkedQuest, user);

            return createNewLinkMilestone(megaQuest, linkedQuest, creatorsLinkMilestone, user);
        } else if (linkedQuestActivity == null && isLinkedQuestSaved && doersLinkMilestone == null) {
            Logger.info(format("Adding milestone to mega Quest :: Creating new link milestone for Quest [%s] and user [%s]", linkedQuest.getId(), user.getId()));

            return createNewLinkMilestone(megaQuest, linkedQuest, creatorsLinkMilestone, user);
        } else if (linkedQuestActivity != null && doersLinkMilestone == null) {
            Logger.info(format("Adding milestone to mega Quest :: Cancelling Quest [%s] Creating new link milestone for user [%s]", linkedQuest.getId(), user.getId()));

            cancelQuest(cRo, linkedQuest, user);

            return createNewLinkMilestone(megaQuest, linkedQuest, creatorsLinkMilestone, user);
        } else if (linkedQuestActivity == null && !isLinkedQuestSaved) {
            Logger.info(format("Adding milestone to mega Quest :: Saving linked Quest [%s] for user [%s]", linkedQuest.getId(), user.getId()));

            saveQuest(linkedQuest, user);

            return doersLinkMilestone;
        } else {
            Logger.info(format("Adding milestone to mega Quest :: Linked milestone already exists for user [%s]", user.getId()));

            return doersLinkMilestone;
        }
    }

    /**
     * Copies given Quest replicating
     * - milestones
     * - milestone videos
     * - gallery images
     *
     * @param quest      Quest to copy
     * @param newDoer    New Quest doer
     * @param copyImages Should copy image gallery
     * @return Copy of the original Quest
     */
    public Quests copyQuestForUser(final Quests quest, final User newDoer, final boolean copyImages) {
        final EntityManager em = jpaApi.em();
        final Quests newQuest = QuestsDAO.createQuest(
                "Copy of " + quest.getTitle(),
                quest.getQuestFeed(),
                quest.getShortDescription(),
                Interests.valueOf(upperCase(quest.getPillar())),
                newDoer.getId(),
                quest.getCreatedBy(),
                true,
                quest.isBackBtnDisabled(),
                quest.isEditableMilestones(),
                quest.isMilestoneControlsDisabled(),
                quest.isTaskViewDisabled(),
                quest.isMultiTeamsEnabled(),
                1,
                quest.getPrivacyLevel(),
                QuestCreatorTypes.valueOf(upperCase(quest.getType())),
                quest.isFundraising(),
                quest.getPhoto(),
                quest.getMode(),
                em
        );

        QuestTasksDAO.getQuestTasksByQuestIdAndUserId(quest.getId(), quest.getCreatedBy(), em)
                .forEach(milestone -> milestoneService.copyMilestone(milestone, newQuest, newDoer));

        if (copyImages) {
            QuestImageDAO.getQuestImagesForQuest(quest.getId(), em)
                    .forEach(image -> QuestImageDAO.addNewQuestImage(newDoer.getId(), newQuest.getId(), image.getQuestImageUrl(), image.getCaption(), em));
        }

        return newQuest;
    }

    @Transactional
    public boolean repeatQuest(final Quests quest,
                               final User user,
                               final QuestActionPointForm point,
                               final boolean resetTasks) {
        final EntityManager em = jpaApi.em();
        final boolean repeated = QuestActivityHome.repeatQuestForUser(quest, user, em);
        if (repeated) {

            // Ensure user is on leaderboard
            if (quest.isLeaderboardEnabled()) {
                LeaderboardMember lm = leaderboardService.initializeLeaderboardMemberIfNotPresent(quest, user);
                Logger.debug("startQuest - added leaderboard member: " + lm.id);
            }

            QuestTasksDAO.getQuestTasksByQuestIdAndUserId(quest.getId(), user.getId(), em).forEach(milestone -> {
                if (resetTasks) {
                    milestoneService.checkMilestone(milestone, user, false, point);
                    Logger.info("resetting tasks for quest "+ quest.getId()); 
                }
                Optional.ofNullable(milestone.getLinkedQuestId())
                        .map(linkedQuestId -> QuestsDAO.findById(linkedQuestId, em))
                        .ifPresent(linkedQuest -> repeatQuest(linkedQuest, user, point, resetTasks));
            });
            addEventHistory(quest.getId(), user.getId(), QUEST_RESTART, quest.getId(), point, em);

            Logger.info(format("Repeat Quest :: Un-checking milestones from mega Quest [%s] for doer [%s]", quest.getId(), user.getId()));
            QuestTasksDAO.getLinkedQuestTasks(quest.getId(), em).forEach(milestone -> milestoneService.checkMilestone(milestone, user, false, point));

            return true;
        } else {
            return false;
        }
    }

    /**
     * Toggles the milestone completeness status taking into account the milestone permissions of Quest mode
     *
     * @param c         Connection
     * @param milestone Milestone to switch status
     * @param doer      User that switches the milestone status
     * @return Toggling result: true if toggle successful, false otherwise
     */
    @Transactional
    public boolean toggleMilestoneCompletion(final Connection c, final Connection cRo, final @Nonnull QuestTasks milestone, final @Nonnull User doer, final QuestActionPointForm point) {
        final EntityManager em = jpaApi.em();

        final Quests quest = QuestsDAO.findById(milestone.getQuestId(), em);
        if (quest == null || quest.isMilestoneControlsDisabled()) {
            Logger.info(format("Won't toggle milestone - Quest with ID [%s] has disabled milestones controls", milestone.getQuestId()));
            return false;
        }
        final QuestActivity questActivity = getQuestActivityForQuestIdAndUser(quest, doer, em);
        if (questActivity == null) {
            Logger.info(format("Won't toggle milestone - no activity found for Quest with ID [%s] and user [%s]", quest.getId(), doer.getId()));
            return false;
        } else if (!questActivity.getStatus().equals(IN_PROGRESS)) {
            Logger.info(format("Won't toggle milestone - Quest with ID [%s] is not in progress for user [%s]", quest.getId(), doer.getId()));
            return false;
        }
        final boolean targetCompletionState = !toBoolean(milestone.getTaskCompleted());
        switch (questActivity.getMode()) {
            case SUPPORT_ONLY:
                Logger.warn(format("Won't toggle milestone - Activity for Quest with ID [%s] started in support only mode for user [%s]", quest.getId(), doer.getId()));
                return false;
            case PACE_YOURSELF:
                if (canCheckMilestone(milestone, doer)) {
                    if (milestone.getLinkedQuestId() == null) {
                        milestoneService.checkMilestone(milestone, doer, targetCompletionState, point);
                    } else {
                        final Quests linkedQuest = QuestsDAO.findById(milestone.getLinkedQuestId(), em);
                        final QuestActivity linkedQuestActivity = getQuestActivityForQuestIdAndUser(linkedQuest, doer, em);
                        if (targetCompletionState) {
                            if (linkedQuestActivity == null) {
                                return startQuest(c, linkedQuest, linkedQuest.getUser(), doer, PACE_YOURSELF, null, point)
                                        && completeQuest(c, cRo, linkedQuest, doer, false, point);
                            } else if (COMPLETE.equals(linkedQuestActivity.getStatus())) {
                                return repeatQuest(linkedQuest, doer, point, false)
                                        && completeQuest(c, cRo, linkedQuest, doer, false, point);
                            } else if (IN_PROGRESS.equals(linkedQuestActivity.getStatus())) {
                                return completeQuest(c, cRo, linkedQuest, doer, false, point);
                            } else {
                                return false;
                            }
                        } else {
                            Logger.warn(format("Invalid acitivity status for Quest with ID [%s] ", linkedQuestActivity.getQuestId()));
                            return repeatQuest(linkedQuest, doer, point, false);
                        }
                    }
                    return true;
                } else {
                    Logger.warn(format("Won't toggle milestone - Quest task with ID [%s] is not editable for user [%s]", milestone.getId(), doer.getId()));
                    return false;
                }
            case TEAM:
                if (isUserInMilestoneTeam(milestone, doer) || canCheckMilestone(milestone, doer)) {
                    milestoneService.checkMilestone(milestone, doer, targetCompletionState, point);
                    return true;
                } else {
                    Logger.warn(format("Won't toggle milestone - Quest task with ID [%s] is not editable for user [%s] and he's not a part of the Quest team", milestone.getId(), doer.getId()));
                    return false;
                }
            default:
                return false;
        }
    }

    @Transactional
    public boolean renameTaskGroup(final @Nonnull QuestTasksGroup tasksGroup,
                                   final @Nonnull User doer,
                                   final String newName) {

        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(tasksGroup.getQuestId(), em);
        if (quest == null) {
            Logger.info(format("Won't rename task group - no Quest found with ID [%s] and user [%s]", tasksGroup.getQuestId(), doer.getId()));
            return false;
        }
        if (StringUtils.isBlank(newName)) {
            Logger.info(format("Won't rename task group - empty value found for Quest with ID [%s] and user [%s]", tasksGroup.getQuestId(), doer.getId()));
            return false;
        }

        if (canEditGroup(tasksGroup, doer)) {
            return QuestTasksGroupDAO.updateQuestTasksGroup(tasksGroup, newName, em);
        } else {
            return false;
        }
    }

    public QuestTasksGroup addTasksGroupToQuest(final Quests quest,
                                                final User doer,
                                                final TasksGroupForm tasksGroupForm) throws QuestOperationForbiddenException {
        final EntityManager em = jpaApi.em();
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, doer, em);
        if (canManageTasksInQuest(quest, doer, activity)) {
            final User assignee = QuestTasksDAO.getTasksOwnerUserForQuest(quest, doer, em);
            return QuestTasksGroupDAO.addNewTasksGroup(doer, assignee, quest, tasksGroupForm, em);
        } else {
            throw new QuestOperationForbiddenException(format("User '%s' is not allowed to add milestones to Quest with ID %s", doer.getEmail(), quest.getId()));
        }
    }

    public List<QuestTasks> listMilestonesForQuest(final Quests quest, final User doer) {
        final EntityManager em = jpaApi.em();
        final User tasksOwnerUser = QuestTasksDAO.getTasksOwnerUserForQuest(quest, doer, em);
        if (tasksOwnerUser == null) {
            return emptyList();
        } else {
            return QuestTasksDAO.getQuestTasksByQuestIdAndUserId(quest.getId(), tasksOwnerUser.getId(), em);
        }
    }

    public List<QuestTasksGroup> listMilestonesGroupsForQuest(final Quests quest, final User doer) {
        final EntityManager em = jpaApi.em();
        final User tasksOwnerUser = QuestTasksDAO.getTasksOwnerUserForQuest(quest, doer, em);
        if (tasksOwnerUser == null) {
            return emptyList();
        } else {
            return QuestTasksGroupDAO.getQuestTasksGroupsByQuestIdAndUserId(quest.getId(), tasksOwnerUser.getId(), em);
        }
    }

    public String getQuestCompletionPercentage(final Quests quest, final User doer) {
        return QuestTasksDAO.getQuestCompletionPercentage(listMilestonesForQuest(quest, doer));
    }

    public boolean canEditMilestone(final QuestTasks milestone, final User user) {
        return canEditTaskRelatedEntity(user, milestone.getQuestId(), milestone.getUserId());
    }

    public boolean canEditGroup(final QuestTasksGroup tasksGroup, final User user) {
        return canEditTaskRelatedEntity(user, tasksGroup.getQuestId(), tasksGroup.getUserId());
    }

    private boolean canEditTaskRelatedEntity(final User user, final Integer entityQuestId, final Integer entityUserId) {
        if (user == null) {
            return false;
        }
        if (user.getId().equals(entityUserId)) {
            return true;
        }
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(entityQuestId, em);
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user, em);

        return canManageTasksInQuest(quest, user, activity);
    }

    private boolean isUserInMilestoneTeam(final QuestTasks milestone, final User user) {
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(milestone.getQuestId(), em);
        final QuestTeamMember teamMember = new QuestTeamDAO(em).getTeamMember(quest, user);
        return TEAM.equals(quest.getMode())
                && teamMember != null
                && teamMember.isActive()
                && user.getId().equals(teamMember.getMember().getId())
                && teamMember.getTeam().getCreator().getId().equals(milestone.getUserId());
    }

    private void cloneMilestonesForDoer(final Quests quest, final User doer) {
        final EntityManager em = jpaApi.em();
        QuestTasksDAO.getQuestTasksByQuestIdAndUserId(quest.getId(), doer.getId(), em)
                .forEach(milestone -> QuestTasksDAO.remove(milestone, em));

        if (QuestTasksGroupDAO.exist(quest.getId(), quest.getCreatedBy(), em)) {
            QuestTasksGroupDAO.getQuestTasksGroupsByQuestIdAndUserId(quest.getId(), quest.getCreatedBy(), em)
                    .forEach(taskGroup -> QuestTasksGroupDAO.copyGroupToUser(taskGroup, doer, quest, em));
        } else {
            QuestTasksDAO.getQuestTasksByQuestIdAndUserId(quest.getId(), quest.getCreatedBy(), em)
                    .forEach(questTask -> QuestTasksDAO.copyTaskWithoutGroupToUser(questTask, doer, quest, em));
        }
    }

    public boolean renameQuest(Integer questId, String questName, User user) throws QuestOperationForbiddenException  {
        final EntityManager em = jpaApi.em();
        final Quests quest = findById(questId, em);

        if (quest == null) {
            throw new NoSuchElementException();
        }

        final QuestPermissionsDTO permissions = new QuestPermissionsDTO(quest, user);

        if (permissions.editable) {
            quest.setTitle(questName);
            QuestsDAO.update(quest, em);
            return true;
        }
        else {
            throw new QuestOperationForbiddenException(format("User {} is not allowed to rename quest {}", user.getEmail(), questId));
        }
    }
    
    public List<ActivityRecord> getActivityIdsByActivityRecordList(Integer activityRecordListId, EntityManager em){
   	 
    	List<Integer> activityRecordListIds = new ArrayList<>();
    	activityRecordListIds.add(activityRecordListId);
    	return activityService.getActivityIdsByActivityRecordList(activityRecordListIds, em);
    }
    
    public List<AsActivityDTO> getActivitiesByRecordList(Integer activityRecordListId, EntityManager em){
      	 
    	List<Integer> activityRecordListIds = new ArrayList<>();
    	List<AsActivityDTO> activities = new ArrayList<>();
    	if(activityRecordListId==null) {
    		return activities;
    	}
    	activityRecordListIds.add(activityRecordListId);
    	List<ActivityRecord> activityrecords = activityService.getActivityIdsByActivityRecordList(activityRecordListIds, em);
    	
    	Set<Integer> ids = new HashSet<>();
    	for (ActivityRecord activityRecord : activityrecords) {
    		ids.add(activityRecord.getActivityId());
    	}
    
    	
    	if(ids!=null && !ids.isEmpty()) {
    	List<AsActivity> asActivities = AsActivityDAO.getActvitiesByIds(ids, em);
	    	for(AsActivity asActivity :asActivities) {
	    		AsActivityDTO asActivityDTO = new AsActivityDTO();
	    		asActivityDTO.setActivityName(asActivity.getName());
	    		asActivityDTO.setId(asActivity.getId());
	    		activities.add(asActivityDTO);
	    	}
    	}
	    	return activities;
    }
    
    public void addLogActivity(Integer questId, Integer userId, LogActivityForm form, String imageURL){
    	 final EntityManager em = jpaApi.em();
    	 
    	Integer actvityRecordValueId = QuestsDAO.addlogActivity(questId, userId, form, imageURL, em);
    	
    	if(form.getTags()!=null && form.getTags().size()>0)
    	QuestsDAO.addtags(actvityRecordValueId, form.getTags(), em);
    }
    
    public List<LogActivityDTO> getLogActivity(Integer questId,Integer pageNumber,Integer pageSize){
   	 final EntityManager em = jpaApi.em();
   	 
   	List<LogActivityDTO> logActivityDTOList = QuestsDAO.getlogActivity(questId,pageNumber,pageSize,em);
   	
   	 return logActivityDTOList;
   	
   }
    
    public List<AsCommentsDTO> addComments(Integer questId, Integer userId, CommentsForm form) {
    	final EntityManager em = jpaApi.em();
    	
    	return QuestsDAO.addComments(questId, userId, form, em);
    	
    }
    
    public AsLikesDTO addLikes(Integer questId, Integer userId, Integer activityRecordListValueId) {
    	final EntityManager em = jpaApi.em();
    	
    	return QuestsDAO.addLikes(questId, userId, activityRecordListValueId, em);
    	
    }
    
    
    public List<ActivityExportDTO> getActivityExportData(Integer questId) {
    	final EntityManager em = jpaApi.em();
    	
    	List<LogActivityDTO> logActivityDTOList = QuestsDAO.getlogActivity(questId,0,0,em);
    	
    	 return logActivityDTOList.stream()
                 .map(ActivityExportDTO::from)
                 .collect(toList());
 
    	
    }
    
    public Quests getQuestById(Integer questId) {
    	final EntityManager em = jpaApi.em();
    	
    	return QuestsDAO.getQuestById(questId, em);
    
    }
    
    
    public boolean editLogAcivity(Integer activityRecordValueId,String imageURL,LogActivityForm form,User user) {
    	final EntityManager em = jpaApi.em();
    	
    	Boolean isUpdated = QuestsDAO.editLogActivity(activityRecordValueId,imageURL,form.getComment(),form.getTitle(),user.getId(),em);
    	
    	return isUpdated;
    	
    }
    
    public boolean deleteLogAcivity(Integer activityRecordValueId,User user) {
    	final EntityManager em = jpaApi.em();
    	
    	Boolean isUpdated = QuestsDAO.deleteLogActivity(activityRecordValueId,user.getId(),em);
    	
    	return isUpdated;
    	
    }
    
    public List<LeaderboardMaxActivityDTO>  leaderboardMaxActivity(Integer questId) {

    	final EntityManager em = jpaApi.em();
    	
    	return QuestsDAO.leaderboardMaxActivity(questId,em);
    	
    	
    }
}
