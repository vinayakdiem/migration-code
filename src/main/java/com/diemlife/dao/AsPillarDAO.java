package com.diemlife.dao;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import constants.QuestMode;
import constants.VideoProvider;
import dto.AllPillarsCount;
import dto.QuestListDetailDTO;
import dto.QuestTaskGeometryDTO;
import exceptions.RequiredParameterMissingException;
import forms.EmbeddedVideoForm;
import forms.QuestActionPointForm;
import forms.TaskCreateForm;
import models.ActivityRecord;
import models.ActivityRecordList;
import models.AsActivity;
import models.AsPillar;
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
public class AsPillarDAO {


    public void persist(AsActivity transientInstance, EntityManager em) {

        try {
            em.persist(transientInstance);
        } catch (Exception e) {
            Logger.error("ActivityRecordDAO :: persist : error persisting Acitivity Record List => " + e, e);
        }
    }

    public static AsPillar findById(Integer id, EntityManager em) {
        try {
        	AsPillar asPillar = em.find(AsPillar.class, id);
            if (asPillar != null) {
                return asPillar;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public static List<AsPillar> findAllPillars(EntityManager em) {
        try {
        	
        	return  em.createQuery("SELECT asp FROM AsPillar asp").getResultList();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }    
    
}
