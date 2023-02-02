package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.TemporalType.TIMESTAMP;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "QuestTeams")
@Table(name = "quest_teams")
@NaturalIdCache
public class QuestTeam extends IdentifiedEntity {

    @NaturalId(mutable = true)
    @Column(name = "team_name", nullable = false, unique = true)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "team_cover_url")
    private String teamCoverUrl;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false, updatable = false)
    private Quests quest;

    @OneToMany(fetch = LAZY, targetEntity = QuestTeamMember.class, cascade = ALL, orphanRemoval = true, mappedBy = "team")
    @OrderBy("since DESC")
    private List<QuestTeamMember> members = new ArrayList<>();

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false, updatable = false)
    private User creator;

    @Temporal(TIMESTAMP)
    @Column(name = "created_on", nullable = false, updatable = false)
    private Date createdOn = Date.from(Instant.now());

    @Column(name = "is_default")
    private boolean defaultTeam;

    @Column(name = "is_individual")
    private boolean individualTeam;

}
