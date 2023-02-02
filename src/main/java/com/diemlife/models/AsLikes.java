package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "as_comment_like")
public class AsLikes {
	
	private Integer id;
	private Integer activityRecordValueId;
	private Integer questId;
	private Integer createdBy;
	private Date createdDate;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "activity_record_value_id")
	public Integer getActivityRecordValueId() {
		return activityRecordValueId;
	}
	public void setActivityRecordValueId(Integer activityRecordValueId) {
		this.activityRecordValueId = activityRecordValueId;
	}
	
	@Column(name = "quest_id")
	public Integer getQuestId() {
		return questId;
	}
	public void setQuestId(Integer questId) {
		this.questId = questId;
	}
	
	@Column(name = "user_id")
	public Integer getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(Integer createdBy) {
		this.createdBy = createdBy;
	}
	
	@Column(name = "created_on")
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	
}
