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
public class AsLikesDTO {

	public Integer activityRecordValueId;
	public Long count;
	public List<UserSummaryDTO> users;
	public Integer getActivityRecordValueId() {
		return activityRecordValueId;
	}
	
	public void setActivityRecordValueId(Integer activityRecordValueId) {
		this.activityRecordValueId = activityRecordValueId;
	}
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}
	public List<UserSummaryDTO> getUsers() {
		return users;
	}
	public void setUsers(List<UserSummaryDTO> users) {
		this.users = users;
	}
	
	
}
