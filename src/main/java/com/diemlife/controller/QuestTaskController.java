package com.diemlife.controller;

import static com.diemlife.dao.QuestsDAO.findById;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static play.mvc.Controller.session;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.forbidden;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notAcceptable;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.unauthorized;
import static services.TaskGroupService.DEFAULT_TASK_GROUP_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.dao.QuestTasksDAO;
import com.diemlife.dao.QuestTasksGroupDAO;
import com.diemlife.dto.LinkPreviewDTO;
import com.diemlife.dto.MilestoneDTO;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTasksGroup;
import com.diemlife.models.Quests;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.typesafe.config.Config;

import com.diemlife.dto.TaskGroupDTO;
import exceptions.QuestOperationForbiddenException;
import forms.TaskMoveForm;
import forms.TasksGroupForm;
import forms.TasksGroupManageForm;
import forms.TasksGroupRenameForm;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.libs.Json;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.LinkPreviewService;
import services.MilestoneService;
import services.QuestService;
import services.TaskGroupService;
import services.UserProvider;

public class QuestTaskController {

    private final FormFactory formFactory;
    private final JPAApi jpaApi;
    private final Config config;
    private final LinkPreviewService linkPreviewService;
    private final QuestService questService;
    private final TaskGroupService taskGroupService;
    private final UserProvider userProvider;
    private final MilestoneService milestoneService;

    @Inject
    public QuestTaskController(final Config config,
                               final LinkPreviewService linkPreviewService,
                               final QuestService questService,
                               final TaskGroupService taskGroupService,
                               final FormFactory formFactory,
                               final UserProvider userProvider,
                               final JPAApi api,
                               MilestoneService milestoneService) {
        this.config = config;
        this.linkPreviewService = linkPreviewService;
        this.questService = questService;
        this.taskGroupService = taskGroupService;
        this.userProvider = userProvider;
        this.jpaApi = api;
        this.formFactory = formFactory;
        this.milestoneService = milestoneService;
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result addLinkToTask() {
        final DynamicForm requestData = formFactory.form().bindFromRequest();
        final String taskId = requestData.get("taskId");
        final String taskLink = requestData.get("taskLink");
        if (isNotBlank(taskId) && !isNumeric(taskId)) {
            return badRequest("Invalid parameter 'taskId': " + taskId);
        }
        if (isBlank(taskLink)) {
            return badRequest("Missing parameter 'taskLink'");
        }
        final EntityManager em = this.jpaApi.em();
        if (isBlank(taskId)) {
            final LinkPreviewDTO linkPreview = linkPreviewService.createLinkPreview(taskLink);
            final MilestoneDTO result = new MilestoneDTO();
            result.setLinkPreview(linkPreview);
            result.setLinkUrl(taskLink);
            return ok(Json.toJson(result));
        }
        final QuestTasks questTask = QuestTasksDAO.findById(Integer.valueOf(taskId), em);
        if (questTask == null) {
            return badRequest();
        }
        final User user = this.userProvider.getUser(session());
        if (questService.canEditMilestone(questTask, user)) {
            questTask.setVideo(null);
            questTask.setLinkUrl(taskLink);
            questTask.setLinkedQuestId(null);
            em.merge(questTask);
            return ok(Json.toJson(milestoneService.convertToDto(questTask, getEnvUrl(), linkPreviewService)));
        } else {
            return forbidden();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result addQuestLinkToTask() {
        final DynamicForm requestData = formFactory.form().bindFromRequest();
        final String taskId = requestData.get("taskId");
        final String linkedQuestId = requestData.get("linkedQuestId");
        if (!isNumeric(taskId)) {
            return badRequest("Invalid parameter 'taskId': " + taskId);
        }
        if (!isNumeric(linkedQuestId)) {
            return badRequest("Invalid parameter 'linkedQuestId': " + linkedQuestId);
        }
        final EntityManager em = this.jpaApi.em();
        final QuestTasks questTask = QuestTasksDAO.findById(Integer.valueOf(taskId), em);
        if (questTask == null) {
            return badRequest();
        }
        final User user = this.userProvider.getUser(session());
        if (questService.canEditMilestone(questTask, user)) {
            questTask.setImageUrl(null);
            questTask.setVideo(null);
            questTask.setLinkUrl(null);
            questTask.setLinkedQuestId(Integer.valueOf(linkedQuestId));
            final QuestTasks savedTask = em.merge(questTask);
            return ok(Json.toJson(milestoneService.convertToDto(savedTask, getEnvUrl(), linkPreviewService)));
        } else {
            return forbidden();
        }
    }

    private String getEnvUrl() {
        return config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
    }

    @Transactional(readOnly = true)
    @JwtSessionLogin
    public Result getQuestTasksGroups(final Integer questId, final Integer userId) {
        if (questId == null) {
            return badRequest();
        }
        final EntityManager em = this.jpaApi.em();
        final Quests quest = em.find(Quests.class, questId);
        if (quest == null) {
            return notFound();
        }
        final User user = em.find(User.class, userId);
        final String envUrl = getEnvUrl();
        final List<TaskGroupDTO> taskGroups = questService.listMilestonesGroupsForQuest(quest, user)
                .stream()
                .map(taskGroup -> milestoneService.convertToDto(taskGroup, envUrl, linkPreviewService))
                .collect(toList());

        final List<QuestTasks> ungroupedTasks = questService.listMilestonesForQuest(quest, user).stream()
                .filter(task -> task.getQuestTasksGroup() == null)
                .collect(toList());
        if (ungroupedTasks.isEmpty()) {
        	for(TaskGroupDTO taskGroupDTO:taskGroups) {
            	for(MilestoneDTO milestoneDTO :taskGroupDTO.getQuestTasks()) {
            		if(milestoneDTO.getActivityRecordListId()!=null)
            			//milestoneDTO.setActivityRecords(questService.getActivityIdsByActivityRecordList(milestoneDTO.getActivityRecordListId(),em));
            			milestoneDTO.setActivities(questService.getActivitiesByRecordList(milestoneDTO.getActivityRecordListId(),em));
            	}
            }
        	return ok(Json.toJson(taskGroups));
        } else {
            final User tasksOwnerUser = QuestTasksDAO.getTasksOwnerUserForQuest(quest, user, em);
            final QuestTasksGroup transientDefaultGroup = new QuestTasksGroup();

            transientDefaultGroup.setQuestId(quest.getId());
            transientDefaultGroup.setUserId(tasksOwnerUser == null ? null : tasksOwnerUser.getId());
            transientDefaultGroup.setQuestTasks(ungroupedTasks);
            transientDefaultGroup.setGroupName(DEFAULT_TASK_GROUP_NAME);
            transientDefaultGroup.setGroupOrder(taskGroups.size());

            final List<TaskGroupDTO> ungroupedTasksGroups = singletonList(milestoneService.convertToDto(transientDefaultGroup, envUrl, linkPreviewService));
            final List<TaskGroupDTO> mergedGroups = new ArrayList<>();

            for(TaskGroupDTO taskGroupDTO:ungroupedTasksGroups) {
            	for(MilestoneDTO milestoneDTO :taskGroupDTO.getQuestTasks()) {
            		if(milestoneDTO.getActivityRecordListId()!=null)
            		//milestoneDTO.setActivityRecords(questService.getActivityIdsByActivityRecordList(milestoneDTO.getActivityRecordListId(),em));
            		milestoneDTO.setActivities(questService.getActivitiesByRecordList(milestoneDTO.getActivityRecordListId(),em));
            	}
            }
            for(TaskGroupDTO taskGroupDTO:taskGroups) {
            	for(MilestoneDTO milestoneDTO :taskGroupDTO.getQuestTasks()) {
            		if(milestoneDTO.getActivityRecordListId()!=null)
            			//milestoneDTO.setActivityRecords(questService.getActivityIdsByActivityRecordList(milestoneDTO.getActivityRecordListId(),em));
            			milestoneDTO.setActivities(questService.getActivitiesByRecordList(milestoneDTO.getActivityRecordListId(),em));
            	}
            }
            mergedGroups.addAll(taskGroups);
            mergedGroups.addAll(ungroupedTasksGroups);

            return ok(Json.toJson(mergedGroups));
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result updateQuestTasksGroup() {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final Form<TasksGroupRenameForm> formBinding = formFactory.form(TasksGroupRenameForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final EntityManager em = this.jpaApi.em();
        final TasksGroupRenameForm form = formBinding.get();
        final QuestTasksGroup tasksGroup = QuestTasksGroupDAO.findById(form.getGroupId(), em);
        if (tasksGroup == null) {
            return notFound();
        }
        return ok(BooleanNode.valueOf(questService.renameTaskGroup(tasksGroup, user, form.getGroupName())));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result addGroup(final Integer questId) {
        final Form<TasksGroupForm> formBinding = formFactory.form(TasksGroupForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        final TasksGroupForm form = formBinding.get();
        if (form.getGroupId() != null) {
            return badRequest();
        }
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = this.jpaApi.em();
        final Quests quest = findById(questId, em);
        if (quest == null) {
            return notFound();
        }
        try {
            final QuestTasksGroup tasksGroup = questService.addTasksGroupToQuest(quest, user, form);
            if (tasksGroup == null) {
                return internalServerError();
            } else {
                return ok(Json.toJson(tasksGroup));
            }
        } catch (final QuestOperationForbiddenException e) {
            return forbidden();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result createDefaultGroup(final Integer questId) {
        return createGroup(questId, true);
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result createGroup(final Integer questId) {
        return createGroup(questId, false);
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result updateGroup(final Integer groupId) {
        return manageGroup((form, user) ->
                taskGroupService.updateGroup(user, groupId, form.getGroupName(), form.getGroupOrder()));
    }

    @Transactional
    @JwtSessionLogin
    public Result removeGroup(final Integer groupId) {
        return manageGroup((form, user) ->
                taskGroupService.removeGroup(user, groupId));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result moveTask() {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final Form<TaskMoveForm> formBinding = formFactory.form(TaskMoveForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        try {
            final TaskMoveForm form = formBinding.get();
            final QuestTasks task = taskGroupService.moveTask(user, form.getTaskId(), form.getGroupId(), form.getTaskOrder());
            if (task == null) {
                return notAcceptable();
            } else {
                return ok(Json.toJson(milestoneService.convertToDto(task, getEnvUrl(), linkPreviewService)));
            }
        } catch (final QuestOperationForbiddenException e) {
            return forbidden();
        }
    }

    private Result createGroup(final Integer questId, final boolean isDefault) {
        return manageGroup((form, user) -> {
            final String groupName = Optional.ofNullable(form.getGroupName()).orElse(DEFAULT_TASK_GROUP_NAME);
            return isDefault
                    ? taskGroupService.createDefaultGroup(user, form.getGroupOwnerId(), questId, groupName)
                    : taskGroupService.createGroup(user, form.getGroupOwnerId(), questId, groupName);
        });
    }

    private Result manageGroup(final BiFunction<TasksGroupManageForm, User, QuestTasksGroup> tasksGroupConsumer) {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final Form<TasksGroupManageForm> formBinding = formFactory.form(TasksGroupManageForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }
        try {
            final QuestTasksGroup group = tasksGroupConsumer.apply(formBinding.get(), user);
            if (group == null) {
                return notAcceptable();
            } else {
                return ok(Json.toJson(milestoneService.convertToDto(group, getEnvUrl(), linkPreviewService)));
            }
        } catch (final QuestOperationForbiddenException e) {
            return forbidden();
        }
    }

}
