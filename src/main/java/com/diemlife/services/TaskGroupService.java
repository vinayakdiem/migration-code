package com.diemlife.services;

import com.diemlife.constants.Util;
import com.diemlife.dao.ActivityDAO;
import com.diemlife.dao.AsPillarDAO;
import com.diemlife.dao.QuestTasksDAO;
import com.diemlife.dao.QuestTasksGroupDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dto.AllPillarsCount;
import com.diemlife.exceptions.QuestOperationForbiddenException;
import forms.TasksGroupManageEditForm;
import forms.TaskCreateForm;
import com.diemlife.models.ActivityRecord;
import com.diemlife.models.AsPillar;
import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTasksGroup;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.diemlife.dao.QuestActivityHome.getQuestActivityForQuestIdAndUser;
import static com.diemlife.dao.QuestTasksDAO.getTasksOwnerUserForQuest;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static com.diemlife.utils.QuestSecurityUtils.canManageTasksInQuest;

@Singleton
public class TaskGroupService {

    public static final String DEFAULT_TASK_GROUP_NAME = "Tasks";
    public static final int DEFAULT_TASK_ORDER = Short.MAX_VALUE;

    private final JPAApi jpaApi;
    private final ActivityService activityService;

    @Inject
    public TaskGroupService(final JPAApi jpaApi,final ActivityService activityService) {
        this.jpaApi = jpaApi;
        this.activityService = activityService;
    }

    @Transactional
    public QuestTasksGroup createDefaultGroup(final User user, final Integer groupOwnerId, final Integer questId, final String groupName) {
        final String defaultGroupName = isBlank(groupName) ? DEFAULT_TASK_GROUP_NAME : groupName;
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(questId, em);
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user, em);
        if (canManageTasksInQuest(quest, user, activity)) {
            Logger.info(format("TaskGroupService::createDefaultGroup - Creating default task group '%s' for Quest with ID %s", defaultGroupName, questId));
        } else {
            throw new QuestOperationForbiddenException(format("User '%s' is not allowed to create task groups in Quest with ID %s", user.getEmail(), quest.getId()));
        }

        final User assignee = getOwnerOrAssigneeUser(groupOwnerId, quest, user, em);
        final QuestTasksGroup defaultGroup = QuestTasksGroupDAO.addNewTasksGroup(assignee, quest, defaultGroupName, em);

        moveTasksToDefaultGroup(defaultGroup);

        return defaultGroup == null ? null : em.find(QuestTasksGroup.class, defaultGroup.getId());
    }

    @Transactional
    public QuestTasksGroup createGroup(final User user, final Integer groupOwnerId, final Integer questId, final String groupName) {
        final String nonNullGroupName = isBlank(groupName) ? DEFAULT_TASK_GROUP_NAME : groupName;
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(questId, em);
        //final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user, em);
        /*if (canManageTasksInQuest(quest, user, activity)) {
            Logger.info(format("TaskGroupService::createGroup - Creating new task group '%s' for Quest with ID %s", nonNullGroupName, questId));
        } else {
            throw new QuestOperationForbiddenException(format("User '%s' is not allowed to create task groups in Quest with ID %s", user.getEmail(), quest.getId()));
        }*/

        final User assignee = getOwnerOrAssigneeUser(groupOwnerId, quest, user, em);
        final List<QuestTasks> ungroupedTasks = QuestTasksDAO.getQuestTasksByQuestIdAndUserId(quest.getId(), assignee.getId(), em).stream()
                .filter(task -> task.getQuestTasksGroup() == null)
                .collect(toList());
        if (!ungroupedTasks.isEmpty()) {
            final QuestTasksGroup defaultGroup = QuestTasksGroupDAO.addNewTasksGroup(assignee, quest, DEFAULT_TASK_GROUP_NAME, em);

            moveUngroupedTasksToDefaultGroup(ungroupedTasks, defaultGroup, em);
        }
        final QuestTasksGroup group = QuestTasksGroupDAO.addNewTasksGroup(assignee, quest, nonNullGroupName, em);

        rearrangeGroupsInQuest(quest, user);

        return group;
    }

    @Transactional
    public QuestTasksGroup updateGroup(final User user, final Integer groupId, final String newGroupName, final Integer newPositionInQuest) {
        final EntityManager em = jpaApi.em();
        final QuestTasksGroup group = QuestTasksGroupDAO.findById(groupId, em);
        if (group == null) {
            Logger.warn("TaskGroupService::updateGroup - Task group not found with ID " + groupId);

            return null;
        }
        final Quests quest = QuestsDAO.findById(group.getQuestId(), em);
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user, em);
        if (group.getUserId().equals(user.getId()) || canManageTasksInQuest(quest, user, activity)) {
            Logger.info(format("TaskGroupService::updateGroup - Updating task group '%s' with ID %s at position %s", group.getGroupName(), groupId, group.getGroupOrder()));
        } else {
            throw new QuestOperationForbiddenException(format("User '%s' is not allowed to move task groups in Quest with ID %s", user.getEmail(), quest.getId()));
        }

        final Integer position = Optional.ofNullable(newPositionInQuest).orElse(group.getGroupOrder() == null
                ? DEFAULT_TASK_ORDER
                : group.getGroupOrder()
        );
        final boolean hasPositionChanged = !position.equals(group.getGroupOrder());

        final String name = Optional.ofNullable(newGroupName).map(String::trim).orElse(group.getGroupName() == null
                ? DEFAULT_TASK_GROUP_NAME
                : group.getGroupName()
        );
        final boolean hasNameChanged = !name.equals(group.getGroupName());

        if (hasPositionChanged) {
            Logger.info(format("TaskGroupService::updateGroup - Moving task group with ID %s at position %s", groupId, position));

            final List<QuestTasksGroup> existingGroups = new ArrayList<>(QuestTasksGroupDAO.getQuestTasksGroupsByQuestIdAndUserId(quest.getId(), user.getId(), em));
            final AtomicInteger groupIndex = new AtomicInteger(0);
            existingGroups.removeIf(existingGroup -> existingGroup.getId().equals(group.getId()));
            existingGroups.forEach(existingGroup -> existingGroup.setGroupOrder(groupIndex.getAndIncrement()));
            if (position < 0) {
                existingGroups.add(0, group);
            } else if (position >= existingGroups.size()) {
                existingGroups.add(group);
            } else {
                existingGroups.add(position, group);
            }
            groupIndex.set(0);
            existingGroups.forEach(existingGroup -> {
                existingGroup.setGroupOrder(groupIndex.getAndIncrement());
                em.merge(existingGroup);
            });
        }
        if (hasNameChanged) {
            Logger.info(format("TaskGroupService::updateGroup - Renaming task group with ID %s to '%s'", groupId, name));

            group.setGroupName(name);
            em.merge(group);
        }

        return group;
    }

    @Transactional
    public QuestTasks createTask(final User currentUser, final Integer questOwnerId, final Integer questId, final TaskCreateForm taskData) {
        final EntityManager em = jpaApi.em();
        final Quests quest = QuestsDAO.findById(questId, em);
       /* final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, currentUser, em);
        if (canManageTasksInQuest(quest, currentUser, activity)) {
            Logger.info(format("TaskGroupService::createTask - Creating new task for Quest with ID %s in group with ID %s at position %s", questId, taskData.getGroupId(), taskData.getOrder()));
        } else {
            throw new QuestOperationForbiddenException(format("User '%s' is not allowed to create tasks in Quest with ID %s", currentUser.getEmail(), quest.getId()));
        }*/

        final User assignee = getOwnerOrAssigneeUser(questOwnerId, quest, currentUser, em);
        final QuestTasks createdTask = QuestTasksDAO.addNewTask(currentUser, assignee, quest, taskData, em);
        if (createdTask == null) {
            return null;
        }

        final QuestTasksGroup targetGroup = taskData.getGroupId() == null
                ? getByIndexOrCreateDefault(questId, questOwnerId, assignee, taskData.getGroupIndex())
                : em.find(QuestTasksGroup.class, taskData.getGroupId());

        return targetGroup == null
                ? createdTask
                : addTaskToGroup(createdTask, targetGroup, taskData.getOrder());
    }

    private User getOwnerOrAssigneeUser(final Integer questOwnerId,
                                        final Quests quest, User currentUser,
                                        final EntityManager em) {
        //When receiving a user from a session, he is not always the owner of the quest
        if (questOwnerId != null) {
            return UserService.getById(questOwnerId, em);
        }
        return getTasksOwnerUserForQuest(quest, currentUser, em);
    }

    private QuestTasksGroup getByIndexOrCreateDefault(final Integer questId, final Integer groupOwnerId, final User user, final Integer groupIndex) {
        final List<QuestTasksGroup> groups = QuestTasksGroupDAO.getQuestTasksGroupsByQuestIdAndUserId(questId, user.getId(), jpaApi.em());
        if (Util.isEmpty(groups)) {
            return createDefaultGroup(user, groupOwnerId, questId, DEFAULT_TASK_GROUP_NAME);
        }
        if (groupIndex == null || groupIndex < 0 || groupIndex >= groups.size()) {
            return groups.iterator().next();
        } else {
            return groups.get(groupIndex);
        }
    }

    @Transactional
    public QuestTasks moveTask(final User user, final Integer taskId, final Integer targetGroupId, final Integer targetPositionInGroup) {
        final EntityManager em = jpaApi.em();

        final QuestTasks task = em.find(QuestTasks.class, taskId);
        if (task == null) {
            Logger.warn("TaskGroupService::moveTask - Task not found with ID " + taskId);

            return null;
        }

        final Quests quest = QuestsDAO.findById(task.getQuestId(), em);
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user, em);
        if (task.getUserId().equals(user.getId()) || canManageTasksInQuest(quest, user, activity)) {
            Logger.info(format("TaskGroupService::moveTask - Moving task with ID %s to group with ID %s at position %s", taskId, targetGroupId, targetPositionInGroup));
        } else {
            throw new QuestOperationForbiddenException(format("User '%s' is not allowed to move tasks in Quest with ID %s", user.getEmail(), quest.getId()));
        }

        final QuestTasksGroup sourceGroup = task.getQuestTasksGroup();
        final QuestTasksGroup targetGroup;
        final boolean isSameGroup;
        if (sourceGroup != null && sourceGroup.getId().equals(targetGroupId)) {
            targetGroup = sourceGroup;
            isSameGroup = true;
        } else {
            targetGroup = em.find(QuestTasksGroup.class, targetGroupId);
            isSameGroup = false;
        }
        if (targetGroup == null) {
            Logger.error("TaskGroupService::moveTask - Task group not found with ID " + targetGroupId);

            return task;
        }

        final QuestTasks orphanedTask = removeTaskFromGroup(task, sourceGroup, !isSameGroup);
        final QuestTasks groupedTask = addTaskToGroup(orphanedTask, targetGroup, targetPositionInGroup);

        Logger.info(format(
                "TaskGroupService::moveTask - Task with ID %s moved from group with ID %s to group with ID %s at position %s",
                task.getId(),
                sourceGroup == null ? null : sourceGroup.getId(),
                targetGroup.getId(),
                targetPositionInGroup
        ));

        return groupedTask;
    }

    @Transactional
    public void removeTask(final QuestTasks task) {
        final EntityManager em = jpaApi.em();
        final QuestTasksGroup group = task.getQuestTasksGroup();
        final QuestTasks orphanedTask = removeTaskFromGroup(task, group, true);

        QuestTasksDAO.getLastTaskCompletions(orphanedTask.getId(), em).forEach(em::remove);

        em.remove(orphanedTask);
    }

    @Transactional
    public QuestTasksGroup removeGroup(final User user, final Integer groupId) {
        final EntityManager em = jpaApi.em();
        final QuestTasksGroup group = em.find(QuestTasksGroup.class, groupId);
        final Quests quest = QuestsDAO.findById(group.getQuestId(), em);
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user, em);
        if (canManageTasksInQuest(quest, user, activity) && Util.isEmpty(group.getQuestTasks())) {
            Logger.info(format("TaskGroupService::removeGroup - Removing empty task group with ID %s", groupId));

            em.remove(group);

            return group;
        } else if (!Util.isEmpty(group.getQuestTasks())) {
            throw new QuestOperationForbiddenException(format("Task group with ID %s in Quest with ID %s is not empty", group, quest.getId()));
        } else {
            throw new QuestOperationForbiddenException(format("User '%s' is not allowed to remove task groups in Quest with ID %s", user.getEmail(), quest.getId()));
        }
    }

    private QuestTasks removeTaskFromGroup(final QuestTasks task, final QuestTasksGroup sourceGroup, final boolean removeEmptyGroup) {
        Logger.info(format(
                "TaskGroupService::removeTaskFromGroup - Removing task with ID %s from group with ID %s",
                task.getId(),
                sourceGroup == null ? null : sourceGroup.getId()
        ));

        final EntityManager em = jpaApi.em();
        final List<QuestTasks> sourceGroupTasks = sourceGroup == null ? emptyList() : sourceGroup.getQuestTasks();
        if (sourceGroup != null) {
            sourceGroupTasks.removeIf(sourceTask -> sourceTask.getId().equals(task.getId()));
        }
        task.setQuestTasksGroup(null);
        task.setOrder(DEFAULT_TASK_ORDER);

        final QuestTasks orphanedTask = em.merge(task);

        if (sourceGroupTasks.isEmpty() && removeEmptyGroup) {
            Optional.ofNullable(sourceGroup).ifPresent(group -> {
                Logger.info("TaskGroupService::removeTaskFromGroup - Removing empty group with ID " + group.getId());

                em.remove(group);
            });
        } else {
            rearrangeTasksInGroup(sourceGroup);
        }

        return orphanedTask;
    }

    private QuestTasks addTaskToGroup(final QuestTasks task, final QuestTasksGroup targetGroup, final Integer targetPositionInGroup) {
        Logger.info(format("TaskGroupService::addTaskToGroup - Added task with ID %s to group with ID %s", task.getId(), targetGroup.getId()));

        final EntityManager em = jpaApi.em();
        if (targetGroup.getQuestTasks() == null) {
            targetGroup.setQuestTasks(new ArrayList<>());
        }
        final int position = Optional.ofNullable(targetPositionInGroup).orElse((int) Short.MAX_VALUE);
        task.setQuestTasksGroup(targetGroup);
        task.setOrder(position);

        final List<QuestTasks> targetGroupTasks = targetGroup.getQuestTasks();
        if (position < 0) {
            targetGroupTasks.add(0, task);
        } else if (position >= targetGroupTasks.size()) {
            targetGroupTasks.add(task);
        } else {
            targetGroupTasks.add(position, task);
        }

        final QuestTasks groupedTask = em.merge(task);

        rearrangeTasksInGroup(targetGroup);

        return groupedTask;
    }

    private void rearrangeTasksInGroup(final QuestTasksGroup group) {
        final EntityManager em = jpaApi.em();
        if (group != null && group.getQuestTasks() != null && !group.getQuestTasks().isEmpty()) {
            Logger.info("TaskGroupService::rearrangeTasksInGroup - Cleaning up tasks order in group with ID " + group.getId());

            final List<QuestTasks> groupTasks = group.getQuestTasks();
            IntStream.range(0, groupTasks.size()).boxed().forEach(index -> {
                final QuestTasks sourceTask = group.getQuestTasks().get(index);
                sourceTask.setOrder(index);
                em.merge(sourceTask);
            });
        }
    }

    private void rearrangeGroupsInQuest(final Quests quest, final User user) {
        final EntityManager em = jpaApi.em();
        final List<QuestTasksGroup> groups = QuestTasksGroupDAO.getQuestTasksGroupsByQuestIdAndUserId(quest.getId(), user.getId(), em);
        if (groups != null && !groups.isEmpty()) {
            Logger.info("TaskGroupService::rearrangeGroupsInQuest - Cleaning up tasks groups order in Quest with ID " + quest.getId());

            IntStream.range(0, groups.size()).boxed().forEach(index -> {
                final QuestTasksGroup group = groups.get(index);
                group.setGroupOrder(index);
                em.merge(group);
            });
        }
    }

    protected void moveTasksToDefaultGroup(final QuestTasksGroup defaultGroup) {
        if (defaultGroup != null) {
            final EntityManager em = jpaApi.em();
            final List<QuestTasks> ungroupedTasks = QuestTasksDAO.getQuestTasksByQuestIdAndUserId(defaultGroup.getQuestId(), defaultGroup.getUserId(), em)
                    .stream()
                    .filter(task -> task.getQuestTasksGroup() == null).collect(toList());

            moveUngroupedTasksToDefaultGroup(ungroupedTasks, defaultGroup, em);
        }
    }

    private void moveUngroupedTasksToDefaultGroup(List<QuestTasks> ungroupedTasks, QuestTasksGroup defaultGroup, EntityManager em) {
        final AtomicInteger order = new AtomicInteger(0);
        ungroupedTasks.forEach(task -> {
            task.setQuestTasksGroup(defaultGroup);
            task.setOrder(order.getAndIncrement());
            em.merge(task);
            if (Optional.ofNullable(defaultGroup).map(QuestTasksGroup::getQuestTasks).map(tasks -> tasks.add(task)).orElse(false)) {
                em.merge(defaultGroup);
            }
        });
    }

public void updateGroupNameById(List<TasksGroupManageEditForm> taskGroupsWithIds) {
    	
    	final EntityManager em = jpaApi.em();
    	taskGroupsWithIds.forEach(tasksGroupManageEditForm->{
    		QuestTasksGroupDAO.updateGroupNameById(tasksGroupManageEditForm.getGroupName(), tasksGroupManageEditForm.getId(), em);
    	});
    }
    
    public void createGroupWithGroupOrder( final User user,Integer groupOwnerId,final Quests quest,final TasksGroupManageEditForm group) {
    	 final EntityManager em = jpaApi.em();
    	
    	 final User assignee = getOwnerOrAssigneeUser(groupOwnerId, quest, user, em);
         final QuestTasksGroup defaultGroup = QuestTasksGroupDAO.addNewTasksGroup(assignee, quest, group.getGroupName(),em);
    }
    
    public Map<String,List<AllPillarsCount>> getActivityCountForPillarsByUserId( final Integer userId) {
   	 final EntityManager em = jpaApi.em();
   	
   	
   	List<AllPillarsCount> allPillarsCounts = new ArrayList<>();
	List<AllPillarsCount> allQuestCount = new ArrayList<>();

	List<Integer> userIds = new ArrayList<>();
	userIds.add(userId);
	
	
   	allPillarsCounts = QuestsDAO.getTotalPillarCountByUserIds(userIds, em);
   	 
   	 
   	 /*List<Integer> activityRecordListIds = QuestTasksDAO.getActivityRecordListIdsByUser(userId,em);
   	 
   	 List<ActivityRecord> activityRecords = new ArrayList<>(); 
   	 if(activityRecordListIds!=null && activityRecordListIds.size()>0) {
   		 activityRecords = activityService.getActivityIdsByActivityRecordList(activityRecordListIds, em);
   	 }
   	 
   	 
   	 List<Integer> taskActvities = new ArrayList();

		for(ActivityRecord activityRecord:activityRecords) {
			taskActvities.add(activityRecord.getActivityId());
		}
   	 
		
		List<AllPillarsCount> allPillarsCounts = new ArrayList<>();
		List<AllPillarsCount> allQuestCount = new ArrayList<>();
		
		if(taskActvities!=null && taskActvities.size()>0) {
			allPillarsCounts = activityService.getTotalPillarsByActivityIds(taskActvities,em);
		}
*/		
		 List<AsPillar> pillars =  AsPillarDAO.findAllPillars(em); 
		 List<AsPillar> pillarNameNotFound = new ArrayList<>();
		 
		 for(AsPillar asPillar :pillars) {
			 boolean found=false;
			if(allPillarsCounts!=null && allPillarsCounts.size()>0) {
				for(AllPillarsCount allPillarsCount :allPillarsCounts) {
					if(allPillarsCount.getId()==asPillar.getId()) {
						allPillarsCount.setName(asPillar.getName());
						found=true;
						break;
					}
				}
			} 
			
			if(!found) {
				pillarNameNotFound.add(asPillar);
			}
		 }
		
		 if(pillarNameNotFound.size()>0 || allPillarsCounts==null || allPillarsCounts.size()==0) {
			 for(AsPillar pillar :pillarNameNotFound) {
				 AllPillarsCount allPillarsCount = new AllPillarsCount();
				 allPillarsCount.setCount((long) 0);
				 allPillarsCount.setId(pillar.getId());
				 allPillarsCount.setName(pillar.getName());
				 allPillarsCounts.add(allPillarsCount);
				 
			 }
		 }
		 
		 
		/* for(AllPillarsCount allPillarsCount :allPillarsCounts) {
			 	AllPillarsCount allQuest = new AllPillarsCount(); 
				allQuest.setName(allPillarsCount.getName());		
				allQuest.setCount(allPillarsCount.getCount());
				allQuest.setId(allPillarsCount.getId());
				
			 if(allPillarsCount.getCount()>0) {

				 List<Integer> activityRecordList = activityService.getActivityRecordListByPillar(allPillarsCount.getId(), em);
					
					if(activityRecordList!=null && activityRecordList.size()>0) {
						Long count = 	activityService.getTotalQuestByUserAndActivityRecordService(userId,activityRecordList,em);
						allQuest.setCount(count);
					}else {
						allQuest.setCount((long)0);
					}
				}
			 allQuestCount.add(allQuest);
		 }
*/		 
		 Map<String,List<AllPillarsCount>>  totalpillars = new HashMap<String, List<AllPillarsCount>>();
		 totalpillars.put("activities", allPillarsCounts);
		 totalpillars.put("quests", allQuestCount);
		
		 return totalpillars;	
    
    
    }
  }
