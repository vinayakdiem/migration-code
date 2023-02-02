package com.diemlife.models;

import lombok.Getter;

@Getter
public class QuestLeaderboard {

    // Totals
    private static String TAG_AGGREGATE_INDIVIDUAL = "AGGREGATE_INDIVIDUAL";
    private static String TAG_AGGREGATE_TEAM = "AGGREGATE_TEAM";
    
    // Average
    private static String TAG_AGGREGATE_TEAM_AVERAGE = "AGGREGATE_TEAM_AVERAGE";
    
    private long questId;
    private String leaderboardSlug;
    private Long attributeId;
    private String attributeTag;
    private Double conversion;
    private boolean hidden;
    private Integer ordinal;

    public QuestLeaderboard(long questId, String leaderboardSlug, long attributeId, String attributeTag, Double conversion, boolean hidden, Integer ordinal) {
        this.questId = questId;
        this.leaderboardSlug = leaderboardSlug;
        this.attributeId = attributeId;
        this.attributeTag = attributeTag;
        this.conversion = conversion;
        this.hidden = hidden;
        this.ordinal = ordinal;
    }

    public boolean isAggregateTag() {
        return (isAggregateIndividualTag() || isAggregateTeamTag() || isAggregateTeamAverageTag());
    }

    public boolean isAggregateIndividualTag() {
        return TAG_AGGREGATE_INDIVIDUAL.equals(attributeTag);
    }

    public boolean isAggregateTeamTag() {
        return TAG_AGGREGATE_TEAM.equals(attributeTag);
    }
    
    public boolean isAggregateTeamAverageTag() {
        return TAG_AGGREGATE_TEAM_AVERAGE.equals(attributeTag);
    }
}
