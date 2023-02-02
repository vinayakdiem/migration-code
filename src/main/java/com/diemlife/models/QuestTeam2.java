package com.diemlife.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestTeam2 {
    private int id;
    private String teamName;
    private String logoUrl;
    private long questId;
    private long creatorUserId;
    private long createdOn;
    private boolean isDefault;
    private boolean isIndividual;
    
    public QuestTeam2(int id, String teamName, String logoUrl, long questId, long creatorUserId, long createdOn, boolean isDefault, 
        boolean isIndividual)
    {
        this.id = id;
        this.teamName = teamName;
        this.logoUrl = logoUrl;
        this.questId = questId;
        this.creatorUserId = creatorUserId;
        this.createdOn = createdOn;
        this.isDefault = isDefault;
        this.isIndividual = isIndividual;
    }
}
