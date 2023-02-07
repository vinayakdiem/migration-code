package com.diemlife.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.ActivityRecord;

import play.Logger;

/**
 * Created by Raj on 8/13/22.
 */

@Repository
public class ActivityRecordDAO {


	@PersistenceContext
	private EntityManager em;
	
    public void persist(ActivityRecord transientInstance) {

        try {
            em.persist(transientInstance);
        } catch (Exception e) {
            Logger.error("ActivityRecordDAO :: persist : error persisting Acitivity Record List => " + e, e);
        }
    }

    public ActivityRecord findById(Integer id) {
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
    
    
    public List<ActivityRecord> getActvityRecordByActivityRecordListIds(final List<Integer> activityRecordListIds) {
    			return em.createQuery("SELECT ar FROM ActivityRecord ar " +
    						"WHERE ar.activityRecordListId IN(:activityRecordListIds)", ActivityRecord.class)
    						.setParameter("activityRecordListIds", activityRecordListIds)
    						.getResultList();
}

   public void deleteActvityRecordByActivityIds(final List<Integer> actvivityRecordIds) {
    			em.createQuery("DELETE FROM ActivityRecord ar " +
    						"WHERE ar.id IN(:actvivityRecordIds)")
    						.setParameter("actvivityRecordIds", actvivityRecordIds)
    						.executeUpdate();
    }
}
