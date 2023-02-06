package com.diemlife.dao;

import java.util.List;

import javax.persistence.EntityManager;

import com.diemlife.models.AsActivity;
import com.diemlife.models.AsPillar;

import play.Logger;

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
