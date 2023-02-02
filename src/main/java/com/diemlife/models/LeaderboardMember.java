package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "LeaderboardMembers")
@Table(name = "leaderboard_member", uniqueConstraints = {
        @UniqueConstraint(name = "leaderboard_member_quest_user_ux", columnNames = {"quest_id", "platform_user_id"}),
        @UniqueConstraint(name = "leaderboard_member_quest_personal_ux", columnNames = {"quest_id", "personal_info_id"})
})
public class LeaderboardMember extends IdentifiedEntity {

    @ManyToOne(targetEntity = Quests.class)
    @JoinColumn(name = "quest_id")
    private Quests quest;

    @ManyToOne(targetEntity = User.class)
    @JoinColumn(name = "platform_user_id")
    private User platformUser;

    @ManyToOne(targetEntity = PersonalInfo.class)
    @JoinColumn(name = "personal_info_id")
    private PersonalInfo personalInfo;

    @ManyToOne(targetEntity = Address.class)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "hidden")
    private boolean hidden;
}
