package com.diemlife.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
class QuestTeamMemberId implements Serializable {

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "member_id")
    private Integer memberId;

}
