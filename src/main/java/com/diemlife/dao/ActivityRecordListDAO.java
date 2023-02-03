package com.diemlife.dao;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.diemlife.constants.QuestMode;
import com.diemlife.constants.VideoProvider;
import com.diemlife.dto.QuestListDetailDTO;
import com.diemlife.dto.QuestTaskGeometryDTO;
import com.diemlife.exceptions.RequiredParameterMissingException;
import forms.EmbeddedVideoForm;
import forms.QuestActionPointForm;
import forms.TaskCreateForm;
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
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.TEAM;
import static com.diemlife.dao.QuestActivityHome.getQuestActivityForQuestIdAndUser;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.MD;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.SM;
import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.XS;
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
