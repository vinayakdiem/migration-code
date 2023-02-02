package com.diemlife.dto;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static com.diemlife.utils.URLUtils.relativeTeamUrl;

import java.io.Serializable;
import java.util.List;

import com.diemlife.models.QuestTeam;
import com.diemlife.models.QuestTeamMember;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuestTeamDTO implements Serializable {

    public final Long teamId;
    public final Integer creatorId;
    public final Integer questId;
    public final String teamName;
    public final String teamPageUrl;
    public final String teamLogoUrl;
    public final String teamCoverUrl;
    public final Integer[] memberIds;
    public final Integer amountBacked;
    public final boolean defaultTeam;

    public static QuestTeamDTO from(final QuestTeam team) {
        return from(team, emptyList());
    }

    public static QuestTeamDTO from(final QuestTeam team, final List<QuestMemberDTO> backers) {
        return new QuestTeamDTO(
                team.getId(),
                team.getCreator().getId(),
                team.getQuest().getId(),
                team.getName(),
                relativeTeamUrl(team),
                team.getLogoUrl(),
                team.getTeamCoverUrl(),
                team.getMembers().stream().filter(QuestTeamMember::isActive).map(teamMember -> teamMember.getMember().getId()).collect(toList()).toArray(new Integer[]{}),
                backers.stream().mapToInt(backer -> backer.amountBacked.stream().mapToInt(Number::intValue).sum()).sum(),
                team.isDefaultTeam()
        );
    }

}
