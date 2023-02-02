package com.diemlife.dto;

import java.io.Serializable;

import com.diemlife.constants.QuestActivityStatus;
import com.diemlife.constants.QuestMode;

public class MyQuestDTO implements Serializable {
    public Long questId;
    public Long userId;
    public Boolean questActivityExists;
    public QuestMode questActivityMode;
    public QuestActivityStatus questActivityStatus;
    public Boolean questStarred;
    public Boolean questInProgress;
    public Boolean questCompleted;
    public Boolean questSaved;
    public Boolean questFollowed;
    public Boolean questRepeatable;
    public Long questCompleteCounter;
    public Long tasksCompletedDoer;
    public Long tasksTotalDoer;
    public Long tasksCompletedCreator;
    public Long tasksTotalCreator;
}
