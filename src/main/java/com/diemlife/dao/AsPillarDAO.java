package com.diemlife.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.AsActivity;
import com.diemlife.models.AsPillar;

import play.Logger;

/**
 * Created by Raj on 8/13/22.
 */
@Repository
public class AsPillarDAO {
	
	@PersistenceContext
	EntityManager em;

    public void persist(AsActivity transientInstance) {

        try {
            em.persist(transientInstance);
        } catch (Exception e) {
            Logger.error("ActivityRecordDAO :: persist : error persisting Acitivity Record List => " + e, e);
        }
    }

    public AsPillar findById(Integer id) {
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
    
    
    public List<AsPillar> findAllPillars() {
        try {
        	
        	return  em.createQuery("SELECT asp FROM AsPillar asp").getResultList();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }    
    
}
