package com.diemlife.dao;

import com.diemlife.models.QuestTeam;
import com.diemlife.models.QuestTeam2;
import com.diemlife.models.QuestTeamMember;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;

public class QuestTeamDAO extends TypedDAO<QuestTeam> {

    public QuestTeamDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public List<QuestTeam> listTeamsForQuest(final Quests quest, final boolean excludeDefault, final boolean excludeIndividual) {
        if (quest == null) {
            return emptyList();
        }
        return entityManager
                .createQuery("" +
                        "SELECT qt FROM QuestTeams qt " +
                        "WHERE qt.quest.id = :questId" +
                        (excludeDefault ? "  AND qt.defaultTeam = FALSE" : "") +
                        (excludeIndividual ? "  AND qt.individualTeam = FALSE" : ""), QuestTeam.class)
                .setParameter("questId", quest.getId())
                .getResultList();
    }

    public QuestTeam getTeamForQuestAndUser(final Quests quest, final User user, final boolean getDefault) {
        if (quest == null) {
            return null;
        }
        return entityManager
                .createQuery("" +
                        "SELECT qt FROM QuestTeams qt " +
                        "WHERE qt.quest.id = :questId" +
                        " AND qt.creator.id = :userId" +
                        (getDefault ? "  AND qt.defaultTeam = TRUE" : " AND qt.defaultTeam = FALSE"), QuestTeam.class)
                .setParameter("questId", quest.getId())
                .setParameter("userId", user.getId())
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public QuestTeam getAllTeamForQuestAndUser(final Quests quest, final User user) {
        if (quest == null) {
            return null;
        }
        return entityManager
                .createQuery("" +
                        "SELECT qt FROM QuestTeams qt " +
                        "WHERE qt.quest.id = :questId" +
                        " AND qt.creator.id = :userId", QuestTeam.class)
                .setParameter("questId", quest.getId())
                .setParameter("userId", user.getId())
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<QuestTeam2> listTeamsForQuest(Connection c, long questId, boolean excludeDefault, boolean excludeIndividual) {
        List<QuestTeam2> result = new LinkedList<>();
        
        try (PreparedStatement ps = c.prepareStatement("select id, team_name, logo_url, creator_user_id, created_on, is_default, is_individual " +
            "from quest_teams where quest_id = ?"))
        {
			ps.setLong(1, questId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
                    int id = rs.getInt(1);
                    String teamName = rs.getString(2);
                    String logoUrl = rs.getString(3);
                    long creatorUserId = rs.getLong(4);
                    java.sql.Timestamp createdOn = rs.getTimestamp(5);
                    boolean isDefault = rs.getBoolean(6);
					boolean isIndividual = rs.getBoolean(7);

                    // drop this result row?
                    if ((isDefault && excludeDefault) || (isIndividual && excludeIndividual)) {
                        continue;
                    }

					result.add(new QuestTeam2(id, teamName, logoUrl, questId, creatorUserId, createdOn.getTime(), isDefault, isIndividual));
				}
			}
		} catch (Exception e) {
			Logger.error("listTeamsForQuest - error", e);
			result.clear();
		}

		return result;
    }

    // Note: intentionally made this static to keep entitymanager out of new code
    public static QuestTeam2 getTeam(Connection c, long teamId) {
        
        try (PreparedStatement ps = c.prepareStatement("select quest_id, team_name, logo_url, creator_user_id, created_on, is_default, is_individual " +
            "from quest_teams where id = ?"))
        {
			ps.setLong(1, teamId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
                    long questId = rs.getLong(1);
                    String teamName = rs.getString(2);
                    String logoUrl = rs.getString(3);
                    long creatorUserId = rs.getLong(4);
                    java.sql.Timestamp createdOn = rs.getTimestamp(5);
                    boolean isDefault = rs.getBoolean(6);
					boolean isIndividual = rs.getBoolean(7);

                    // only expect 1 result (or none)
					return new QuestTeam2((int) teamId, teamName, logoUrl, questId, creatorUserId, createdOn.getTime(), isDefault, isIndividual);
				}
			}
		} catch (Exception e) {
			Logger.error("getTeam - error", e);
		}

		return null;
    }

    public static QuestTeam2 getActiveTeamByQuestAndUser(Connection c, long questId, long userId) {
        try (PreparedStatement ps = c.prepareStatement("select id, team_name, logo_url, creator_user_id, created_on, is_default, is_individual " +
            "from quest_teams as qt join quest_team_members as qtm on qt.id = qtm.team_id where qt.quest_id = ? and qtm.member_id = ? and qtm.active = 1"))
        {
			ps.setLong(1, questId);
			ps.setLong(2, userId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
                    int teamId = rs.getInt(1);
                    String teamName = rs.getString(2);
                    String logoUrl = rs.getString(3);
                    long creatorUserId = rs.getLong(4);
                    java.sql.Timestamp createdOn = rs.getTimestamp(5);
                    boolean isDefault = rs.getBoolean(6);
					boolean isIndividual = rs.getBoolean(7);

                    // only expect 1 result (or none)
					return new QuestTeam2(teamId, teamName, logoUrl, questId, creatorUserId, createdOn.getTime(), isDefault, isIndividual);
				}
			}
		} catch (Exception e) {
			Logger.error("getTeamByQuestAndUser - error", e);
		}

		return null;
    }
    
    public List<QuestTeam> listActiveTeamsForUser(final User user) {
        if (user == null) {
            return emptyList();
        }
        return entityManager
                .createQuery("SELECT qt FROM QuestTeams qt " +
                        "INNER JOIN QuestTeamMembers qtm ON qtm.team.id = qt.id " +
                        "WHERE qtm.member.id = :userId AND qtm.active = TRUE", QuestTeam.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

	public QuestTeam getTeam(long teamId) {
		return entityManager.createQuery("SELECT qt FROM QuestTeams qt where qt.id = :teamId", QuestTeam.class)
				.setParameter("teamId", teamId)
				.getResultList()
				.stream()
				.findFirst()
				.orElse(null);
	}
	
    public QuestTeam getTeam(final Quests quest, final String teamName) {
        if (quest == null || isBlank(teamName)) {
            return null;
        }
        return entityManager.createQuery("SELECT qt FROM QuestTeams qt " +
                "WHERE qt.quest.id = :questId AND LOWER(qt.name) = :teamName", QuestTeam.class)
                .setParameter("questId", quest.getId())
                .setParameter("teamName", lowerCase(teamName))
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public QuestTeam getDefaultTeam(final Quests quest) {
        if (quest == null) {
            return null;
        }
        return entityManager.createQuery("SELECT qt FROM QuestTeams qt " +
                        "WHERE qt.quest.id = :questId AND qt.creator.id = :creatorId AND qt.defaultTeam = TRUE", QuestTeam.class)
                .setParameter("questId", quest.getId())
                .setParameter("creatorId", quest.getOrigCreatedBy())
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);

//         " +
//                        "
    }

    public QuestTeamMember getTeamMember(final Quests quest, final User doer) {
        if (quest == null || doer == null || quest.getId() == null || doer.getId() == null) {
            return null;
        }
        return getTeamMember(quest.getId(), doer.getId(), true);
    }

    public QuestTeamMember getTeamMember(final Integer questId, final Integer doerId) {
        if (questId == null || doerId == null) {
            return null;
        }
        return getTeamMember(questId, doerId, true);
    }

    public QuestTeamMember getTeamMember(final Quests quest, final User doer, final boolean activeOnly) {
        if (quest == null || doer == null || quest.getId() == null || doer.getId() == null) {
            return null;
        }
        return getTeamMember(quest.getId(), doer.getId(), activeOnly);
    }

    public QuestTeamMember getTeamMember(final Integer questId, final Integer doerId, final boolean activeOnly) {
        if (questId == null || doerId == null) {
            return null;
        }
        return entityManager.createQuery("SELECT qtm FROM QuestTeamMembers qtm " +
                "INNER JOIN QuestTeams qt ON qt.id = qtm.team.id " +
                "WHERE qt.quest.id = :questId " +
                "AND qtm.member.id = :doerId " +
                (activeOnly ? "AND qtm.active = TRUE " : "") +
                "ORDER BY qtm.active DESC, qt.defaultTeam ASC, qtm.since DESC", QuestTeamMember.class)
                .setParameter("questId", questId)
                .setParameter("doerId", doerId)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public QuestTeam createTeam(final Quests quest,
                                final User creator,
                                final String name,
                                final String logoUrl,
                                final boolean isDefault,
                                final boolean isIndividual) {
        final QuestTeam team = new QuestTeam();
        team.setName(name);
        team.setLogoUrl(logoUrl);
        team.setQuest(quest);
        team.setCreator(creator);
        team.setDefaultTeam(isDefault);
        team.setIndividualTeam(isIndividual);
        return save(team, QuestTeam.class);
    }

    public QuestTeam updateTeam(final QuestTeam team) {
        if (team == null || team.getId() == null) {
            return null;
        } else {
            return save(team, QuestTeam.class);
        }
    }

	// See if the user is already active for this quest on some team
	public boolean isUserActiveTeamMemberForQuest(Quests quest, User user) {
		final List<QuestTeam> questTeams = listTeamsForQuest(quest, true, false);
        return questTeams.stream()
                .flatMap(questTeam -> questTeam.getMembers().stream())
                .filter(QuestTeamMember::isActive)
                .anyMatch(member -> member.getMember().getId().equals(user.getId()));
	}

	public void leaveAllTeams(final User user, final Quests quest) {
        listTeamsForQuest(quest, false, false).forEach(team -> leaveTeam(team, user));
    }

	public boolean joinTeam(final QuestTeam team, final User joiningUser) {
		return joinTeam(team, joiningUser, false);
	}
	
	// createAsInactive: special case where we want to map a user to a quest team just before a quest is started
    public boolean joinTeam(final QuestTeam team, final User joiningUser, boolean createAsInactive) {
        if (team == null || team.getId() == null || joiningUser == null || joiningUser.getId() == null) {
            return false;
        }
        if (isUserActiveTeamMemberForQuest(team.getQuest(), joiningUser)) {
			Logger.warn(format("User '%s' attempted to join the team '%s' but is already an active member of another team of the same Quest [%s]", joiningUser.getEmail(), team.getName(), team.getQuest().getId()));
            return false;
		}
		
        final QuestTeamMember existingMember = getQuestTeamMember(team, joiningUser);
        if (existingMember == null) {
            team.getMembers().add((createAsInactive ? new QuestTeamMember(team, joiningUser, false) : new QuestTeamMember(team, joiningUser)));
        } else {
            existingMember.setSince(Date.from(Instant.now()));
            existingMember.setActive(true);
        }

        Logger.debug(format("User '%s' joined the team '%s' of the Quest [%s]", joiningUser.getEmail(), team.getName(), team.getQuest().getId()));

        return save(team, QuestTeam.class) != null;
    }

    public void leaveTeam(final QuestTeam team, final User leavingUser) {
        if (team == null || team.getId() == null || leavingUser == null || leavingUser.getId() == null) {
            Logger.error("QuestTeamDAO::leaveTeam - Both team and leavingUser are mandatory");
        } else {
            final QuestTeamMember existingMember = getQuestTeamMember(team, leavingUser);
            if (existingMember == null) {
                Logger.warn(format("User '%s' attempted to leave the team '%s' of Quest [%s] but is not an active member of it", leavingUser.getEmail(), team.getQuest().getId(), team.getName()));
            } else {
                final QuestTeam teamToLeave = existingMember.getTeam();
                if (teamToLeave.isIndividualTeam()) {
                    entityManager.remove(teamToLeave);
                } else if (teamToLeave.isDefaultTeam()) {
                    entityManager.remove(existingMember);
                } else {
                    existingMember.setActive(false);
                    entityManager.merge(existingMember);
                }
            }
        }
    }

    private static QuestTeamMember getQuestTeamMember(final QuestTeam team, final User user) {
        return team.getMembers()
                .stream()
                .filter(member -> member.getMember().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
    }

    // Returns (memberUserId, teamUserId)
    public Map<Long, Long> getTeamsWithMembersForQuest(Connection c, long questId, boolean includeDefaultTeam) {
		Map<Long, Long> result = new HashMap<>();
		
		// Note: this only works for numeric data
		try (PreparedStatement ps = c.prepareStatement("select qtm.member_id, qt.creator_user_id from quest_teams qt join quest_team_members qtm on qt.id = qtm.team_id " + 
            "where qt.quest_id = ? and qtm.active = 1" + (includeDefaultTeam ? "" : " and qt.is_default = 0")))
        {
			ps.setLong(1, questId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long memberUserId = rs.getLong(1);
					long teamUserId = rs.getLong(2);

					result.put(memberUserId, teamUserId);
				}
			}
		} catch (Exception e) {
			Logger.error("getTeamsWithMembersForQuest - error", e);
			result.clear();
		}

		return result;
    }
}
