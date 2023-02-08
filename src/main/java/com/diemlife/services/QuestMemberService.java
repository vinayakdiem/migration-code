package com.diemlife.services;

import com.diemlife.constants.QuestEdgeType;
import com.diemlife.constants.QuestMemberStatus;
import com.diemlife.dao.*;
import com.diemlife.dto.QuestMemberDTO;
import com.diemlife.dto.QuestTeamDTO;
import com.diemlife.models.*;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import play.db.Database;
import play.Logger;
import play.db.NamedDatabase;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import java.security.SecureRandom;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

import static com.diemlife.constants.QuestMemberStatus.Backer;
import static com.diemlife.dao.UserHome.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Service
public class QuestMemberService {

	@Autowired
    private SecureRandom random = new SecureRandom();
	
	@Autowired
    private Consumer<QuestMemberDTO> randomId = member -> member.userId = (member.userId > 0
            ? member.userId
            : nextNegativeInt(random));
    
    @Autowired
    private Database dbRo;


    private Collection<QuestMemberDTO> buildMembers(QuestTeamDAO teamDao, Quests quest, User doer) {
        final QuestTeamMember teamCreator = teamDao.getTeamMember(quest, doer);
        final QuestTeam team = teamCreator == null ? null : teamCreator.getTeam();
        final boolean isDoerTeamCreator = teamCreator != null && !team.isDefaultTeam() && teamCreator.getMember().getId().equals(team.getCreator().getId());
        final List<QuestMemberDTO> backers = getQuestBackings(quest.getId(), quest.getUser().getId(), (team == null || team.isDefaultTeam() || !quest.isFundraising())
                ? null
                : team.getCreator().getId()).stream().peek(randomId).collect(toList());
        final List<QuestMemberDTO> ticketBuyers = UserHome.getQuestParticipants(quest.getId(), quest.getUser().getId()).stream().peek(randomId).collect(toList());
        final List<QuestMemberDTO> usersDoing = UserHome.getUserQuestActivityByQuestId(quest.getId()).stream().peek(randomId).collect(toList());
        final List<QuestMemberDTO> usersSaved = QuestSavedDAO.getSavedMembersForQuest(quest).stream().peek(randomId).collect(toList());
        final Map<Integer, QuestMemberDTO> members = new LinkedHashMap<>();
        mergeMembers(members, backers);
        mergeMembers(members, ticketBuyers);
        mergeMembers(members, usersDoing);
        mergeMembers(members, usersSaved);

        CompanyRoleDAO companyRoleDAO= new CompanyRoleDAO();
        CompanyRole companyRole = companyRoleDAO.getCompanyRoleForUser(doer);
        final User companyUser = companyRole!=null?companyRole.getUser():new User();

        return members.values()
                .stream()
                .filter(questMember-> {
                    assert companyUser != null;
                    return !Objects.equals(questMember.getUserId(), companyUser.getId());
                })
                .filter(questMember -> {
                    if (isDoerTeamCreator) {
                        return (
							(team.getMembers().stream().anyMatch(teamMember -> isActiveTeamMember(teamMember, questMember.userId) || isBacker(questMember))) ||
							isTeamParticipant(team, questMember)
                        );
                    } else {
                        return true;
                    }
                })
                .peek(member -> {
                    if (quest.getCreatedBy().equals(member.userId)) {
                        member.addStatus(QuestMemberStatus.Creator);
                    }
                    if (quest.getAdmins().stream().anyMatch(admin -> admin.getId().equals(member.userId))) {
                        member.addStatus(QuestMemberStatus.Admin);
                    }
                })
                .sorted(comparing(QuestMemberDTO::getSortingScore).reversed())
                .collect(toList());
    }

    public Collection<QuestMemberDTO> getQuestMembers(final Quests quest, final User doer) {
        final Map<Integer, QuestMemberDTO> members = new LinkedHashMap<>();
        final QuestTeamDAO teamDao = new QuestTeamDAO();

        Integer questId = quest.getId();
        Integer doerId = quest.getUser().getId();

        List<QuestEdge> children;
        try (Connection c = dbRo.getConnection()) {
            QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
            children = qeDao.getEdgesByType(c, questId, QuestEdgeType.CHILD.toString());
        } catch (Exception e) {
            Logger.error("buildQuestPageData - error with edges", e);
            children = new LinkedList<QuestEdge>();
        } 
        boolean isMegaQuest = !children.isEmpty();

        if (isMegaQuest) {
            // This gets ugly from a merging standpoint but should work
            for (QuestEdge edge : children) {
                // Lookup the quest for now.  Eliminate this later if we can.
                long childQuestId = edge.getQuestDst();
                Quests childQuest = QuestsDAO.findById((int) childQuestId);
                final User childCreator = UserService.getById(childQuest.getCreatedBy());
                mergeMembers(members, buildMembers(teamDao, childQuest, childCreator));
            }
        } else {
            // This should perform almost the same for single quest as before
            mergeMembers(members, buildMembers(teamDao, quest, doer));
        }

        return members.values();
    }

    private boolean isActiveTeamMember(final QuestTeamMember teamMember, final Integer userId) {
        return teamMember.isActive() && teamMember.getMember().getId().equals(userId);
    }

    private boolean isBacker(final QuestMemberDTO questMember) {
        return questMember.memberStatus.contains(Backer);
    }

    private boolean isTeamParticipant(QuestTeam team, QuestMemberDTO questMember) {
		Long targetTeamId = team.getId();
		Number questMemberTeamId = questMember.teamId;
		return ((targetTeamId != null) && targetTeamId.equals(questMemberTeamId));
	}

    private Collection<QuestTeamDTO> buildTeams(QuestTeamDAO dao, Quests quest) {
       return dao.listTeamsForQuest(quest, false, true).stream()
                .map(team -> QuestTeamDTO.from(team, getQuestRaisedFunds(team.getQuest().getId(), team.getCreator().getId())))
                .sorted((left, right) -> ObjectUtils.compare(right.amountBacked, left.amountBacked, false))
                .collect(toList()); 
    }

    public Collection<QuestTeamDTO> getQuestTeams(final @NotNull Quests quest) {
        final QuestTeamDAO dao = new QuestTeamDAO();
       
        // TODO: factor this out into some shared thing
        Integer questId = quest.getId();
        long _questId = (long) questId.intValue();
        List<QuestEdge> children;
        try (Connection c = dbRo.getConnection()) {
            QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
            children = qeDao.getEdgesByType(c, _questId, QuestEdgeType.CHILD.toString());
        } catch (Exception e) {
            Logger.error("getQuestTeams - error with edges", e);
            children = new LinkedList<QuestEdge>();
        } 
        boolean isMegaQuest = !children.isEmpty();

        Logger.debug("getQuestTeams - " + _questId);

        Collection<QuestTeamDTO> teams = new LinkedList<QuestTeamDTO>();
        if (isMegaQuest) {
            // This gets ugly from a merging standpoint but should work
            for (QuestEdge edge : children) {
                // Lookup the quest for now.  Eliminate this later if we can.
                long childQuestId = edge.getQuestDst();
                Quests childQuest = QuestsDAO.findById((int) childQuestId);

                teams.addAll(buildTeams(dao, childQuest));
            }
        } else {
            // This should perform almost the same for single quest as before 
            teams.addAll(buildTeams(dao, quest));
        }
        
        return teams;
    }

    public QuestTeamDTO getQuestTeam(final @NotNull Quests quest, final @NotNull User doer, final boolean checkDefault) {
        final QuestTeamDAO dao = new QuestTeamDAO();

        // TODO: factor this out into some shared thing
        Integer questId = quest.getId();
        long _questId = (long) questId.intValue();
        Logger.debug("getQuestTeam - " + _questId);

        QuestTeam team = dao.getTeamForQuestAndUser(quest, doer, checkDefault);
        if (team == null) {
            return null;
        }

        QuestTeamDTO teamDto = QuestTeamDTO.from(team);

        return teamDto;
    }

    public QuestTeamDTO getTeam(final @NotNull Long teamId) {
        final QuestTeamDAO dao = new QuestTeamDAO();
        return Optional.ofNullable(dao.load(teamId, QuestTeam.class))
                .map(team -> QuestTeamDTO.from(team, getQuestRaisedFunds(team.getQuest().getId(), team.getCreator().getId())))
                .orElse(null);
    }

    private static void mergeMembers(final Map<Integer, QuestMemberDTO> members, final Collection<QuestMemberDTO> sublist) {
        sublist.forEach(member -> members.merge(member.userId, member, (base, delta) -> {
            delta.memberStatus.forEach(base::addStatus);
            delta.amountBacked.forEach(base::addBacking);
            return base;
        }));
    }

    private static int nextNegativeInt(final Random random) {
        final int next = random.nextInt();
        if (next < 0) {
            return next;
        } else {
            return nextNegativeInt(random);
        }
    }

}
