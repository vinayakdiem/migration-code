package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class LeaderboardAttribute implements Serializable {

    private String id;
    private String name;
    private boolean ascendingScoring;
    private boolean questDefault;
    private String unit;
    private long creator;
    private Date createdOn;

    public LeaderboardAttribute(String id, String name, boolean ascendingScoring, boolean questDefault, String unit, long creator, Date createdOn) {
        this.id = id;
        this.name = name;
        this.ascendingScoring = ascendingScoring;
        this.questDefault = questDefault;
        this.unit = unit;
        this.creator = creator;
        this.createdOn = createdOn;
    }

    @JsonGetter("asc")
    public boolean isAsc() {
        return ascendingScoring;
    }

}
