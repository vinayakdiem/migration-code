package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.data.geo.Point;


@Entity
@Table(name = "as_activity_record_value")
public class AsActivityRecordValue {
	
	private Integer id;
	private Double valueNumeric;
	private String valueString;
	private String imageURL;
    private String comment;
    private Integer attributeId;
    private Integer unitId;
    private Integer actvityRecordId;
    private String title;
    private Point geoPoint;
	private Integer actitvityId;
	private Integer pillarId;
	private Integer createdBy;
	private Integer modifiedBy;
	private Date createdDate;
	private Date modifiedDate;
	private String attributeValue;
	private Boolean deleted;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "value_numeric")
	public Double getValueNumeric() {
		return valueNumeric;
	}
	public void setValueNumeric(Double valueNumeric) {
		this.valueNumeric = valueNumeric;
	}
	@Column(name = "value_string")
	public String getValueString() {
		return valueString;
	}
	public void setValueString(String valueString) {
		this.valueString = valueString;
	}
	
	@Column(name = "image_url")
	public String getImageURL() {
		return imageURL;
	}
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	
	@Column(name = "comments")
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@Column(name = "attribute_id")
	public Integer getAttributeId() {
		return attributeId;
	}
	public void setAttributeId(Integer attributeId) {
		this.attributeId = attributeId;
	}
	
	@Column(name = "attribute_unit_id")
	public Integer getUnitId() {
		return unitId;
	}
	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}
	
	@Column(name = "activity_record_id")
	public Integer getActvityRecordId() {
		return actvityRecordId;
	}
	public void setActvityRecordId(Integer actvityRecordId) {
		this.actvityRecordId = actvityRecordId;
	}
	
	@Column(name = "title")
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Column(name = "geo_point")
	public Point getGeoPoint() {
		return geoPoint;
	}
	public void setGeoPoint(Point geoPoint) {
		this.geoPoint = geoPoint;
	}
	
	@Column(name = "activity_id")
	public Integer getActitvityId() {
		return actitvityId;
	}
	public void setActitvityId(Integer actitvityId) {
		this.actitvityId = actitvityId;
	}
	
	@Column(name = "pillar_id")
	public Integer getPillarId() {
		return pillarId;
	}
	public void setPillarId(Integer pillarId) {
		this.pillarId = pillarId;
	}
	
	@Column(name = "created_by")
	public Integer getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(Integer createdBy) {
		this.createdBy = createdBy;
	}
	
	@Column(name = "modified_by")
	public Integer getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(Integer modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	
	@Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	@Column(name = "modified_date")
	public Date getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
	@Column(name = "attribute_value")
	public String getAttributeValue() {
		return attributeValue;
	}
	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}
	
	@Column(name = "deleted")
	public Boolean getDeleted() {
		return deleted;
	}
	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

}
