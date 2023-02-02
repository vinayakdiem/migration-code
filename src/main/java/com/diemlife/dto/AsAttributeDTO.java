package com.diemlife.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsAttributeDTO {
	Integer id;
	String attributeName;
	public List<AsUnitDTO> units;

}
