package com.diemlife.dto;


public class LeaderboardMaxActivityDTO {
   
	public String userName;
	public String userFirstName;
	public String userLastName;
	public Integer userId;
	public String imageURL;
	public String activityName;
	public Long score;
	public Integer rank;
	public String attributeValue;
	public Integer totalActivities;
	public String scoreInHrAndMinutes;
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
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
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public String getImageURL() {
		return imageURL;
	}
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	public String getActivityName() {
		return activityName;
	}
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}
	public Long getScore() {
		return score;
	}
	public void setScore(Long score) {
		this.score = score;
	}
	public Integer getRank() {
		return rank;
	}
	public void setRank(Integer rank) {
		this.rank = rank;
	}
	public String getAttributeValue() {
		return attributeValue;
	}
	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}
	
	public Integer getTotalActivities() {
		return totalActivities;
	}
	public void setTotalActivities(Integer totalActivities) {
		this.totalActivities = totalActivities;
	}
	
	public String getScoreInHrAndMinutes() {
		return scoreInHrAndMinutes;
	}
	public void setScoreInHrAndMinutes(String scoreInHrAndMinutes) {
		this.scoreInHrAndMinutes = scoreInHrAndMinutes;
	}
	
	@Override
	public String toString() {
		return "LeaderboardMaxActivityDTO [userName=" + userName + ", userFirstName=" + userFirstName
				+ ", userLastName=" + userLastName + ", userId=" + userId + ", imageURL=" + imageURL + ", activityName="
				+ activityName + ", score=" + score + ", rank=" + rank + ", attributeValue=" + attributeValue
				+ ", totalActivities=" + totalActivities + "]";
	}
	
		
}
