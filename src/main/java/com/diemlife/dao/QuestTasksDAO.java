package com.diemlife.dao;

import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.TEAM;
//FIXME Vinayak
//import static com.diemlife.dao.QuestActivityHome.getQuestActivityForQuestIdAndUser;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.MD;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.SM;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.XS;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
//FIXME Vinayak
//import static org.apache.commons.lang.BooleanUtils.toBoolean;
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
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

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
@Repository
public class QuestTasksDAO {

	@PersistenceContext
	EntityManager em;
	
    public class CompletionPercentage {
        public Integer questId;
        public String completionPercentage;
    }

    public void persist(QuestTasks transientInstance) {

        try {
            em.persist(transientInstance);
        } catch (Exception e) {
            Logger.error("QuestTasksDAO :: persist : error persisting quest task => " + e, e);
        }
    }

    public void remove(QuestTasks persistentInstance) {
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

    public QuestTasks findById(Integer id) {
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

    public Point buildGeoPoint(final QuestActionPointForm togglePoint) {
        return togglePoint == null ? null : new GeometryFactory().createPoint(new Coordinate(
        		//FIXME Vinayak
//                togglePoint.getLatitude().doubleValue(),
//                togglePoint.getLongitude().doubleValue()
        ));
    }

    public QuestTasks addNewTask(final User creator,
                                        final User assignee,
                                        final Quests quest,
                                        final TaskCreateForm task) {
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
        
        //FIXME Vinayak
//        ActivityRecordList activityRecordList =  newActivityRecordList(creator.getId(),task.getActivitiesIds());
        
        try {
            final Date now = new Date();
            final QuestTasks questTask = new QuestTasks();

          //FIXME Vinayak
//            if (task.getVideo() != null) {
//                questTask.setVideo(createEmbeddedVideo(task.getVideo(), em));
//            }

            questTask.setQuestId(quest.getId());
            questTask.setUserId(assignee.getId());
          //FIXME Vinayak
//            questTask.setTask(task.getTask());
            questTask.setTaskCompleted(Boolean.FALSE.toString().toUpperCase());
            questTask.setCreatedBy(creator.getId());
            questTask.setCreatedDate(now);
          //FIXME Vinayak
//            questTask.setLinkedQuestId(task.getLinkedQuestId());
//            questTask.setPoint(buildGeoPoint(task.getPoint()));
//            questTask.setRadiusInKm(task.getRadiusInKm());
//            questTask.setQuestTasksGroup(task.getGroupId() == null ? null : em.find(QuestTasksGroup.class, task.getGroupId()));
//            questTask.setImageUrl(task.getImageUrl());
//            questTask.setLinkUrl(task.getLinkUrl());
//            questTask.setActivityRecordListId(activityRecordList.getId());
//            questTask.setTitle(task.getTitle());

          //FIXME Vinayak
//            final int lastOrder = getLastTaskOrder(assignee.getId(), quest.getId(), task.getGroupId());
//            questTask.setOrder(lastOrder + 1);

            em.persist(questTask);

            return questTask;
        } catch (final PersistenceException e) {
            Logger.error("QuestTasksDAO :: addNewTask : error adding new task for assignee = " + assignee.getId() + " => " + e.getMessage(), e);

            return null;
        }
    }

    public EmbeddedVideo createEmbeddedVideo(final EmbeddedVideoForm videoForm) {
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

    public QuestTasks copyTaskWithoutGroupToUser(final QuestTasks task,
                                                        final User user,
                                                        final Quests quest) {
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
            final int lastOrder = getLastTaskOrder(user.getId(), quest.getId(), null);
            questTask.setOrder(lastOrder + 1);

            em.persist(questTask);

            return questTask;
        } catch (final PersistenceException e) {
            Logger.error(format("Error copying task with ID '%s' to user with id '%s'", quest.getId(), user.getId()), e);

            return null;
        }
    }

    public int getLastTaskOrder(final Integer userId, final Integer questId, final Integer groupId) {
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

    public List<QuestListDetailDTO> getQuestsCompletionPercentage(final User user) {
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

    public QuestTasks getLinkedQuestTaskForMegaQuest(final Integer megaQuestId,
                                                            final Integer linkedQuestId,
                                                            final Integer userId) {
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

    public List<QuestTasks> getQuestTasks(final Integer questId) {
        return em.createQuery("SELECT qt FROM QuestTasks qt " +
                "WHERE qt.questId = :questId", QuestTasks.class)
                .setParameter("questId", questId)
                .getResultList();
    }

    public List<QuestTasks> getLinkedQuestTasks(final Integer linkedQuestId) {
        return em.createQuery("SELECT qt FROM QuestTasks qt " +
                "WHERE qt.linkedQuestId = :linkedQuestId", QuestTasks.class)
                .setParameter("linkedQuestId", linkedQuestId)
                .getResultList();
    }

    public QuestTaskCompletionHistory getLastTaskCompletion(final Integer taskId, final Integer userId) {
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

    public List<QuestTaskCompletionHistory> getLastTaskCompletions(final Integer taskId) {
        return em.createQuery("SELECT ch FROM QuestTaskCompletionHistory ch WHERE ch.milestoneId = :taskId", QuestTaskCompletionHistory.class)
                .setParameter("taskId", taskId)
                .getResultList();
    }

    public List<QuestTasks> getQuestTasksByQuestIdAndWaypointIsNotNull(final Integer questId, final Integer userId) {
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

    public List<QuestTasks> getQuestTasksByQuestIdAndUserId(final Integer questId, final Integer userId) {
        if (questId == null || userId == null) {
            return Collections.emptyList();
        }
        List<QuestTasks> questTasks = em.createQuery("SELECT qt FROM QuestTasks qt " +
                " WHERE qt.questId = :questId AND qt.userId = :userId ORDER BY qt.order ASC ", QuestTasks.class)
                .setParameter("questId", questId)
                .setParameter("userId", userId)
                .getResultList();

        Map<Integer, QuestTaskGeometryDTO> taskGeometryDTOMap = findAllGeometryByQuestAndUser(questId, userId);

        return questTasks.stream().peek(l -> {
            Geometry geometry = taskGeometryDTOMap.get(l.getId()).getGeometry();
            if (geometry != null) {
                GeometryFactory geometryFactory = new GeometryFactory();
                l.setPoint(geometryFactory.createPoint(geometry.getCoordinate()));
            }
        }).collect(Collectors.toList());
    }

    public Map<Integer, QuestTaskGeometryDTO> findAllGeometryByQuestAndUser(final Integer questId, final Integer userId) {
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

    public List<QuestTaskGeometryDTO> findAllGeometryByQuestWaypointId(final List<Long> waypointIds) {
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

    public String getQuestCompletionPercentage(final List<QuestTasks> milestones) {
    	//FIXME Vinayak
//        final long complete = milestones.stream().filter(milestone -> toBoolean(milestone.getTaskCompleted())).count();
        final int total = milestones.size();
        final NumberFormat numberFormat = NumberFormat.getPercentInstance();
        numberFormat.setMinimumFractionDigits(0);
        if (total == 0) {
            return numberFormat.format(0.0d);
        }
      //FIXME Vinayak
        return numberFormat.format(0);
//        return numberFormat.format((double) complete / (double) total);
    }

    public void setTaskCompleted(final QuestTasks task, final User modifier, final boolean checked) {
        final Date now = Date.from(Instant.now());
        task.setTaskCompleted(Boolean.valueOf(checked).toString().toUpperCase());
        task.setLastModifiedDate(now);
        task.setLastModifiedBy(modifier.getId());
        task.setTaskCompletionDate(checked ? now : null);
        em.merge(task);
    }

    public User getTasksOwnerUserForQuest(final Quests quest, final User user) {
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
      //FIXME Vinayak
//        final QuestActivity activity = getQuestActivityForQuestIdAndUser(quest, user);
//        final QuestMode mode = activity == null ? quest.getMode() : activity.getMode();
//        if (PACE_YOURSELF.equals(mode)) {
//            return user;
//        } else if (TEAM.equals(mode)) {
        	//FIXME Vinayak
            final QuestTeamMember teamMember = new QuestTeamDAO().getTeamMember(quest, user);
            if (teamMember == null) {
//                return UserHome.findById(quest.getCreatedBy());
            } else {
//                return teamMember.getTeam().getCreator();
            }
//        } else {
        	//FIXME Vinayak
//            return UserHome.findById(quest.getCreatedBy());
//        }
          //FIXME Vinayak
        return null;
    }

    public ActivityRecordList newActivityRecordList(Integer userId, final List<Integer> getActivitiesIds) {
        Logger.debug("new Activity Record List>");
        		
        ActivityRecordList transientInstance = new ActivityRecordList();
        
        transientInstance.setCreatedBy(userId);
        transientInstance.setCreatedDate(new Date());
        transientInstance.setModifiedBy(userId);
        transientInstance.setModifiedDate(new Date());
            
        ActivityRecordListDAO arlDao = new ActivityRecordListDAO();
        
        ActivityRecordList activityRecordList = arlDao.persist(transientInstance);
        
        ActivityRecord activityRecord = null;
        		
	     if(getActivitiesIds!=null) { 
    	   for(Integer activityId:getActivitiesIds) {
	        	activityRecord = new ActivityRecord();
	        	activityRecord.setActivityId(activityId);
	        	activityRecord.setActivityRecordListId(activityRecordList.getId());
	        	activityRecord.setCreatedDate(new Date());
	        	
	        	ActivityRecordDAO arDAO = new ActivityRecordDAO();
	        	arDAO.persist(activityRecord);
	        }
        
       	  }

	    return activityRecordList;
       //return ok();
    }
    
    
    public List<Integer> getActivityRecordListIdsByUser(Integer userId){
    	
    	return em.createQuery("SELECT qt.activityRecordListId FROM QuestTasks qt " +
                " WHERE qt.userId = :userId", Integer.class)
                .setParameter("userId", userId)
                .getResultList();

    }

public Long getTotalQuestByUserAndActivityRecordService(Integer userId,List<Integer> activityRecordListIds){
    	
	final TypedQuery<Long> countQuery = em.createQuery("SELECT count(qt.questId) FROM QuestTasks qt " +
                " WHERE qt.userId = :userId AND qt.activityRecordListId IN(:activityRecordListIds) GROUP BY qt.userId", Long.class);
			
				countQuery.setParameter("userId", userId);
				countQuery.setParameter("activityRecordListIds",activityRecordListIds);

    	 return Optional.ofNullable(countQuery.getSingleResult()).orElse((long)0);

}

    
}
