package dao;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import constants.QuestMode;
import constants.VideoProvider;
import dto.QuestListDetailDTO;
import dto.QuestTaskGeometryDTO;
import exceptions.RequiredParameterMissingException;
import forms.EmbeddedVideoForm;
import forms.QuestActionPointForm;
import forms.TaskCreateForm;
import models.ActivityRecordList;
import models.EmbeddedVideo;
import models.EmbeddedVideo.ThumbnailKey;
import models.QuestActivity;
import models.QuestTaskCompletionHistory;
import models.QuestTasks;
import models.QuestTasksGroup;
import models.QuestTeamMember;
import models.Quests;
import models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static constants.QuestMode.PACE_YOURSELF;
import static constants.QuestMode.TEAM;
import static dao.QuestActivityHome.getQuestActivityForQuestIdAndUser;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static models.EmbeddedVideo.ThumbnailSizes.MD;
import static models.EmbeddedVideo.ThumbnailSizes.SM;
import static models.EmbeddedVideo.ThumbnailSizes.XS;
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.upperCase;

/**
 * Created by Raj on 8/13/22.
 */
public class ActivityRecordListDAO {


    public ActivityRecordList persist(ActivityRecordList transientInstance, EntityManager em) {

        try {
            em.persist(transientInstance);
            //em.flush();
            
            return transientInstance;
            
        } catch (Exception e) {
            Logger.error("ActivityRecordListDAO :: persist : error persisting Acitivity Record List => " + e, e);
        }
        
        return null;
    }

   

    public static ActivityRecordList findById(Integer id, EntityManager em) {
        try {
        	ActivityRecordList activityRecordList = em.find(ActivityRecordList.class, id);
            if (activityRecordList != null) {
                return activityRecordList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
