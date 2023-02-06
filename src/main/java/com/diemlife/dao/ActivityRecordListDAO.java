package com.diemlife.dao;

import javax.persistence.EntityManager;

import com.diemlife.models.ActivityRecordList;

import play.Logger;

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
