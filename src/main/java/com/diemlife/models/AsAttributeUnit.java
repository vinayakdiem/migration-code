package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.stereotype.Indexed;

/**
 * * Created by Raj on 08/13/22.
 */
@Entity
@Table(name = "as_attribute_unit")
@Indexed
public class AsAttributeUnit implements Serializable {

	private Integer id;
	private String abbreviation;
	private String unitNamePlural;
	private String unitNameSingular;
	//private Integer attributeId; 
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "abbreviation")
	public String getAbbreviation() {
		return abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	@Column(name = "unit_name_plural")
	public String getUnitNamePlural() {
		return unitNamePlural;
	}

	public void setUnitNamePlural(String unitNamePlural) {
		this.unitNamePlural = unitNamePlural;
	}

	@Column(name = "unit_name_singular")
	public String getUnitNameSingular() {
		return unitNameSingular;
	}

	public void setUnitNameSingular(String unitNameSingular) {
		this.unitNameSingular = unitNameSingular;
	}
	
	/*@Column(name = "attribute_id")
	public Integer getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(Integer attributeId) {
		this.attributeId = attributeId;
	}*/
}
