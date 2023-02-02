package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * * Created by Raj on 09/11/22.
 */
@Entity
@Table(name = "as_user_tags")
public class AsUserTags implements Serializable {

	private Integer id;
	private String tag;
	private Integer actvityRecordValueId;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "tag")
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@Column(name = "actvity_record_value_id")
	public Integer getActvityRecordValueId() {
		return actvityRecordValueId;
	}

	public void setActvityRecordValueId(Integer actvityRecordValueId) {
		this.actvityRecordValueId = actvityRecordValueId;
	}
		
}
