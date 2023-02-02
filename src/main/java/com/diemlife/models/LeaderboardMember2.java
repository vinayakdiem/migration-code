package com.diemlife.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaderboardMember2 {
    private int id;
    private long questId;
    private Long userId;
    private Integer personalInfoId;
    private Integer addressId;
    private boolean hidden;
    
    public LeaderboardMember2(int id, long questId, Long userId, Integer personalInfoId, Integer addressId, boolean hidden) {
        this.id = id;
        this.questId = questId;
        this.userId = userId;
        this.personalInfoId = personalInfoId;
        this.addressId = addressId;
        this.hidden = hidden;
    }
}
