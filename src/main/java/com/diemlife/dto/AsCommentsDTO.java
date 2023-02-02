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
public class AsCommentsDTO {

	public Integer actvityRecordValueId;
	public String comment;
	public String userFirstName;
	public String userLastName;
	public String userName;
	public String userImageUrl;
	public Integer userId;
	public String creationDate;
	public String creationDateTime;
	public Integer commentId;
	
	
	public Integer getActvityRecordValueId() {
		return actvityRecordValueId;
	}
	public void setActvityRecordValueId(Integer actvityRecordValueId) {
		this.actvityRecordValueId = actvityRecordValueId;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
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
	public Integer getCommentId() {
		return commentId;
	}
	public void setCommentId(Integer commentId) {
		this.commentId = commentId;
	}
	
		
}
