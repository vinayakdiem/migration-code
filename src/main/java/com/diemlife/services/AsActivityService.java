package com.diemlife.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import org.springframework.stereotype.Service;

import com.diemlife.dao.AsActivityDAO;
import com.diemlife.dto.AsActivityAttributesDTO;
import com.diemlife.dto.AsAttributeDTO;
import com.diemlife.dto.AsUnitDTO;

@Service
public class AsActivityService {
	
	public AsActivityAttributesDTO getAttributesAndUnitsByCategoryId(Integer activityId) {
		 
		 AsActivityAttributesDTO asActivityAttributesDTO = new AsActivityAttributesDTO(); 
		List<AsAttributeDTO> attributes = AsActivityDAO.getAttributesByActvitId(activityId);
		List<Integer> attributeIds  = new ArrayList<>();
		List<AsAttributeDTO> attributesForUI =  new ArrayList<>();
		AsAttributeDTO asAttributeDTOForUI = new AsAttributeDTO();
		
		for (AsAttributeDTO asAttributeDTO : attributes) {
			attributeIds.add(asAttributeDTO.getId());
		}
		
		 Map<Integer, List<AsUnitDTO>> unitsMap = AsActivityDAO.getUnitsByAttributeIds(attributeIds, em);
		 
		 for (AsAttributeDTO asAttributeDTO : attributes) {
			 asAttributeDTOForUI = new AsAttributeDTO();
			 	if(unitsMap.containsKey(asAttributeDTO.getId())) {
			 		asAttributeDTOForUI.setAttributeName(asAttributeDTO.getAttributeName());
			 		asAttributeDTOForUI.setId(asAttributeDTO.getId());
			 		asAttributeDTOForUI.setUnits(unitsMap.get(asAttributeDTO.getId()));
			 		attributesForUI.add(asAttributeDTOForUI);
			 	}
			}
		 asActivityAttributesDTO.setActivityId(activityId);
		 asActivityAttributesDTO.setAttributes(attributesForUI);
		 
		 return asActivityAttributesDTO;
	
	}
 }