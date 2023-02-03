package com.diemlife.forms;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;




public class CommentsForm {

   public Integer activityRecordValueId;
   public String comments;
   
   
	public Integer getActivityRecordValueId() {
		return activityRecordValueId;
	}
	public void setActivityRecordValueId(Integer activityRecordValueId) {
		this.activityRecordValueId = activityRecordValueId;
	}
	
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
}
