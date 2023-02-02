package com.diemlife.services;

import com.diemlife.constants.QuestEdgeType;
import com.diemlife.constants.QuestMemberStatus;
import com.diemlife.dao.*;
import com.diemlife.dto.QuestMemberDTO;
import com.diemlife.dto.QuestTeamDTO;
import models.*;
import org.apache.commons.lang3.ObjectUtils;
import play.db.Database;
import play.Logger;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import java.security.SecureRandom;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

import static com.diemlife.constants.QuestMemberStatus.Backer;
import static com.diemlife.dao.UserHome.getQuestBackings;
import static com.diemlife.dao.UserHome.getQuestRaisedFunds;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Singleton
public class QuestMemberService {

    private final JPAApi jpaApi;
    private final SecureRandom random = new SecureRandom();
    private final Consumer<QuestMemberDTO> randomId = member -> member.userId = (member.userId > 0
            ? member.userId
            : nextNegativeInt(random));
    private Database dbRo;

    @Inject
    public QuestMemberService(final JPAApi jpaApi, @NamedDatabase("ro") Database dbRo) {
        this.jpaApi = jpaApi;
        this.dbRo = dbRo;
    }

    private Collection<QuestMemberDTO> buildMembers(EntityManager em, QuestTeamDAO teamDao, Quests quest, User doer) {
        final QuestTeamMember teamCreator = teamDao.getTeamMember(quest, doer);
        final QuestTeam team = teamCreator == null ? null : teamCreator.getTeam();
        final boolean isDoerTeamCreator = teamCreator != null && !team.isDefaultTeam() && teamCreator.getMember().getId().equals(team.getCreator().getId());
        final List<QuestMemberDTO> backers = getQuestBackings(quest.getId(), quest.getUser().getId(), (team == null || team.isDefaultTeam() || !quest.isFundraising())
                ? null
                : team.getCreator().getId(), em).stream().peek(randomId).collect(toList());
        final List<QuestMemberDTO> ticketBuyers = UserHome.getQuestParticipants(quest.getId(), quest.getUser().getId(), em).stream().peek(randomId).collect(toList());
        final List<QuestMemberDTO> usersDoing = UserHome.getUserQuestActivityByQuestId(quest.getId(), em).stream().peek(randomId).collect(toList());
        final List<QuestMemberDTO> usersSaved = QuestSavedDAO.getSavedMembersForQuest(quest, em).stream().peek(randomId).collect(toList());
        final Map<Integer, QuestMemberDTO> members = new LinkedHashMap<>();
        mergeMembers(members, backers);
        mergeMembers(members, ticketBuyers);
        mergeMembers(members, usersDoing);
        mergeMembers(members, usersSaved);

        CompanyRoleDAO companyRoleDAO= new CompanyRoleDAO(jpaApi);
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

    @Transactional
    public Collection<QuestMemberDTO> getQuestMembers(final Quests quest, final User doer) {
        final EntityManager em = jpaApi.em();
        final Map<Integer, QuestMemberDTO> members = new LinkedHashMap<>();
        final QuestTeamDAO teamDao = new QuestTeamDAO(em);

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
                Quests childQuest = QuestsDAO.findById((int) childQuestId, em);
                final User childCreator = UserService.getById(childQuest.getCreatedBy(), em);
                mergeMembers(members, buildMembers(em, teamDao, childQuest, childCreator));
            }
        } else {
            // This should perform almost the same for single quest as before
            mergeMembers(members, buildMembers(em, teamDao, quest, doer));
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

    private Collection<QuestTeamDTO> buildTeams(EntityManager em, QuestTeamDAO dao, Quests quest) {
       return dao.listTeamsForQuest(quest, false, true).stream()
                .map(team -> QuestTeamDTO.from(team, getQuestRaisedFunds(team.getQuest().getId(), team.getCreator().getId(), em)))
                .sorted((left, right) -> ObjectUtils.compare(right.amountBacked, left.amountBacked, false))
                .collect(toList()); 
    }

    @Transactional
    public Collection<QuestTeamDTO> getQuestTeams(final @NotNull Quests quest) {
        final EntityManager em = jpaApi.em();
        final QuestTeamDAO dao = new QuestTeamDAO(em);
       
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
                Quests childQuest = QuestsDAO.findById((int) childQuestId, em);

                teams.addAll(buildTeams(em, dao, childQuest));
            }
        } else {
            // This should perform almost the same for single quest as before 
            teams.addAll(buildTeams(em, dao, quest));
        }
        
        return teams;
    }

    @Transactional
    public QuestTeamDTO getQuestTeam(final @NotNull Quests quest, final @NotNull User doer, final boolean checkDefault) {
        final EntityManager em = jpaApi.em();
        final QuestTeamDAO dao = new QuestTeamDAO(em);

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

    @Transactional
    public QuestTeamDTO getTeam(final @NotNull Long teamId) {
        final EntityManager em = jpaApi.em();
        final QuestTeamDAO dao = new QuestTeamDAO(em);
        return Optional.ofNullable(dao.load(teamId, QuestTeam.class))
                .map(team -> QuestTeamDTO.from(team, getQuestRaisedFunds(team.getQuest().getId(), team.getCreator().getId(), em)))
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
