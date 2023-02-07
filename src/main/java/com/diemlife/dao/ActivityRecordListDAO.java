package com.diemlife.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.ActivityRecordList;

import play.Logger;

/**
 * Created by Raj on 8/13/22.
 */
@Repository
public class ActivityRecordListDAO {
	@PersistenceContext
	EntityManager em;

    public ActivityRecordList persist(ActivityRecordList transientInstance) {

        try {
            em.persist(transientInstance);
            //em.flush();
            
            return transientInstance;
            
        } catch (Exception e) {
            Logger.error("ActivityRecordListDAO :: persist : error persisting Acitivity Record List => " + e, e);
        }
        
        return null;
    }

   

    public ActivityRecordList findById(Integer id) {
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
