package com.diemlife.services;

import com.vividsolutions.jts.geom.Point;
import com.diemlife.dao.QuestActivityHome;
import com.diemlife.dao.QuestTasksDAO;
import com.diemlife.dao.QuestTeamDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dto.MilestoneCompletionDTO;
import com.diemlife.dto.MilestoneDTO;
import com.diemlife.dto.QuestLiteDTO;
import com.diemlife.dto.TaskGroupDTO;
import com.diemlife.exceptions.RequiredParameterMissingException;
import forms.QuestActionPointForm;
import com.diemlife.models.EmbeddedVideo;
import com.diemlife.models.QuestTaskCompletionHistory;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTasksGroup;
import com.diemlife.models.Quests;
import com.diemlife.models.QuestTeam2;
import com.diemlife.models.User;
import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.TransformException;

import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Optional;

import static java.lang.String.format;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;

@Singleton
public class MilestoneService {
    
    private final JPAApi jpaApi;
    private final Database dbRo;
    private final ActivityService activityService;

     @Inject
    public MilestoneService(JPAApi jpaApi, @NamedDatabase("ro") Database dbRo, ActivityService activityService) {
        this.jpaApi = jpaApi;
        this.dbRo = dbRo;
        this.activityService = activityService;
    }
    
    public QuestTasks copyMilestone(final QuestTasks milestone, final Quests newQuest, final User newDoer) {
        if (milestone == null) {
            throw new RequiredParameterMissingException("milestone");
        }
        if (newQuest == null) {
            throw new RequiredParameterMissingException("newQuest");
        }
        if (newDoer == null) {
            throw new RequiredParameterMissingException("newDoer");
        }

        EntityManager em = jpaApi.em();
        final QuestTasks newMilestone = new QuestTasks();
        final Date now = new Date();
        newMilestone.setQuestId(newQuest.getId());
        newMilestone.setTask(milestone.getTask());
        newMilestone.setTaskCompleted(Boolean.FALSE.toString().toUpperCase());
        newMilestone.setCreatedDate(now);
        newMilestone.setLastModifiedDate(now);
        newMilestone.setUserId(newDoer.getId());
        newMilestone.setOrder(milestone.getOrder());
        newMilestone.setLinkedQuestId(milestone.getLinkedQuestId());
        newMilestone.setPoint(milestone.getPoint());
        newMilestone.setRadiusInKm(milestone.getRadiusInKm());
        newMilestone.setPinUrl(milestone.getPinUrl());
        newMilestone.setPinCompletedUrl(milestone.getPinCompletedUrl());
        newMilestone.setQuestTasksGroup(null);
        newMilestone.setImageUrl(milestone.getImageUrl());
        newMilestone.setLinkUrl(milestone.getLinkUrl());
        newMilestone.setQuestMapRouteWaypointId(milestone.getQuestMapRouteWaypointId());

        final EmbeddedVideo video = milestone.getVideo();
        if (video != null) {
            final EmbeddedVideo newVideo = new EmbeddedVideo();
            newVideo.url = video.url;
            newVideo.provider = video.provider;
            if (video.thumbnails != null) {
                newVideo.thumbnails = new LinkedHashMap<>();
                video.thumbnails.forEach((key, value) -> newVideo.thumbnails.putIfAbsent(key, value));
            }
            em.persist(newVideo);

            newMilestone.setVideo(newVideo);
        }
        em.persist(newMilestone);

        return newMilestone;
    }

    public QuestTaskCompletionHistory checkMilestone(final QuestTasks milestone,
                                                            final User checker,
                                                            final boolean completed,
                                                            final QuestActionPointForm togglePoint) {
        return checkMilestone(milestone, checker, completed, togglePoint, true);
    }

    public QuestTaskCompletionHistory checkMilestone(final QuestTasks milestone,
                                                            final User checker,
                                                            final boolean completed,
                                                            final QuestActionPointForm togglePoint, boolean logActivityFeed) {
        EntityManager em = jpaApi.em();
        final Instant now = Instant.now();
        milestone.setLastModifiedDate(Date.from(now));
        milestone.setLastModifiedBy(checker.getId());
        milestone.setTaskCompleted(Boolean.valueOf(completed).toString().toUpperCase());
        milestone.setTaskCompletionDate(completed ? Date.from(now) : null);
        em.merge(milestone);

        int taskId = milestone.getId();
        int userId = checker.getId();

        final QuestTaskCompletionHistory completion = new QuestTaskCompletionHistory();
        completion.setMilestoneId(taskId);
        completion.setUserTriggeredId(userId);
        completion.setDateTriggered(Timestamp.from(Instant.now()));
        completion.setCompleted(completed);

        if (milestone.getPoint() != null && milestone.getRadiusInKm() != null) {
            final Point point = QuestTasksDAO.buildGeoPoint(togglePoint);
            completion.setPoint(point);

            if (point == null) {
                Logger.warn(format("Coordinates not provided when toggling the geo-located milestone with ID %s!", taskId));
            } else {
                try {
                    final double distanceInMeters = JTS.orthodromicDistance(milestone.getPoint().getCoordinate(), point.getCoordinate(), WGS84);
                    completion.setGeoPointInArea(milestone.getRadiusInKm().doubleValue() * 1000.0d > distanceInMeters);

                    Logger.info(format("Milestone with ID %s is%s checked within the defined area", taskId, completion.isGeoPointInArea() ? "" : " not"));
                } catch (final TransformException e) {
                    Logger.error(format("Unable to calculate distance when checking milestone with ID %s due to '%s'", milestone.getId(), e.getMessage()), e);
                }
            }
        }
        em.persist(completion);

        if (completed && logActivityFeed) {
            int questId = milestone.getQuestId();
            Double lat;
            Double lon;
            if (togglePoint == null) {
                lat = null;
                lon = null;
            } else {
                lat = togglePoint.getLatitude().doubleValue();
                lon = togglePoint.getLongitude().doubleValue();
            }

            // Record event for activity feed
            QuestTeam2 questTeam = null;
            try (Connection c = dbRo.getConnection()) {
            questTeam = QuestTeamDAO.getActiveTeamByQuestAndUser(c, questId, userId); 
            } catch (SQLException e) {
                Logger.error("checkMilestone - unable to fetch quest team", e);
                questTeam = null;
            }

            activityService.taskCompleted((long) questId, checker.getUserName(), (long) taskId, ((questTeam == null) ? null : (long) questTeam.getId()), lat, lon);
        }

        Logger.info(format("Milestone with ID %s is%s checked", milestone.getId(), completed ? "" : " un"));

        return completion;
    }

    public MilestoneDTO convertToDto(final QuestTasks task, final String envUrl, final LinkPreviewService service) {
        return populateMilestoneData(task, envUrl, service);
    }

    public TaskGroupDTO convertToDto(final QuestTasksGroup taskGroup, final String envUrl, final LinkPreviewService service) {
        final TaskGroupDTO taskGroupDTO = TaskGroupDTO.toDto(taskGroup);
        taskGroupDTO.getQuestTasks().forEach(milestoneDTO -> populateMilestoneData(milestoneDTO, envUrl, service));
        return taskGroupDTO;
    }

    private MilestoneDTO populateMilestoneData(final QuestTasks task, final String envUrl, final LinkPreviewService service) {
        if (task == null) {
            return null;
        }
        EntityManager em = jpaApi.em();
        final MilestoneDTO dto = task instanceof MilestoneDTO ? (MilestoneDTO) task : MilestoneDTO.toDto(task);
        return dto.withLastCompletion(Optional.ofNullable(QuestTasksDAO.getLastTaskCompletion(task.getId(), task.getUserId(), em))
                .map(MilestoneCompletionDTO::toDto)
                .orElse(null))
                .withLinkedQuest(Optional.ofNullable(task.getLinkedQuestId())
                        .map(linkedQuestId -> QuestsDAO.findById(task.getLinkedQuestId(), em))
                        .map(linkedQuest -> {
                            boolean hasActivity;
                            try (Connection c = dbRo.getConnection()) {
                                hasActivity = QuestActivityHome.doesQuestActivityExistForUserIdAndQuestId(c, (long) linkedQuest.getId(), (long) task.getUserId());
                            } catch (SQLException e) {
                                Logger.error("populateMilesotneData - unable to fetch quest activity", e);
                                hasActivity = false;
                            }
                            
                            final User linkedQuestUser;
                            if (hasActivity) {
                                linkedQuestUser = UserHome.findById(task.getUserId(), em);
                            } else {
                                linkedQuestUser = linkedQuest.getUser();
                            }
                            return QuestLiteDTO.toDTO(linkedQuest).withSeoSlugs(publicQuestSEOSlugs(linkedQuest, linkedQuestUser, envUrl));
                        })
                        .orElse(null))
                .withLinkPreview(service.createLinkPreview(dto.getLinkUrl()));
    }

}
