package com.diemlife.dao;

import java.util.List;

import javax.persistence.EntityManager;

import com.diemlife.models.ActivityRecord;

import play.Logger;

/**
 * Created by Raj on 8/13/22.
 */
public class ActivityRecordDAO {


    public void persist(ActivityRecord transientInstance, EntityManager em) {

        try {
            em.persist(transientInstance);
        } catch (Exception e) {
            Logger.error("ActivityRecordDAO :: persist : error persisting Acitivity Record List => " + e, e);
        }
    }

    public static ActivityRecord findById(Integer id, EntityManager em) {
        try {
        	ActivityRecord activityRecord = em.find(ActivityRecord.class, id);
            if (activityRecord != null) {
                return activityRecord;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public static List<ActivityRecord> getActvityRecordByActivityRecordListIds(final List<Integer> activityRecordListIds,
            final EntityManager em) {
    			return em.createQuery("SELECT ar FROM ActivityRecord ar " +
    						"WHERE ar.activityRecordListId IN(:activityRecordListIds)", ActivityRecord.class)
    						.setParameter("activityRecordListIds", activityRecordListIds)
    						.getResultList();
}

   public static void deleteActvityRecordByActivityIds(final List<Integer> actvivityRecordIds,
            final EntityManager em) {
    			em.createQuery("DELETE FROM ActivityRecord ar " +
    						"WHERE ar.id IN(:actvivityRecordIds)")
    						.setParameter("actvivityRecordIds", actvivityRecordIds)
    						.executeUpdate();
    }
}
