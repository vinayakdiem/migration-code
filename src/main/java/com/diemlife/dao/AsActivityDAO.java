package com.diemlife.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.dto.AllPillarsCount;
import com.diemlife.dto.AsActivityAttributesDTO;
import com.diemlife.dto.AsAttributeDTO;
import com.diemlife.dto.AsUnitDTO;
import com.diemlife.models.AsActivity;

import play.Logger;

/**
 * Created by Raj on 8/13/22.
 */
@Repository
public class AsActivityDAO {
	
	@PersistenceContext
	EntityManager em;

    public void persist(AsActivity transientInstance) {
        try {
            em.persist(transientInstance);
        } catch (Exception e) {
            Logger.error("ActivityRecordDAO :: persist : error persisting Acitivity Record List => " + e, e);
        }
    }

    public AsActivity findById(Integer id) {
        try {
        	AsActivity asActivity = em.find(AsActivity.class, id);
            if (asActivity != null) {
                return asActivity;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public List<AsActivity> getActvitiesByIds(final Set<Integer> ids) {
    			return em.createQuery("SELECT aa FROM AsActivity aa " +
    						"WHERE aa.id IN(:ids)", AsActivity.class)
    						.setParameter("ids", ids)
    						.getResultList();
    }

    
    public List<AllPillarsCount> getTotalPillarsByActivityIds(List activityIds) {
      	 
    	String  SQL = "SELECT pil.name,count(*),act.pillarId As count FROM AsActivity act " + 
    			"inner join AsPillar pil on act.pillarId= pil.id " + 
    			"where act.id in(:activityIds) group by act.pillarId";
    	
    	List<Object[]> results = em.createQuery(SQL, Object[].class)
                .setParameter("activityIds", activityIds)
                .getResultList();
    	
    	List<AllPillarsCount> pillasList = new ArrayList(); 
     
    	for (Object[] row : results) {
    		AllPillarsCount container = new AllPillarsCount();
            container.setName((String) row[0]);
            container.setCount((Long)row[1]);
            container.setId((Integer)row[2]);
            pillasList.add(container);
        }
    
    	return pillasList;
    }
    
    public List<AsAttributeDTO> getAttributesByActvitId(Integer activityId) {
     	 
    	String SQLNew = "SELECT asa.name, asa.id from AsAttribute asa inner join AsActivityAttribute aaa on asa.id = aaa.attributeId where aaa.activityId = :activityId";
    	//String  SQL = "SELECT name, id from AsAttribute where activityId = :activityId";
    	
    	
    	List<Object[]> results = em.createQuery(SQLNew, Object[].class)
                .setParameter("activityId", activityId)
                .getResultList();
    	 
    	AsActivityAttributesDTO asActivityAttributesDTO = new AsActivityAttributesDTO();
    	List<AsAttributeDTO> attributes = new ArrayList();
    	for (Object[] row : results) {
    		AsAttributeDTO attribute = new AsAttributeDTO();
    		AsUnitDTO unit = new AsUnitDTO();
    		//FIXME Vinayak
//    		attribute.setAttributeName((String) row[0]);
//    		attribute.setId((Integer)row[1]);
    		attributes.add(attribute);
    	}
    
    	return attributes;
    }


    public Map<Integer, List<AsUnitDTO>> getUnitsByAttributeIds(List attributeIds) {
    	
    	List<AsAttributeDTO> attributes = new ArrayList();
    	List<AsUnitDTO> units = new ArrayList();
    	Map<Integer, List<AsUnitDTO>> attributesUnit = new HashMap<>();
    	
    	String unitSQL = "SELECT unitId FROM AsAttributeUnitList where attributeId in(:attributeIds)";
    	
    	List<Integer> unitIds = em.createQuery(unitSQL, Integer.class)
                .setParameter("attributeIds", attributeIds)
                .getResultList();
    	
    	
    	
    	if(unitIds!=null && unitIds.size()>0) {
	    	String  SQL = "SELECT aau.attributeId, unit.id, unit.abbreviation, unit.unitNamePlural,unit.unitNameSingular FROM AsAttributeUnit unit inner join AsAttributeUnitList aau on unit.id= aau.unitId " + 
	    			" where unit.id in(:unitIds) order by aau.id";
	    	
	    	List<Object[]> results = em.createQuery(SQL, Object[].class)
	               .setParameter("unitIds", unitIds)
	                .getResultList();
	    	 
	    	
	    	for (Object[] row : results) {
	    		AsUnitDTO unit = new AsUnitDTO();
	    		if(!attributesUnit.containsKey((Integer)row[0])) {
	    			units = new ArrayList();
	    		}
	    		
	    		//FIXME Vinayak
//	    		unit.setId((Integer)row[1]);
//	    		unit.setAbbreviation((String)row[2]);
//	    		unit.setUnitNamePlural((String)row[3]);
//	    		unit.setUnitNameSingular((String)row[4]);
	    		units.add(unit);
	    		attributesUnit.put((Integer)row[0],units);
	    		
	        }
    	}
    	return attributesUnit;
    }
    
    public List<Integer> getActivityRecordListByPillar(Integer pillarId) {
     	 
    	String  SQL = "SELECT actRecord.activityRecordListId FROM AsActivity act " + 
    			"inner join ActivityRecord actRecord on act.id= actRecord.activityId " + 
    			"where act.pillarId=:pillarId group by actRecord.activityRecordListId";
    	
    	List<Integer> results = em.createQuery(SQL, Integer.class)
                .setParameter("pillarId", pillarId)
                .getResultList();
    	
    	return results;
    }
}
