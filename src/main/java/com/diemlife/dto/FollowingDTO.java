package com.diemlife.dto;

import java.io.Serializable;

public class FollowingDTO implements Serializable {

    public int questId;
    public long count;

    public FollowingDTO(final int questId, final long count) {
        this.questId = questId;
        this.count = count;
    }

    public static FollowingDTO toDTO(final int questId, final long count) {
        return new FollowingDTO(questId, count);
    }

}
