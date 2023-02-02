package com.diemlife.models;

import java.io.Serializable;

import com.sun.istack.NotNull;

//import constants.LeaderboardMemberStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaderboardScore implements Serializable {

    private long memberId;
    private String attributeId;
    private double score;
    private int memberStatus;
    private boolean hasStarted;
    private boolean hasFinished;
    private boolean outOfRace;

    public LeaderboardScore(long memberId, @NotNull String attributeId, double score, int memberStatus, boolean hasStarted, boolean hasFinished,
        boolean outOfRace)
    {
        this.memberId = memberId;
        this.attributeId = attributeId;
        this.score = score;
        this.memberStatus = memberStatus;
        this.hasStarted = hasStarted;
        this.hasFinished = hasFinished;
        this.outOfRace = outOfRace;
    }

    public boolean getHasStarted() {
        return this.hasStarted;
    }

    public boolean getHasFinished() {
        return this.hasFinished;
    }

    public boolean getOutOfRace() {
        return this.outOfRace;
    }
}
