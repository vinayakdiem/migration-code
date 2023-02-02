package com.diemlife.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AsActivityAttributesDTO {
	
	public Integer activityId;
	public List<AsAttributeDTO> attributes;
	
}
