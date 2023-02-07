package com.diemlife.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.diemlife.constants.ActivityEventType;
import com.diemlife.constants.ActivityUnit;
import com.diemlife.dao.ActivityDAO;
import com.diemlife.dao.ActivityGeoDAO;
import com.diemlife.dao.ActivityRawDAO;
import com.diemlife.dao.ActivityRecordDAO;
import com.diemlife.dao.AsActivityDAO;
import com.diemlife.dao.QuestTasksDAO;
import com.diemlife.dao.QuestTeamDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dto.ActivityDTO;
import com.diemlife.dto.AllPillarsCount;
import com.diemlife.models.Activity;
import com.diemlife.models.ActivityRaw;
import com.diemlife.models.ActivityRecord;
import com.diemlife.models.AsActivity;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTeam2;
import com.diemlife.models.Quests2;
import com.diemlife.models.User2;

import play.Logger;

@Service
public class ActivityService {

    private static final int QUERY_LIMIT_DEFAULT = 15;
    private static final int QUERY_LIMIT_MAX = 25;

    // 10 km
    private static double LEVEL5_RADIUS_METERS_DEFAULT = 10000;
    
    // 100 km
    private static double LEVEL5_RADIUS_METERS_MAX = 100000;

    // 1000 km
    private static double LEVEL2_RADIUS_METERS_DEFAULT = 1000000;
    
    // 10000 km
    private static double LEVEL2_RADIUS_METERS_MAX = 10000000;

    @Autowired
    private ActivityRawDAO arDao;
   
    @Autowired
    private ActivityDAO aDao;
    
    @Autowired
    private ActivityGeoDAO agDao;
  

    public List<Activity> getActivity(String uid) {
        return aDao.get(uid);
    }

    public List<Activity> getCheersForActivity(String uid) {
        return aDao.getByTargetActivityAndEventType(uid, ActivityEventType.CHEER, null, false);
    }

    public List<Activity> getCommentsForActivity(String uid) {
        return aDao.getByTargetActivityAndEventType(uid, ActivityEventType.REPLY, null, false);
    }

    private static Integer selectLimit(Integer limit) {
        return ((limit == null) ? QUERY_LIMIT_DEFAULT : Math.min(limit, QUERY_LIMIT_MAX));
    }

    private static double selectRadius(int level, Double radius) {
        double result;
        switch (level) {
            case 2:
                result = ((radius == null) ? LEVEL2_RADIUS_METERS_DEFAULT : Math.min(radius, LEVEL2_RADIUS_METERS_MAX));
                break;
            case 5:
            default:
                result = ((radius == null) ? LEVEL5_RADIUS_METERS_DEFAULT : Math.min(radius, LEVEL5_RADIUS_METERS_MAX));
                break;
        }

        return result;
    }

    public List<Activity> getActivityByUser(String username, Long ts, Integer limit) {
        return aDao.getByUser(username, ts, selectLimit(limit));
    }

    public List<Activity> getRecentActivityByUser(String username, Integer limit) {
        return aDao.getRecentByUser(username, selectLimit(limit));
    }

    public List<Activity> getActivityByQuest(long questId, Long ts, Integer limit) {
        return aDao.getByQuest(questId, ts, selectLimit(limit));
    }

    public List<Activity> getRecentActivityByQuest(long questId, Integer limit) {
        return aDao.getRecentByQuest(questId, selectLimit(limit));
    }

    public List<Activity> getActivityByTask(long taskId, Long ts, Integer limit) {
        return aDao.getByTask(taskId, ts, selectLimit(limit));
    }

    public List<Activity> getRecentActivityByTask(long taskId, Integer limit) {
        return aDao.getRecentByTask(taskId, selectLimit(limit));
    }

    public List<Activity> getActivityByTeam(long teamId, Long ts, Integer limit) {
        return aDao.getByTeam(teamId, ts, selectLimit(limit));
    }

    public List<Activity> getRecentActivityByTeam(long teamId, Integer limit) {
        return aDao.getRecentByTeam(teamId, selectLimit(limit));
    }

    public List<Activity> getActivityByTs(long ts, Integer limit) {
        return aDao.getByTs(ts, selectLimit(limit));
    }

    public List<Activity> getActivityByGeo(int level, double lat, double lon, double radius, Long ts, Integer limit) {
        Set<String> uids = agDao.getActivityByGeo(level, lat, lon, selectRadius(level, radius), ts, selectLimit(limit));
        if (uids.isEmpty()) {
            return new LinkedList<Activity>();
        }

        return aDao.getBatch(uids);
    }

    public List<ActivityDTO> activityToDto(List<Activity> activityList, String sessionUsername) {
        // Remember lookups as we go -- we should add proper caching later
        HashMap<String, User2> userCache = new HashMap<String, User2>();
        HashMap<Long, Quests2> questsCache = new HashMap<Long, Quests2>();
        HashMap<Long, QuestTeam2> questTeamCache = new HashMap<Long, QuestTeam2>();

        List<ActivityDTO> activityDtoList = new LinkedList<ActivityDTO>();
        for (Activity activity : activityList) {
            String uid = activity.getUid();

            ActivityDTO activityDto = new ActivityDTO();
            activityDto.setUid(uid);
            activityDto.setTs(activity.getTs());
            activityDto.setActivityType(activity.getEventType().toString());
            activityDto.setLat(activity.getLat());
            activityDto.setLon(activity.getLon());
            activityDto.setPostalCode(activity.getPostalCode());

            String username = activity.getUsername();
            activityDto.setUsername(username);
          
            User2 u2 = null;
            if (username != null) {
                if ((u2 = userCache.get(username)) == null) {
                    try (Connection c = dbRo.getConnection()) {
                        u2 = UserHome.getUserByUsername(c, username);
                        userCache.put(username, u2);
                    } catch (SQLException e) {
                        Logger.error("activityToDto - unable to find user: " + username, e);

                        // skip activity record
                        continue;
                    }
                }
            }
            activityDto.setFirstname(((u2 == null) ? null : u2.getFirstname()));
            activityDto.setLastname(((u2 == null) ? null : u2.getLastname()));

            Long questId = activity.getQuestId();
            activityDto.setQuestId(questId);
            activityDto.setTaskId(activity.getTaskId());
           
            if (activity.getQuestId() != null && activity.getTaskId() != null) {
                final List<QuestTasks> filteredTask = QuestTasksDAO.getQuestTasks(activity.getQuestId().intValue()) == null ?
                        null :
                        QuestTasksDAO.getQuestTasks(activity.getQuestId().intValue(), em).stream()
                                .filter(t -> t.getId() == activity.getTaskId().intValue())
                                .collect(Collectors.toList());
                if (filteredTask != null && filteredTask.size() == 1) {
                    String task = filteredTask.get(0) != null ? filteredTask.get(0).getTask() : "";
                    activityDto.setTask(task.length() >= 40 ? task.substring(0, 40) + "..." : task);
                }
            }

            activityDto.setProfilePictureURL(UserHome.findByName(activity.getUsername(), em) == null ? "" : UserHome.findByName(activity.getUsername(), em).getProfilePictureURL());

            Quests2 q2 = null;
            if (questId != null) {
                if ((q2 = questsCache.get(questId)) == null) {
                    try (Connection c = dbRo.getConnection()) {
                        q2 = QuestsDAO.getQuest(c, questId);
                        questsCache.put(questId, q2);
                    } catch (SQLException e) {
                        Logger.error("activityToDto - unable to find quest: " + questId, e);

                        // skip activity record
                        continue;
                    }
                }
            }
            activityDto.setQuestTitle(((q2 == null) ? null : q2.getTitle()));

            Long teamId = activity.getTeamId();
            activityDto.setTeamId(teamId);

            QuestTeam2 qt2 = null;
            if (teamId != null) {
                if ((qt2 = questTeamCache.get(teamId)) == null) {
                    try (Connection c = dbRo.getConnection()) {
                        qt2 = QuestTeamDAO.getTeam(c, teamId);
                        questTeamCache.put(teamId, qt2);
                    } catch (SQLException e) {
                        Logger.error("activityToDto - unbale to team: " + teamId, e);

                        // skip activity record
                        continue;
                    }
                }
            }
            activityDto.setTeamName(((qt2 == null) ? null : qt2.getTeamName()));

            Long cheerCount = activity.getCheerCount();

            activityDto.setCheers(cheerCount);
            activityDto.setComment(activity.getComment());
            activityDto.setCommentImgUrl(activity.getCommentImgUrl());
            activityDto.setTargetActivity(activity.getTargetActivityUid());
            activityDto.setDeleted(activity.getDeleted());

            if ((cheerCount == null) || (cheerCount == 0)) {
                activityDto.setCheeredByUser(null);
            } else {
                activityDto.setCheeredByUser(aDao.isCheeredByUser(activity, sessionUsername));
            }

            activityDto.setQuantity(activity.getQuantity());
            ActivityUnit unit = activity.getUnit();
            if (unit == null) {
                activityDto.setUnit(null);
            } else {
                activityDto.setUnit(unit.toString());
            }
            activityDto.setTag(activity.getTag());

            activityDtoList.add(activityDto);
        }

        return activityDtoList;
    }

    // TODO: change the logging here so that we could reconstruct lost events from java logs if necessary OR
    // need a place to dump failures.  Thusly, in the short term we could just write out what we wanted to do to the java logs and something can scan for that
    // and fix it.  In the longer term, we could write failures to S3, RDS, or some other service.

    public void questCreated(long questId, String username) {
        String msg = username + " created quest " + questId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.QUEST_CREATED, msg, username, null, questId, null, null, null, null, null, null, null, null)) == null) {
            Logger.error("questCreated - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("questCreated - unable to write activity");
            }
        }
    }

    public void questStarted(long questId, String username, Long teamId) {
        String msg = username + " started quest " + questId;
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.QUEST_STARTED, msg, username, teamId, questId, null, null, null, null, null, null, null, null)) == null) {
            Logger.error("questStarted - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("questStarted - unable to write activity");
            }
        }
    }

    public void questJoined(long questId, String username, Long teamId) {
        String msg = username + " joined quest " + questId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.QUEST_JOINED, msg, username, teamId, questId, null, null, null, null, null, null, null, null)) == null) {
            Logger.error("questJoined - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("questJoined - unable to write activity");
            }
        }
    }

    public void questCompleted(long questId, String username, Long teamId, Double lat, Double lon) {
        String msg = username + " completed quest " + questId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.QUEST_COMPLETED, msg, username, teamId, questId, null, lat, lon, null, null, null, null, null)) == null) {
            Logger.error("questCompleted - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("questCompleted - unable to write activity");
            } else {
                if ((lat != null) && (lon != null) && !agDao.insert(activity.getUid(), lat, lon)) {
                    Logger.error("questCompleted - unable to write geo lookup");
                }
            }
        }
    }

    public void questCanceled(long questId, String username, Long teamId) {
        String msg = username + " canceled quest " + questId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.QUEST_CANCELED, msg, username, teamId, questId, null, null, null, null, null, null, null, null)) == null) {
            Logger.error("questCanceled - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("questCanceled - unable to write activity");
            }
        }
    }

    public void questRealtime(long questId, String username, Long teamId, Double lat, Double lon, Double quantity, ActivityUnit unit, String tag) {
        String msg = username + " logged realtime " + quantity + " " + unit + " for " + tag; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.QUEST_REALTIME, msg, username, teamId, questId, null, lat, lon, null, null, quantity, unit, tag)) == null) {
            Logger.error("questRealtime - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("questRealtime - unable to write activity");
            }
        }
    }

    public void taskCompleted(long questId, String username, long taskId, Long teamId, Double lat, Double lon) {
        String msg = username + " completed task " + taskId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.TASK_COMPLETED, msg, username, teamId, questId, taskId, lat, lon, null, null, null, null, null)) == null) {
            Logger.error("taskCompleted - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("taskCompleted - unable to write activity");
            } else {
                if ((lat != null) && (lon != null) && !agDao.insert(activity.getUid(), lat, lon)) {
                    Logger.error("taskCompleted - unable to write geo lookup");
                }
            }
        }
    }

    public void teamCreated(long questId, String username, Long teamId) {
        String msg = username + " created team " + teamId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_TEAM, ActivityEventType.TEAM_CREATED, msg, username, teamId, questId, null, null, null, null, null, null, null, null)) == null) {
            Logger.error("teamCreated - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("teamCreated - unable to write activity");
            }
        }
    }

    public void teamJoined(long questId, String username, Long teamId) {
        String msg = username + " joined team " + teamId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_TEAM, ActivityEventType.TEAM_JOINED, msg, username, teamId, questId, null, null, null, null, null, null, null, null)) == null) {
            Logger.error("teamJoined - unable to write raw activity");
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("teamJoined - unable to write activity");
            }
        }
    }

    public String cheerActivity(String username, String targetActivityUid) {

        // Ensure the activity exists
        List<Activity> cheers = aDao.get(targetActivityUid);
        Activity cheerMe = ((cheers == null) ? null : cheers.get(0));
        if (cheerMe != null) {
            if (cheerMe.isCheerable()) {
                return aDao.cheerActivity(cheerMe, username);
                //This below section left commented out to enable self-cheering on posts
//                if (!username.equals(cheerMe.getUsername())) {
//                    return aDao.cheerActivity(cheerMe, username);
//                } else {
//                    Logger.error("cheerActivity - attempted to cheer activity " + targetActivityUid + ", which is self-owned by " + username);
//                }
            } else {
                Logger.error("cheerActivity - attempted to cheer activity " + targetActivityUid + ", which isn't cheerable");
            }
        } else {
            Logger.warn("cheerActivity - attempted to cheer activity " + targetActivityUid + " which wasn't found.");
        }

        return null;
    }

    public boolean uncheerActivity(String username, String targetActivityUid) {
        boolean result = false;

        // Ensure the activity exists
        List<Activity> cheers = aDao.get(targetActivityUid);
        Activity cheerMe = ((cheers == null) ? null : cheers.get(0));
        if (cheerMe != null) {
            if (cheerMe.isCheerable()) {
                if (!username.equals(cheerMe.getUsername())) {
                    return aDao.uncheerActivity(cheerMe, username);
                } else {
                    Logger.error("uncheerActivity - attempted to uncheer activity " + targetActivityUid + ", which is self-owned by " + username);
                }
            } else {
                Logger.warn("uncheerActivity - attempted to uncheer activity " + targetActivityUid + ", which isn't cheerable");
            }
        } else {
            Logger.warn("uncheerActivity - attempted to uncheer activity " + targetActivityUid + " which wasn't found.");
        }

        return result;
    }

    // Post a comment to user's feed
    public boolean postUserComment(String username, String targetUsername, String comment, String commentImgUrl) {
        // You can't post a top-level comment to someone else's feed
        if (!username.equals(targetUsername)) {
            return false;
        }

        String msg = username + " posted comment to user feed " + targetUsername; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_USER, ActivityEventType.COMMENT, msg, targetUsername, null, null, null, null, null, comment, commentImgUrl, null, null, null)) == null) {
            Logger.error("postUserComment - unable to write raw activity");
            return false;
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("postUserComment - unable to write activity");
                return false;
            }
        }

        return true;
    }

    // Post a comment to a quest's feed
    public boolean postQuestComment(String username, long targetQuestId, Long targetTaskId, String comment, String commentImgUrl) {

        String msg = username + " posted comment to quest feed " + targetQuestId + ", with taskId " + ((targetTaskId == null) ? "null" : targetTaskId); 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_QUEST, ActivityEventType.COMMENT, msg, username, null, targetQuestId, targetTaskId, null, null, comment, commentImgUrl, null, null, null)) == null) {
            Logger.error("postQuestComment - unable to write raw activity");
            return false;
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("postQuestComment - unable to write activity");
                return false;
            }
        }

        return true;
    }

    // Post a comment to a team's feed
    public boolean postTeamComment(String username, long targetTeamId, String comment, String commentImgUrl) {
        
        String msg = username + " posted comment to quest feed " + targetTeamId; 
        ActivityRaw activityRaw;
        if ((activityRaw = arDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_TEAM, ActivityEventType.COMMENT, msg, username, targetTeamId, null, null, null, null, comment, commentImgUrl, null, null, null)) == null) {
            Logger.error("postTeamComment - unable to write raw activity");
            return false;
        } else {
            Activity activity = aDao.insert(activityRaw);
            if (activity == null) {
                Logger.error("postTeamComment - unable to write activity");
                return false;
            }
        }

        return true;
    }

    // Post reply
    public boolean postTargetedComment(String username, String commentTargetActivityUid, String comment, String commentImgUrl) {

        String msg = username + " posted a reply to " + commentTargetActivityUid;
        Activity activity = aDao.insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_USER, 0, ActivityEventType.REPLY, msg, username, null, null, null, null, null, comment, commentImgUrl,
            commentTargetActivityUid, null, null, null, null);
        if (activity == null) {
            Logger.error("postTargetedComment - unable to write activity");
            return false;
        }
        return true;
    }

    // Edit comment
    public boolean editComment(String username, String commentTargetActivityUid, String comment, String commentImgUrl) {

        boolean result = false;

        boolean commentEmpty = ((comment == null) || comment.isEmpty());
        boolean commentImgUrlEmpty = ((commentImgUrl == null) || commentImgUrl.isEmpty());
        if (!commentEmpty || !commentImgUrlEmpty) {

            // Lookup the existing comment
            List<Activity> activityList = aDao.get(commentTargetActivityUid);
            Activity activity = ((activityList == null) ? null : activityList.get(0));
            if (activity != null) {
                if (activity.isComment() || activity.isReply()) {
                    String currentComment = activity.getComment();
                    String currentCommentImgUrl = activity.getCommentImgUrl();

                    // Only do updates if we were passed in a non-empty value and it differs from what we already had
                    String newComment = ((!commentEmpty && !comment.equals(currentComment)) ? comment : null);
                    String newCommentImgUrl = ((!commentImgUrlEmpty && !commentImgUrl.equals(currentCommentImgUrl)) ? commentImgUrl : null);
                    if ((newComment != null) || (newCommentImgUrl != null)) {
                        if (aDao.updateComment(activity, newComment, newCommentImgUrl, currentComment, currentCommentImgUrl)) {
                           result = true; 
                        } else {
                            Logger.error("editComment - unable to edit comment for activity " + commentTargetActivityUid);
                        }
                    } else {
                        // else an edit was posted to set it the same thing, just pretend all is well
                        return true;
                    }
                } else {
                    Logger.error("editComment - activity is not a comment or a reply " + commentTargetActivityUid);
                }
            } else {
                Logger.error("editComment - cannot find target activity " + commentTargetActivityUid);
            }
        } else {
            Logger.error("editComment - no meaningful edit posted for " + commentTargetActivityUid);
        }

        return result;
    }

    // Delete comment
    public boolean deleteComment(String username, String commentTargetActivityUid) {
        boolean result = false;

        // Lookup the existing comment
        List<Activity> activityList = aDao.get(commentTargetActivityUid);
        Activity activity = ((activityList == null) ? null : activityList.get(0));
        if (activity != null) {
            if (activity.isComment() || activity.isReply()) {
                if (username.equals(activity.getUsername())) {
                    if (aDao.deleteComment(activity)) {
                        result = true; 
                    } else {
                        Logger.error("deleteComment - unable to edit comment for activity " + commentTargetActivityUid);
                    }
                } else {
                    Logger.error("deleteComment - activity " + commentTargetActivityUid + " is not owned by user " + username);
                }
            } else {
                Logger.error("deleteComment - activity is not a comment or a reply " + commentTargetActivityUid);
            }
        } else {
            Logger.error("deleteComment - cannot find target activity " + commentTargetActivityUid);
        }

        return result;
    }
    
   public List<AsActivity> getActivityByQuestForAllTasks(Integer questId, EntityManager em) {
	   
	   List<QuestTasks> questTasks =  QuestTasksDAO.getQuestTasks(questId,em);
	   
	   List<Integer> activityRecordListIds = new ArrayList<>();
	   Set<Integer> activityIds = new HashSet<>();
	   List<AsActivity> activities = new ArrayList<>();
	   
	   for(QuestTasks questTask: questTasks) {
		   activityRecordListIds.add(questTask.getActivityRecordListId());
	   }
	   
	   List<ActivityRecord> activityRecords = new ArrayList<>();
	   if(activityRecordListIds.size()>0) {
		   activityRecords = ActivityRecordDAO.getActvityRecordByActivityRecordListIds(activityRecordListIds, em);
	   }
	   
	   for(ActivityRecord activityRecord:activityRecords) {
		   activityIds.add(activityRecord.getActivityId());
	   }
	   
	   
	   if(activityIds.size()>0) {
		   activities = AsActivityDAO.getActvitiesByIds(activityIds,em);
		   
	   }
	   
	   return activities;
	   
   }
 
 public List<ActivityRecord> getActivityIdsByActivityRecordList(List<Integer> activityRecordListIds, EntityManager em){
	 
	 List<ActivityRecord> activityRecords = new ArrayList<>();
	 activityRecords = ActivityRecordDAO.getActvityRecordByActivityRecordListIds(activityRecordListIds, em);
	 return activityRecords;
 } 
 
 public void createActivitiesByIdAndListId(List<Integer> newActivities, Integer activityRecordListId, EntityManager em) {
	 
	   ActivityRecord activityRecord = null;
		
	   for(Integer activityId:newActivities) {
        	activityRecord = new ActivityRecord();
        	activityRecord.setActivityId(activityId);
        	activityRecord.setActivityRecordListId(activityRecordListId);
        	activityRecord.setCreatedDate(new Date());
        	
        	ActivityRecordDAO arDAO = new ActivityRecordDAO();
        	arDAO.persist(activityRecord, em);
        }
    
 }
 
 public void deleteActivitiesById(List<Integer> actvivityRecordIds,EntityManager em) {
	 
	   ActivityRecordDAO arDAO = new ActivityRecordDAO();
      	arDAO.deleteActvityRecordByActivityIds(actvivityRecordIds, em);
      }
 
 
 public List<AllPillarsCount> getTotalPillarsByActivityIds(List<Integer> activityIds ,EntityManager em) {
	 
	
	 List<AllPillarsCount> allPillarsCounts =  AsActivityDAO.getTotalPillarsByActivityIds(activityIds,em);
	  
	 return allPillarsCounts; 
	 
}
 
 public List<Integer> getActivityRecordListByPillar(Integer pillarId ,EntityManager em) {
	 
		
	 List<Integer> activityRecordListCount =  AsActivityDAO.getActivityRecordListByPillar(pillarId,em);
	  
	 return activityRecordListCount; 
	 
}
 
public Long getTotalQuestByUserAndActivityRecordService(Integer userId,List<Integer> activityRecordList,EntityManager em) {
	return QuestTasksDAO.getTotalQuestByUserAndActivityRecordService(userId,activityRecordList,em);
} 
 }