package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Date;

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
@Table(name = "as_activity_record")
@Indexed
public class ActivityRecord implements Serializable {

	private Integer id;
	private Date createdDate;
	private Integer activityId;
	private Integer activityRecordListId;
	private Integer categoryId;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	@Column(name = "activity_id")
	public Integer getActivityId() {
		return activityId;
	}

	public void setActivityId(Integer activityId) {
		this.activityId = activityId;
	}

	@Column(name = "activity_record_list_id")
	public Integer getActivityRecordListId() {
		return activityRecordListId;
	}

	public void setActivityRecordListId(Integer activityRecordListId) {
		this.activityRecordListId = activityRecordListId;
	}

	@Column(name = "category_id")
	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

}
