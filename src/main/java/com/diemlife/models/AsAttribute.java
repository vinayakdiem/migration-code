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
@Table(name = "as_attribute")
@Indexed
public class AsAttribute implements Serializable {

	private Integer id;
	//private Integer attributeUnitId;
	private String name;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/*@Column(name = "attribute_unit_id")
	public Integer getAttributeUnitId() {
		return attributeUnitId;
	}

	public void setAttributeUnitId(Integer attributeUnitId) {
		this.attributeUnitId = attributeUnitId;
	}*/
	
}
