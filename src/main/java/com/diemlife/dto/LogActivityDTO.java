/**
 * 
 */
package com.diemlife.dto;

import java.util.Date;
import java.util.List;

/**
 * @author Raj
 *
 */
public class LogActivityDTO {

	public Integer actvityRecordValueId;
	public String imageURL;
	public String title;
	public String comment;
	public String attributeName;
	public String attributeValue;
	public String abbreviation;
	public String unitNamePlural;
	public String unitNameSingular;
	public String activityName;
	public List<String> tags;
	public String userFirstName;
	public String userLastName;
	public String userName;
	public String userImageUrl;
	public Integer userId;
	public String pillarName;
	public String creationDate;
	public String creationDateTime;
	public List<AsCommentsDTO> userComments;
	public AsLikesDTO likes;
	public String email;
	public boolean updated;
	
	public Integer getActvityRecordValueId() {
		return actvityRecordValueId;
	}
	public void setActvityRecordValueId(Integer actvityRecordValueId) {
		this.actvityRecordValueId = actvityRecordValueId;
	}
	public String getImageURL() {
		return imageURL;
	}
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public String getAttributeValue() {
		return attributeValue;
	}
	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}
	public String getAbbreviation() {
		return abbreviation;
	}
	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}
	public String getUnitNamePlural() {
		return unitNamePlural;
	}
	public void setUnitNamePlural(String unitNamePlural) {
		this.unitNamePlural = unitNamePlural;
	}
	public String getUnitNameSingular() {
		return unitNameSingular;
	}
	public void setUnitNameSingular(String unitNameSingular) {
		this.unitNameSingular = unitNameSingular;
	}
	public String getActivityName() {
		return activityName;
	}
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserImageUrl() {
		return userImageUrl;
	}
	public void setUserImageUrl(String userImageUrl) {
		this.userImageUrl = userImageUrl;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public String getPillarName() {
		return pillarName;
	}
	public void setPillarName(String pillarName) {
		this.pillarName = pillarName;
	}
	public String getUserFirstName() {
		return userFirstName;
	}
	public void setUserFirstName(String userFirstName) {
		this.userFirstName = userFirstName;
	}
	public String getUserLastName() {
		return userLastName;
	}
	public void setUserLastName(String userLastName) {
		this.userLastName = userLastName;
	}
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	public String getCreationDateTime() {
		return creationDateTime;
	}
	public void setCreationDateTime(String creationDateTime) {
		this.creationDateTime = creationDateTime;
	}
	public List<AsCommentsDTO> getUserComments() {
		return userComments;
	}
	public void setUserComments(List<AsCommentsDTO> userComments) {
		this.userComments = userComments;
	}
	public AsLikesDTO getLikes() {
		return likes;
	}
	public void setLikes(AsLikesDTO likes) {
		this.likes = likes;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public boolean isUpdated() {
		return updated;
	}
	public void setUpdated(boolean updated) {
		this.updated = updated;
	}
	
	
	
}
