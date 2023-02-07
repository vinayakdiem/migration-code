package com.diemlife.dao;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.forms.TasksGroupForm;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTasksGroup;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Repository
public class QuestTasksGroupDAO {

	@PersistenceContext
	EntityManager em;

    public QuestTasksGroup findById(Integer taskGroupId) {
        try {
            QuestTasksGroup questTask = em.find(QuestTasksGroup.class, taskGroupId);
            if (questTask != null) {
                return questTask;
            }
        } catch (final PersistenceException e) {
            Logger.error(format("Error finding quest task group by ID [%s]", taskGroupId), e);
            throw e;
        }
        return null;
    }

    private List<QuestTasks> findAllByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery("SELECT t FROM QuestTasks t WHERE t.id in :ids", QuestTasks.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public boolean exist(Integer questId, Integer userId) {
        return countQuestTasksGroupsByQuestIdAndUserId(questId, userId) > 0;
    }

    public long countQuestTasksGroupsByQuestIdAndUserId(final Integer questId, final Integer userId) {
        if (questId == null || userId == null) {
            return 0L;
        }
        return em.createQuery("SELECT COUNT(qt.id) FROM QuestTasksGroup qt WHERE qt.questId = :questId AND qt.userId = :userId", Long.class)
                .setParameter("questId", questId)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    public boolean updateQuestTasksGroup(QuestTasksGroup tasksGroup, String name) {
        try {
        	//FIXME Vinayak
//            tasksGroup.setGroupName(name);
            em.merge(tasksGroup);
            return true;
        } catch (Exception e) {
        	//FIXME Vinayak
//            Logger.error(format("Unable to update tasks group with ID %s due to '%s'", tasksGroup.getId(), e.getMessage()), e);
            return false;
        }
    }

    public List<QuestTasksGroup> getQuestTasksGroupsByQuestIdAndUserId(final Integer questId, final Integer userId) {
        if (questId == null || userId == null) {
            return Collections.emptyList();
        }
        return em.createQuery("SELECT qt FROM QuestTasksGroup qt WHERE qt.questId = :questId AND qt.userId = :userId ORDER BY qt.groupOrder", QuestTasksGroup.class)
                .setParameter("questId", questId)
                .setParameter("userId", userId)
                .getResultList();
    }

    public QuestTasksGroup addNewTasksGroup(User creator, User assignee, Quests quest, TasksGroupForm tasksGroupForm) {
        if (creator == null) {
            throw new RequiredParameterMissingException("creator");
        }
        if (assignee == null) {
            throw new RequiredParameterMissingException("assignee");
        }
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        if (tasksGroupForm == null) {
            throw new RequiredParameterMissingException("tasksGroupForm");
        }

        try {
            final Integer lastOrder = getLastTasksGroupOrder(assignee.getId(), quest.getId());
            final Date now = new Date();
            final QuestTasksGroup tasksGroup = new QuestTasksGroup();
          //FIXME Vinayak
//            final List<QuestTasks> questTasks = findAllByIds(tasksGroupForm.getGroupTasks());

          //FIXME Vinayak
//            tasksGroup.setQuestId(quest.getId());
//            tasksGroup.setUserId(assignee.getId());
//            tasksGroup.setQuestTasks(questTasks);
//            tasksGroup.setGroupName(tasksGroupForm.getGroupName());
//            tasksGroup.setCreatedDate(now);
//            tasksGroup.setGroupOrder(lastOrder + 1);

//            questTasks.forEach(task -> {
//                task.setQuestTasksGroup(tasksGroup);
//                em.merge(task);
//            });

            em.persist(tasksGroup);

            return tasksGroup;
        } catch (final PersistenceException e) {
            Logger.error("QuestTasksGroupDAO :: addNewTasksGroup : error adding new tasks group for quest = " + quest.getId() + " => " + e.getMessage(), e);

            return null;
        }
    }

    public QuestTasksGroup addNewTasksGroup(final User assignee, final Quests quest, final String groupName) {
        if (assignee == null) {
            throw new RequiredParameterMissingException("assignee");
        }
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        if (groupName == null) {
            throw new RequiredParameterMissingException("groupName");
        }

        try {
            final QuestTasksGroup tasksGroup = new QuestTasksGroup();

          //FIXME Vinayak
//            tasksGroup.setQuestId(quest.getId());
//            tasksGroup.setUserId(assignee.getId());
//            tasksGroup.setQuestTasks(new ArrayList<>());
//            tasksGroup.setGroupName(groupName);
//            tasksGroup.setCreatedDate(Timestamp.from(Instant.now()));

            final Integer lastOrder = getLastTasksGroupOrder(assignee.getId(), quest.getId());
          //FIXME Vinayak
//            tasksGroup.setGroupOrder(lastOrder + 1);

            em.persist(tasksGroup);

            return tasksGroup;
        } catch (final PersistenceException e) {
            Logger.error("QuestTasksGroupDAO :: addNewTasksGroup : error adding new tasks group for quest = " + quest.getId() + " => " + e.getMessage(), e);

            return null;
        }
    }

    public void copyGroupToUser(final QuestTasksGroup taskGroup, final User doer, final Quests quest) {
        if (taskGroup == null) {
            throw new RequiredParameterMissingException("taskGroup");
        }
        if (doer == null) {
            throw new RequiredParameterMissingException("doer");
        }
        final Date now = new Date();
        final QuestTasksGroup clonedGroup = new QuestTasksGroup();
        try {
        	//FIXME Vinayak
//            final Integer lastOrder = getLastTasksGroupOrder(doer.getId(), taskGroup.getQuestId(), em);
//
//            clonedGroup.setUserId(doer.getId());
//            clonedGroup.setQuestId(taskGroup.getQuestId());
//            clonedGroup.setGroupName(taskGroup.getGroupName());
//            clonedGroup.setCreatedDate(now);
//            clonedGroup.setGroupOrder(lastOrder + 1);

            em.persist(clonedGroup);

          //FIXME Vinayak
//            taskGroup.getQuestTasks().stream()
//                    .filter(Objects::nonNull)
//                    .map(questTask -> QuestTasksDAO.copyTaskWithoutGroupToUser(questTask, doer, quest, em))
//                    .filter(Objects::nonNull)
//                    .peek(clonedTask -> clonedTask.setQuestTasksGroup(clonedGroup))
//                    .forEach(clonedTask -> em.merge(clonedGroup));

        } catch (final PersistenceException e) {
        	//FIXME Vinayak
//            Logger.error("QuestTasksGroupDAO :: copyGroupToUser : error copying group " + taskGroup.getId(), e);
        }
    }

    private Integer getLastTasksGroupOrder(final Integer userId, final Integer questId) {
        try {
            final Integer order = em
                    .createQuery("SELECT MAX(qt.groupOrder) FROM QuestTasksGroup qt WHERE qt.questId = :questId AND qt.userId = :userId", Integer.class)
                    .setParameter("questId", questId)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return order == null ? 0 : order;
        } catch (final NoResultException e) {
            return 0;
        } catch (final PersistenceException e) {
            Logger.error(format("Error getting last tasks group order for Quest with ID [%s] and user with ID [%s]", questId, userId), e);
            throw e;
        }
    }

     public void updateGroupNameById(final String groupName, final Integer groupId) {
        try {
            em
                    .createQuery("UPDATE QuestTasksGroup SET groupName=:groupName WHERE id = :groupId")
                    .setParameter("groupName", groupName)
                    .setParameter("groupId", groupId)
                    .executeUpdate();
        } catch (final PersistenceException e) {
            Logger.error(format("Error updating group nmae [%s] with group Id [%s]",groupName,groupId), e);
            throw e;
        }
    }
}
