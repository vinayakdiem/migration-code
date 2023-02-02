package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.TemporalType.TIMESTAMP;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "QuestTeamMembers")
@Table(name = "quest_team_members")
public class QuestTeamMember implements Serializable {

    @EmbeddedId
    private QuestTeamMemberId id;

    @ManyToOne(fetch = LAZY)
    @MapsId("teamId")
    private QuestTeam team;

    @ManyToOne(fetch = LAZY)
    @MapsId("memberId")
    private User member;

    @Temporal(TIMESTAMP)
    @Column(name = "member_since", nullable = false, updatable = false)
    private Date since = Date.from(Instant.now());

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public QuestTeamMember(final @NonNull QuestTeam team,
                           final @NonNull User member) {
        this.team = team;
        this.member = member;
        this.id = new QuestTeamMemberId(team.getId(), member.getId());
    }
    
    public QuestTeamMember(final @NonNull QuestTeam team,
                           final @NonNull User member,
                           boolean active) {
        this.team = team;
        this.member = member;
        this.id = new QuestTeamMemberId(team.getId(), member.getId());
        this.active = active;
    }

}
