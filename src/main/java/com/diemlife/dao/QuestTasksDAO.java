package com.diemlife.dao;

import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.TEAM;
import static com.diemlife.dao.QuestActivityHome.getQuestActivityForQuestIdAndUser;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.MD;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.SM;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.XS;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.upperCase;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import com.diemlife.constants.QuestMode;
import com.diemlife.constants.VideoProvider;
import com.diemlife.dto.QuestListDetailDTO;
import com.diemlife.dto.QuestTaskGeometryDTO;
import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.forms.EmbeddedVideoForm;
import com.diemlife.forms.QuestActionPointForm;
import com.diemlife.forms.TaskCreateForm;
import com.diemlife.models.ActivityRecord;
import com.diemlife.models.ActivityRecordList;
import com.diemlife.models.EmbeddedVideo;
import com.diemlife.models.EmbeddedVideo.ThumbnailKey;
import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestTaskCompletionHistory;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTasksGroup;
import com.diemlife.models.QuestTeamMember;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import play.Logger;

/**
 * Created by acoleman1 on 6/5/17.
 */
public class QuestTasksDAO {

    public static class CompletionPercentage {
        public Integer questId;
        public String completionPercentage;
    }

    public void persist(QuestTasks transientInstance, EntityManager em) {

        try {
            em.persist(transientInstance);
        } catch (Exception e) {
            Logger.error("QuestTasksDAO :: persist : error persisting quest task => " + e, e);
        }
    }

    public static void remove(QuestTasks persistentInstance, EntityManager em) {
        try {
            em.createQuery("SELECT ch " +
                    "FROM QuestTaskCompletionHistory ch " +
                    "WHERE ch.milestoneId = :milestoneId", QuestTaskCompletionHistory.class)
                    .setParameter("milestoneId", persistentInstance.getId())
                    .getResultList()
                    .forEach(em::remove);
            em.remove(persistentInstance);
        } catch (final Exception e) {
            Logger.error("QuestTasksDAO :: remove : error removing quest task => " + e, e);
        }
    }

    public static QuestTasks findById(Integer id, EntityManager em) {
        try {
            QuestTasks questTask = em.find(QuestTasks.class, id);
            if (questTask != null) {
                return questTask;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Point buildGeoPoint(final QuestActionPointForm togglePoint) {
        return togglePoint == null ? null : new GeometryFactory().createPoint(new Coordinate(
                togglePoint.getLatitude().doubleValue(),
                togglePoint.getLongitude().doubleValue()
        ));
    }

    public static QuestTasks addNewTask(final User creator,
                                        final User assignee,
                                        final Quests quest,
                                        final TaskCreateForm task,
                                        final EntityManager em) {
        if (creator == null) {
            throw new RequiredParameterMissingException("creator");
        }
        if (assignee == null) {
            throw new RequiredParameterMissingException("assignee");
        }
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        if (task == null) {
            throw new RequiredParameterMissingException("task");
        }
        
        ActivityRecordList activityRecordList =  newActivityRecordList(creator.getId(),task.getActivitiesIds(),em);
        
        try {
            final Date now = new Date();
            final QuestTasks questTask = new QuestTasks();

            if (task.getVideo() != null) {
                questTask.setVideo(createEmbeddedVideo(task.getVideo(), em));
            }

            questTask.setQuestId(quest.getId());
            questTask.setUserId(assignee.getId());
            questTask.setTask(task.getTask());
            questTask.setTaskCompleted(Boolean.FALSE.toString().toUpperCase());
            questTask.setCreatedBy(creator.getId());
            questTask.setCreatedDate(now);
            questTask.setLinkedQuestId(task.getLinkedQuestId());
            questTask.setPoint(buildGeoPoint(task.getPoint()));
            questTask.setRadiusInKm(task.getRadiusInKm());
            questTask.setQuestTasksGroup(task.getGroupId() == null ? null : em.find(QuestTasksGroup.class, task.getGroupId()));
            questTask.setImageUrl(task.getImageUrl());
            questTask.setLinkUrl(task.getLinkUrl());
            questTask.setActivityRecordListId(activityRecordList.getId());
            questTask.setTitle(task.getTitle());

            final int lastOrder = getLastTaskOrder(assignee.getId(), quest.getId(), task.getGroupId(), em);
            questTask.setOrder(lastOrder + 1);

            em.persist(questTask);

            return questTask;
        } catch (final PersistenceException e) {
            Logger.error("QuestTasksDAO :: addNewTask : error adding new task for assignee = " + assignee.getId() + " => " + e.getMessage(), e);

            return null;
        }
    }

    public static EmbeddedVideo createEmbeddedVideo(final EmbeddedVideoForm videoForm, final EntityManager em) {
        final EmbeddedVideo video = new EmbeddedVideo();
        video.url = videoForm.getUrl();
        video.provider = VideoProvider.valueOf(upperCase(videoForm.getProvider()));
        em.persist(video);
        em.flush();
        video.thumbnails = ImmutableMap.of(
                new ThumbnailKey(video.id, XS.name().toLowerCase()), videoForm.getThumbnails().getXs(),
                new ThumbnailKey(video.id, SM.name().toLowerCase()), videoForm.getThumbnails().getSm(),
                new ThumbnailKey(video.id, MD.name().toLowerCase()), videoForm.getThumbnails().getMd()
        );
        return video;
    }

    public static QuestTasks copyTaskWithoutGroupToUser(final QuestTasks task,
                                                        final User user,
                                                        final Quests quest,
                                                        final EntityManager em) {
        if (task == null) {
            throw new RequiredParameterMissingException("task");
        }
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }

        try {
            final Date now = new Date();

            final QuestTasks questTask = new QuestTasks();
            questTask.setQuestId(quest.getId());
            questTask.setUserId(user.getId());
            questTask.setTask(task.getTask());
            questTask.setVideo(task.getVideo());
            questTask.setTaskCompleted(Boolean.FALSE.toString().toUpperCase());
            questTask.setCreatedDate(now);
            questTask.setCreatedBy(task.getCreatedBy());
            questTask.setLastModifiedDate(now);
            questTask.setLastModifiedBy(user.getId());
            questTask.setLinkedQuestId(task.getLinkedQuestId());
            questTask.setPoint(task.getPoint());
            questTask.setRadiusInKm(task.getRadiusInKm());
            questTask.setPinUrl(task.getPinUrl());
            questTask.setPinCompletedUrl(task.getPinCompletedUrl());
            questTask.setQuestTasksGroup(null);
            questTask.setImageUrl(task.getImageUrl());
            questTask.setLinkUrl(task.getLinkUrl());
            questTask.setQuestMapRouteWaypointId(task.getQuestMapRouteWaypointId());
            questTask.setActivityRecordListId(task.getActivityRecordListId());
            questTask.setTitle(task.getTitle());
            final int lastOrder = getLastTaskOrder(user.getId(), quest.getId(), null, em);
            questTask.setOrder(lastOrder + 1);

            em.persist(questTask);

            return questTask;
        } catch (final PersistenceException e) {
            Logger.error(format("Error copying task with ID '%s' to user with id '%s'", quest.getId(), user.getId()), e);

            return null;
        }
    }

    public static int getLastTaskOrder(final Integer userId, final Integer questId, final Integer groupId, final EntityManager em) {
        try {
            final TypedQuery<Integer> orderQuery = em
                    .createQuery("" +
                            "SELECT MAX(qt.order) FROM QuestTasks qt " +
                            "WHERE qt.questId = :questId" +
                            "  AND qt.userId = :userId" +
                            (groupId == null ? "" : "  AND qt.questTasksGroup.id = :groupId"), Integer.class);
            orderQuery.setParameter("questId", questId);
            orderQuery.setParameter("userId", userId);
            if (groupId != null) {
                orderQuery.setParameter("groupId", groupId);
            }

            return Optional.ofNullable(orderQuery.getSingleResult()).orElse(0);
        } catch (final NoResultException e) {
            return 0;
        } catch (final PersistenceException e) {
            Logger.error(format("Error getting last task order for Quest with ID [%s] and user with ID [%s]", questId, userId), e);
            throw e;
        }
    }

    public static List<QuestListDetailDTO> getQuestsCompletionPercentage(final User user, final EntityManager em) {
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
        if (user.getId() == null) {
            throw new RequiredParameterMissingException("user.id");
        }
        return em.createQuery("SELECT " +
                "NEW dto.QuestListDetailDTO(" +
                "q.id, " +
                "qa.mode, " +
                "COUNT(DISTINCT CASE WHEN (qt_a.taskCompleted = 'TRUE') THEN qt_a.id ELSE NULL END), " +
                "COUNT(DISTINCT qt_a.id), " +
                "COUNT(DISTINCT CASE WHEN (ct_a.taskCompleted = 'TRUE') THEN ct_a.id ELSE NULL END), " +
                "COUNT(DISTINCT ct_a.id)" +
                ") " +
                "FROM Quests q " +
                "INNER JOIN QuestActivity qa ON qa.questId = q.id " +
                "LEFT OUTER JOIN QuestTasks qt_a ON qt_a.questId = q.id AND qt_a.userId = qa.userId " +
                "LEFT OUTER JOIN QuestTasks ct_a ON ct_a.questId = q.id AND ct_a.userId = q.createdBy " +
                "WHERE qa.userId = :userId " +
                "GROUP BY q.id", QuestListDetailDTO.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    public static QuestTasks getLinkedQuestTaskForMegaQuest(final Integer megaQuestId,
                                                            final Integer linkedQuestId,
                                                            final Integer userId,
                                                            final EntityManager em) {
        return em.createQuery("SELECT qt FROM QuestTasks qt " +
                "WHERE qt.questId = :megaQuestId " +
                "AND qt.linkedQuestId = :linkedQuestId " +
                "AND qt.userId = :userId ", QuestTasks.class)
                .setParameter("megaQuestId", megaQuestId)
                .setParameter("linkedQuestId", linkedQuestId)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static List<QuestTasks> getQuestTasks(final Integer questId,
                                                 final EntityManager em) {
        return em.createQuery("SELECT qt FROM QuestTasks qt " +
                "WHERE qt.questId = :questId", QuestTasks.class)
                .setParameter("questId", questId)
                .getResultList();
    }

    public static List<QuestTasks> getLinkedQuestTasks(final Integer linkedQuestId,
                                                       final EntityManager em) {
        return em.createQuery("SELECT qt FROM QuestTasks qt " +
                "WHERE qt.linkedQuestId = :linkedQuestId", QuestTasks.class)
                .setParameter("linkedQuestId", linkedQuestId)
                .getResultList();
    }

    public static QuestTaskCompletionHistory getLastTaskCompletion(final Integer taskId, final Integer userId, final EntityManager em) {
        return em.createQuery("SELECT ch FROM QuestTaskCompletionHistory ch " +
                "WHERE ch.milestoneId = :taskId " +
                "AND ch.userTriggeredId = :userId " +
                "ORDER BY ch.dateTriggered DESC", QuestTaskCompletionHistory.class)
                .setParameter("taskId", taskId)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static List<QuestTaskCompletionHistory> getLastTaskCompletions(final Integer taskId, final EntityManager em) {
        return em.createQuery("SELECT ch FROM QuestTaskCompletionHistory ch WHERE ch.milestoneId = :taskId", QuestTaskCompletionHistory.class)
                .setParameter("taskId", taskId)
                .getResultList();
    }

    public static List<QuestTasks> getQuestTasksByQuestIdAndWaypointIsNotNull(final Integer questId, final Integer userId, final EntityManager em) {
        if (questId == null) {
            return Collections.emptyList();
        }
        if (userId == null) {
            return em.createQuery("SELECT qt FROM QuestTasks qt " +
                    "INNER JOIN Quests q ON q.id = qt.questId " +
                    "WHERE qt.questId = :questId AND qt.userId = q.createdBy AND qt.questMapRouteWaypointId IS NOT NULL", QuestTasks.class)
                    .setParameter("questId", questId)
                    .getResultList();
        } else {
            return em.createQuery("SELECT qt FROM QuestTasks qt " +
                    "WHERE qt.questId = :questId AND qt.userId = :userId AND qt.questMapRouteWaypointId IS NOT NULL", QuestTasks.class)
                    .setParameter("questId", questId)
                    .setParameter("userId", userId)
                    .getResultList();
        }
    }

    public static List<QuestTasks> getQuestTasksByQuestIdAndUserId(final Integer questId, final Integer userId, final EntityManager em) {
        if (questId == null || userId == null) {
            return Collections.emptyList();
        }
        List<QuestTasks> questTasks = em.createQuery("SELECT qt FROM QuestTasks qt " +
                " WHERE qt.questId = :questId AND qt.userId = :userId ORDER BY qt.order ASC ", QuestTasks.class)
                .setParameter("questId", questId)
                .setParameter("userId", userId)
                .getResultList();

        Map<Integer, QuestTaskGeometryDTO> taskGeometryDTOMap = findAllGeometryByQuestAndUser(questId, userId, em);

        return questTasks.stream().peek(l -> {
            Geometry geometry = taskGeometryDTOMap.get(l.getId()).getGeometry();
            if (geometry != null) {
                GeometryFactory geometryFactory = new GeometryFactory();
                l.setPoint(geometryFactory.createPoint(geometry.getCoordinate()));
            }
        }).collect(Collectors.toList());
    }

    public static Map<Integer, QuestTaskGeometryDTO> findAllGeometryByQuestAndUser(final Integer questId, final Integer userId, final EntityManager em) {
        List<QuestTaskGeometryDTO> questTaskGeometryDTOS =
                em.createQuery("SELECT NEW dto.QuestTaskGeometryDTO (" +
                        " qt.id, " +
                        " CASE WHEN qmrw.point IS NOT NULL THEN qmrw.point ELSE qt.point END " +
                        " )" +
                        " FROM QuestTasks qt " +
                        " LEFT JOIN QuestMapRouteWaypoint qmrw ON qt.questMapRouteWaypointId = qmrw.id " +
                        " WHERE qt.questId = :questId AND qt.userId = :userId ")
                        .setParameter("questId", questId)
                        .setParameter("userId", userId)
                        .getResultList();

        return questTaskGeometryDTOS.stream().collect(toMap(QuestTaskGeometryDTO::getQuestTaskId, k -> k));
    }

    public static List<QuestTaskGeometryDTO> findAllGeometryByQuestWaypointId(final List<Long> waypointIds, final EntityManager em) {
        return em.createQuery("SELECT NEW dto.QuestTaskGeometryDTO (" +
                " qt.id, " +
                " CASE WHEN qmrw.point IS NOT NULL THEN qmrw.point ELSE qt.point END " +
                " )" +
                " FROM QuestTasks qt " +
                " JOIN QuestMapRouteWaypoint qmrw ON qt.questMapRouteWaypointId = qmrw.id " +
                " WHERE qt.questMapRouteWaypointId IN (:questMapRouteWaypointId) ")
                .setParameter("questMapRouteWaypointId", waypointIds)
                .getResultList();
    }

    public static String getQuestCompletionPercentage(final List<QuestTasks> milestones) {
        final long complete = milestones.stream().filter(milestone -> toBoolean(milestone.getTaskCompleted())).count();
        final int total = milestones.size();
        final NumberFormat numberFormat = NumberFormat.getPercentInstance();
        numberFormat.setMinimumFractionDigits(0);
        if (total == 0) {
            return numberFormat.format(0.0d);
        }
        return numberFormat.format((double) complete / (double) total);
    }

    public static void setTaskCompleted(final QuestTasks task, final User modifier, final boolean checked, final EntityManager em) {
        final Date now = Date.from(Instant.now());
        task.setTaskCompleted(Boolean.valueOf(checked).toString().toUpperCase());
        task.setLastModifiedDate(now);
        task.setLastModifiedBy(modifier.getId());
        task.setTaskCompletionDate(checked ? now : null);
        em.merge(task);
    }

    public static User getTasksOwnerUserForQuest(final Quests quest, final User user, final EntityManager em) {
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user, em);
        final QuestMode mode = activity == null ? quest.getMode() : activity.getMode();
        if (PACE_YOURSELF.equals(mode)) {
            return user;
        } else if (TEAM.equals(mode)) {
            final QuestTeamMember teamMember = new QuestTeamDAO(em).getTeamMember(quest, user);
            if (teamMember == null) {
                return UserHome.findById(quest.getCreatedBy(), em);
            } else {
                return teamMember.getTeam().getCreator();
            }
        } else {
            return UserHome.findById(quest.getCreatedBy(), em);
        }
    }

    public static ActivityRecordList newActivityRecordList(Integer userId, final List<Integer> getActivitiesIds,EntityManager em) {
        Logger.debug("new Activity Record List>");
        		
        ActivityRecordList transientInstance = new ActivityRecordList();
        
        transientInstance.setCreatedBy(userId);
        transientInstance.setCreatedDate(new Date());
        transientInstance.setModifiedBy(userId);
        transientInstance.setModifiedDate(new Date());
        
        //EntityManager em = this.jpaApi.em();
        
        ActivityRecordListDAO arlDao = new ActivityRecordListDAO();
        
        ActivityRecordList activityRecordList = arlDao.persist(transientInstance, em);
        
        ActivityRecord activityRecord = null;
        		
	     if(getActivitiesIds!=null) { 
    	   for(Integer activityId:getActivitiesIds) {
	        	activityRecord = new ActivityRecord();
	        	activityRecord.setActivityId(activityId);
	        	activityRecord.setActivityRecordListId(activityRecordList.getId());
	        	activityRecord.setCreatedDate(new Date());
	        	
	        	ActivityRecordDAO arDAO = new ActivityRecordDAO();
	        	arDAO.persist(activityRecord, em);
	        }
        
       	  }

	    return activityRecordList;
       //return ok();
    }
    
    
    public static List<Integer> getActivityRecordListIdsByUser(Integer userId,final EntityManager em){
    	
    	return em.createQuery("SELECT qt.activityRecordListId FROM QuestTasks qt " +
                " WHERE qt.userId = :userId", Integer.class)
                .setParameter("userId", userId)
                .getResultList();

    }

public static Long getTotalQuestByUserAndActivityRecordService(Integer userId,List<Integer> activityRecordListIds,final EntityManager em){
    	
	final TypedQuery<Long> countQuery = em.createQuery("SELECT count(qt.questId) FROM QuestTasks qt " +
                " WHERE qt.userId = :userId AND qt.activityRecordListId IN(:activityRecordListIds) GROUP BY qt.userId", Long.class);
			
				countQuery.setParameter("userId", userId);
				countQuery.setParameter("activityRecordListIds",activityRecordListIds);

    	 return Optional.ofNullable(countQuery.getSingleResult()).orElse((long)0);

}

    
}
