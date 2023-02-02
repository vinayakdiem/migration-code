package com.diemlife.constants;

public enum ActivityEventType {
    // Event Types
        // User events
    USER_CREATED,
   
        // Quest events
    QUEST_CREATED,
    QUEST_STARTED,
    QUEST_JOINED,
    QUEST_SUPPORTED,
    QUEST_COMPLETED,
    QUEST_CANCELED,
    QUEST_REALTIME,

        // Task events
        // Note: maybe these will be more distinct from quests in the future
    TASK_COMPLETED,

        // Team events
    TEAM_CREATED,
    TEAM_JOINED,

        // Interaction events
        // TODO: can we jam existing Newsfeed into this?
    COMMENT,
    REPLY,
    CHEER 
}
