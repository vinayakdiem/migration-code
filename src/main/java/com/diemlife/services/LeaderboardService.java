package com.diemlife.services;

import com.diemlife.constants.LeaderboardMemberStatus;
import com.diemlife.dao.AttributesDAO;
import com.diemlife.dao.HappeningParticipantDAO;
import com.diemlife.dao.LeaderboardDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.QuestTeamDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dto.LeaderboardScoreDTO;
import com.diemlife.models.Address;
import com.diemlife.models.Happening;
import com.diemlife.models.HappeningParticipant;
import com.diemlife.models.IdentifiedEntity;
import com.diemlife.models.LeaderboardAttribute;
import com.diemlife.models.LeaderboardMember;
import com.diemlife.models.LeaderboardMember2;
import com.diemlife.models.LeaderboardScore;
import com.diemlife.models.PersonalInfo;
import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestLeaderboard;
import com.diemlife.models.Quests;
import com.diemlife.models.QuestTeam2;
import com.diemlife.models.User;
import play.Logger;
import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.upperCase;

@Singleton
public class LeaderboardService {

    private final JPAApi jpaApi;
    private final Database db;
    private final Database dbRo;

    @Inject
    public LeaderboardService(final JPAApi jpaApi, Database db, @NamedDatabase("ro") Database dbRo) {
        this.jpaApi = jpaApi;
        this.db = db;
        this.dbRo = dbRo;
    }

    /*
    @Transactional(readOnly = true)
    public List<LeaderboardScoreDTO> getLeaderboard(final Quests quest, final String attributeSlug, final boolean withZeros, final boolean withHidden) {
        return getLeaderboardLocal(quest, attributeSlug, withZeros, withHidden, false);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardScoreDTO> getLeaderboardSync(final Quests quest, final String attributeSlug, final boolean withZeros, final boolean withHidden) {
        return getLeaderboardLocal(quest, attributeSlug, withZeros, withHidden, true);
    }
    */

    @Transactional(readOnly = true)
    public List<LeaderboardScoreDTO> getLeaderboardLocal(final Quests quest, final String attributeSlug, final boolean withZeros, final boolean withHidden, final boolean sync,
        final boolean withHiddenUsers)
    {
        if (quest == null || !quest.isLeaderboardEnabled()) {
            return emptyList();
        }
        final EntityManager em = jpaApi.em();
        final LeaderboardDAO leaderboardDao = new LeaderboardDAO(em);

        long questId = quest.getId();

        LeaderboardAttribute _attribute = null;
        Set<String> _hiddenSlugs = null;
        try (Connection c = sync ? db.getConnection() : dbRo.getConnection()) {
            if ((_attribute = leaderboardDao.getLeaderboardAttribute(c, attributeSlug)) != null) {
               _hiddenSlugs = leaderboardDao.getHiddenLeaderboardSlugsForQuest(c, questId);
            }
        } catch (SQLException e) {
            Logger.error("getLeaderboard - unbale to fetch leaderboard for quest: " + questId, e);
        }
        if (_attribute == null) {
          return emptyList(); 
        }

        final LeaderboardAttribute attribute = _attribute;
        final Set<String> hiddenSlugs = ((_hiddenSlugs == null) ? new HashSet<String>() : _hiddenSlugs);

        List<LeaderboardScore> scores;
        try (Connection c = sync ? db.getConnection() : dbRo.getConnection()) {
            QuestLeaderboard ql = leaderboardDao.getQuestLeaderboard(c, questId, attributeSlug);
            if (ql != null) {
                if (ql.isAggregateTag()) {
                    AttributesDAO attrDao = AttributesDAO.getInstance();
                    Map<Long, Double> agg = attrDao.aggregateValuesByUser(c, ql.getAttributeId(), (ql.getConversion() != null));

                    if (agg.isEmpty()) {
                        Logger.warn("getLeaderboard - No attribute values found for quest " + questId + " and attribute " + attributeSlug);
                    }
			
                    if (ql.isAggregateIndividualTag()) {
                        // note: solution doesn't take memberstatus into account
                        TreeMap<Double, List<LeaderboardScore>> orderedScores = new TreeMap<Double, List<LeaderboardScore>>();
			
                        for (Long key : agg.keySet()) {
                            List<LeaderboardScore> userScores = leaderboardDao.getScoresByQuestAndUser(c, questId, key, withHiddenUsers);
                            if (!userScores.isEmpty()) {
                                // Found an existing score entry from which to steal attributes to make our fake entry
                                LeaderboardScore ls = userScores.get(0);
                                Double _score = agg.get(key);
                                Double conversion = ql.getConversion();
                                Double score = ((conversion == null) ? _score : (conversion * _score));
				
                                List<LeaderboardScore> list = orderedScores.get(score);
                                if (list == null) {
                                    list = new LinkedList<LeaderboardScore>();
                                    orderedScores.put(score, list);
                                }
                                list.add(new LeaderboardScore(ls.getMemberId(), ls.getAttributeId(), score, ls.getMemberStatus(), ls.getHasStarted(), ls.getHasFinished(),
                                    ls.getOutOfRace()));
                            }
                        }
			
                        // withZeros=true grab all of the users for leaderboard_member for the quest
                        List<LeaderboardMember2> allMembers = (withZeros ? leaderboardDao.getAllMembers2(c, questId) : new LinkedList<LeaderboardMember2>());

                        List<LeaderboardScore> appendToEnd = new LinkedList<LeaderboardScore>();
                        if (!allMembers.isEmpty()) {
                            for (LeaderboardMember2 member : allMembers) {
                                if (withHiddenUsers || !member.isHidden()) {
                                    // Add a fabricated row for each user not returned by aggregate set
                                    Long userId = member.getUserId();

                                    // Note: the attribute_values table only contains results from platform users but we can fabricate rows for guests
                                    if ((userId == null) || !agg.containsKey(userId)) {
                                        // add to the zeros list
                                        appendToEnd.add(new LeaderboardScore(member.getId(), attributeSlug, 0.0, LeaderboardMemberStatus.NoInfo.ordinal(), false, false, false));
                                    }
                                }
                            }
                        }
			
                        // finish sorting results
                        scores = new LinkedList<LeaderboardScore>();
                        Collection<List<LeaderboardScore>> results = (attribute.isAsc() ? orderedScores.values() : orderedScores.descendingMap().values());
                        for (List<LeaderboardScore> result : results) {
                            scores.addAll(result);
                        }
                        scores.addAll(appendToEnd);
			
                    } else if (ql.isAggregateTeamTag() || ql.isAggregateTeamAverageTag()) {
                        QuestTeamDAO qtDao = new QuestTeamDAO(em);
			
			            // grab team members without default team
                        Map<Long, Long> memberToTeam = qtDao.getTeamsWithMembersForQuest(c, questId, false);

                        // grab all hidden leaderboard members for this quest
                        Set<Long> hiddenMembers = leaderboardDao.getHiddenLeaderboadMembersForQuest(c, questId);

                        // exclude the hidden members from the results
                        for (Long key: hiddenMembers) {
                            agg.remove(key);
                        }

                        // Track number of members for each team
                        Map<Long, Integer> teamMemberCount = new HashMap<Long, Integer>();

                        // Lookup a user to see if they're on a team.  If so, add the user's total to the team's total.
                        Map<Long, Double> teamScores = new HashMap<Long, Double>();
                        for (Long key : agg.keySet()) {
			                // Uses the team creator's platform id
                            Long teamUserId = memberToTeam.get(key);
                            if (teamUserId != null) {
                                // User is on a team
                                Double _score = agg.get(key);
                                Double conversion = ql.getConversion();
                                Double score = ((conversion == null) ? _score : (conversion * _score));
                                
                                // Add score to any running total for team
                                Double currScore = teamScores.get(teamUserId);
                                score = ((currScore == null) ? score : (score + currScore));
                                teamScores.put(teamUserId, score);

                                // Update team member count
                                Integer currCount = teamMemberCount.get(teamUserId);
                                teamMemberCount.put(teamUserId, ((currCount == null) ? 1 : (currCount + 1)));
                            }
                        }
			
                        if (teamMemberCount.isEmpty()) {
                            Logger.warn("getLeaderboard - didn't match any scores to a team for a team aggregate view.");
                        }

                        // note: solution doesn't take memberstatus into account but perhaps that is irrelevant for teams
                        TreeMap<Double, List<LeaderboardScore>> orderedScores = new TreeMap<Double, List<LeaderboardScore>>();

                        // fabricate score entries for each team
                        for (Long key : teamScores.keySet()) {
                            // find the leaderboard member then makeup the rest
                            // Note: think we always want to allow hidden users here so that the user that created the team can be fetched for this team workaround
                            Integer memberId = leaderboardDao.getLeaderboardMemberIdByUserId(c, questId, key, true);
                            if (memberId != null) {
                                Double score = teamScores.get(key);
                                if (ql.isAggregateTeamAverageTag()) {
                                    // switch to average rather than total
                                    score = (score / teamMemberCount.get(key));
                                }

                                List<LeaderboardScore> list = orderedScores.get(score);
                                if (list == null) {
                                    // this is a list in case more than one team has the same score
                                    list = new LinkedList<LeaderboardScore>();
                                    orderedScores.put(score, list);
                                }
                                Logger.debug("getLeaderboard - team with user id " + key + " and memberId " + memberId + " has " + (ql.isAggregateTeamAverageTag() ? "averaged " : "totaled ") +
                                    score + " for quest " + questId);
                                list.add(new LeaderboardScore(memberId, attributeSlug, score, LeaderboardMemberStatus.Started.ordinal(), false, false, false));
                            } else {
				                Logger.warn("getLeaderboard - unable to match team user id " + key + " to a leaderboard member.");
			                }
                        }

                        // withZeros=true grab all the teams for the quest to fabricate an aggregate or average of zero score for any team that doesn't have
                        // members or doesn't have any submitted results
                        List<QuestTeam2> allTeams = (withZeros ? qtDao.listTeamsForQuest(c, questId, true, false) : new LinkedList<QuestTeam2>());
			
                        List<LeaderboardScore> appendToEnd = new LinkedList<LeaderboardScore>();
                        if (!allTeams.isEmpty()) {
                            for (QuestTeam2 team : allTeams) {
                                // Add a fabricated row for each user not returned by aggregate set
                                long userId = team.getCreatorUserId();

                                // Note: the attribute_values table only contains results from platform users but we can fabricate rows for guests
                                if (!teamMemberCount.containsKey(userId)) {
                                    // find the leaderboard member then makeup the rest
                                    Integer memberId = leaderboardDao.getLeaderboardMemberIdByUserId(c, questId, userId, withHiddenUsers);

                                    // add to the zeros list
                                    if (memberId != null) {
                                        appendToEnd.add(new LeaderboardScore(memberId, attributeSlug, 0.0, LeaderboardMemberStatus.NoInfo.ordinal(), false, false, false));
                                    }
                                }
                            }
                        }
			
                        // team
                        scores = new LinkedList<LeaderboardScore>();
                        Collection<List<LeaderboardScore>> results = (attribute.isAsc() ? orderedScores.values() : orderedScores.descendingMap().values());
                        for (List<LeaderboardScore> result : results) {
                            scores.addAll(result);
                        }
                        scores.addAll(appendToEnd);
			
                    } else {
                        // satisy compile complaint
                        scores = new LinkedList<LeaderboardScore>();
		            }
                } else {
                    // withZeros has a dual purpose here.  Intuitively, if we want results with a score of 0 (i.e. nothing), then it is fine
                    // to fabricate implicit records, which will have a score of 0.
                    scores = leaderboardDao.getScoresByQuest(c, quest.getId(), attribute.getId(), attribute.isAsc(), withZeros, withHiddenUsers).stream()
                        .filter(score -> withZeros || score.getScore() > 0)
                        .filter(score -> withHidden || !hiddenSlugs.contains(score.getAttributeId()))
                        .collect(toList());
                }
            } else {
		        // TODO: is this a case worth warning about?  Not sure if there is anything that would ask for a quest leaderboard if it had none.
		
                scores = new LinkedList<LeaderboardScore>();
            }
        } catch (SQLException e) {
            Logger.error("getLeaderboard - unbale to fetch scores for quest: " + questId, e);
            scores = new LinkedList<LeaderboardScore>();
        }
         
        final AtomicInteger place = new AtomicInteger(0);
        return scores.stream().map(score -> {
            final LeaderboardScoreDTO dto = toDto(score, attribute, leaderboardDao.getMember(score.getMemberId()));
            dto.setPlace(place.incrementAndGet());
            return dto;
        }).collect(toList());
    }

    public static LeaderboardScoreDTO toDto(final LeaderboardScore score, LeaderboardAttribute attribute, LeaderboardMember member) {
        final LeaderboardScoreDTO dto = new LeaderboardScoreDTO();
        dto.setMemberId(member.getId());
        dto.setScore(score.getScore());
        dto.setUnit(attribute.getUnit());
        dto.setStatus(LeaderboardMemberStatus.fromInt(score.getMemberStatus()).name());
        final User platformUser = member.getPlatformUser();
        if (platformUser != null) {
            dto.setUserId(platformUser.getId());
            dto.setFirstName(platformUser.getFirstName());
            dto.setLastName(platformUser.getLastName());
            dto.setAvatarUrl(platformUser.getProfilePictureURL());
            dto.setCountry(upperCase(platformUser.getCountry()));
	        dto.setUserName(platformUser.getUserName());
        }
        final PersonalInfo personalInfo = member.getPersonalInfo();
        if (personalInfo != null) {
            dto.setFirstName(personalInfo.firstName);
            dto.setLastName(personalInfo.lastName);
            dto.setGender(Optional.ofNullable(personalInfo.gender)
                    .map(Object::toString)
                    .orElse(null));
            dto.setAge(Optional.ofNullable(personalInfo.birthDate)
                    .map(date -> LocalDate.from(date.toInstant().atZone(ZoneOffset.UTC)))
                    .map(date -> Period.between(date, LocalDate.now()).getYears())
                    .orElse(null));
        }
        final Address address = member.getAddress();
        if (address != null) {
            dto.setCity(address.city);
            dto.setState(upperCase(address.state));
            dto.setCountry(upperCase(address.country));
        }
        return dto;
    }

    @Transactional
    public List<LeaderboardMember> updateLeaderboardMembers(final Happening event) {
        final EntityManager em = jpaApi.em();
        final HappeningParticipantDAO participantDao = new HappeningParticipantDAO(em);
        final LeaderboardDAO leaderboardDao = new LeaderboardDAO(em);

        // Note: this will grab hidden members but I think that's ok since this is meant to be an admin feature
        final List<LeaderboardMember> members = leaderboardDao.getAllMembers(event.quest);
        final List<HappeningParticipant> newParticipants = participantDao.findByHappeningId(event.getId()).stream()
                .filter(participant -> participant.person != null)
                .filter(participant -> members.stream()
                        .map(LeaderboardMember::getPersonalInfo)
                        .filter(Objects::nonNull)
                        .map(IdentifiedEntity::getId)
                        .noneMatch(id -> id.equals(participant.person.id)))
                .collect(toList());
        if (newParticipants.isEmpty()) {
            Logger.info("No new participants to add to the leaderboard for Quest with ID " + event.quest.getId());
            return emptyList();
        }

        final List<LeaderboardMember> result = new ArrayList<>();
        for (final HappeningParticipant participant : newParticipants) {
            final LeaderboardMember member = initializeLeaderboardMember(participant);
            result.add(member);
        }

        return result;
    }

    @Transactional
    public LeaderboardMember initializeLeaderboardMember(final HappeningParticipant participant) {
        final EntityManager em = jpaApi.em();
        final LeaderboardDAO leaderboardDao = new LeaderboardDAO(em);
        final Quests quest = QuestsDAO.findById(participant.event.quest.getId(), em);
        final LeaderboardMember member = leaderboardDao.createLeaderboardMember(
                quest,
                UserHome.findByEmail(participant.person.email, em),
                participant.person,
                participant.address
        );

        long questId = quest.getId();
        try (Connection c = db.getConnection()) {
            // Don't populate quest with this detail as it is local to this method

            // Grab non-aggregate attrs
            List<LeaderboardAttribute> attributes = leaderboardDao.getNonAggregateLeaderboardAttributeByQuest(c, questId);

            if (attributes != null) {
                for (LeaderboardAttribute attribute : attributes) {
                    if (shouldAddScore(participant, leaderboardDao.getStripeSkuForEvent(c, attribute, participant.event))) {
                        final LeaderboardScore score = leaderboardDao.getOrCreateScore(c, member.getId(), attribute.getId());
                        if (score == null) {
                            Logger.warn("initializeLeaderboardMember - failed.  We can probably recover from this.");
                        } else {
                            Logger.info(format("Leaderboard score for attribute '%s' and member ID %s is %s (%s)", attribute.getId(), member.getId(), score.getScore(),
                                LeaderboardMemberStatus.fromInt(score.getMemberStatus()).name()));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error("initializeLeaderboardMember - unbale to fetch leaderboard for quest: " + questId, e);
        }

        return member;
    }

    @Transactional
    public LeaderboardMember initializeLeaderboardMemberIfNotPresent(final QuestActivity doer) {
        final EntityManager em = jpaApi.em();
        return initializeLeaderboardMemberIfNotPresent(QuestsDAO.findById(doer.getQuestId(), em), UserHome.findById(doer.getUserId(), em));
    }

    @Transactional
    public LeaderboardMember initializeLeaderboardMemberIfNotPresent(Quests quest, User user) {
        final EntityManager em = jpaApi.em();
        final LeaderboardDAO leaderboardDao = new LeaderboardDAO(em);
        final LeaderboardMember member = leaderboardDao.createLeaderboardMember(quest, user, null, null);

        long questId = quest.getId();
        try (Connection c = db.getConnection()) {
            // Populate quest with this data as the quest could have come from elsewhere

            // Do not get aggregate attrs
            List<LeaderboardAttribute> attributes = leaderboardDao.getNonAggregateLeaderboardAttributeByQuest(c, questId);

            quest.setLeaderboardAttributes(attributes);

            for (LeaderboardAttribute attribute : attributes) {
                String attributeId = attribute.getId();
                final LeaderboardScore score = leaderboardDao.getOrCreateScore(c, member.getId(), attributeId);
                if (score == null) {
                    Logger.warn("initializeLeaderboardMemberIfNotPresent - failed.  We can probably recover from this.");
                } else {
                    Logger.info(format("Leaderboard score for attribute '%s' and member with User ID %s is %s (%s)", attributeId, user.getId(), score.getScore(),
                        LeaderboardMemberStatus.fromInt(score.getMemberStatus()).name()));
                }
            }
        } catch (SQLException e) {
            Logger.error("initializeLeaderboardMemberIfNotPresent - unbale to fetch leaderboard for quest: " + questId, e);
        }

        return member;
    }

    private boolean shouldAddScore(final HappeningParticipant participant, String stripeSku) {
        if (isBlank(participant.stripeSkuId)) {
            return true;
        }
        if (stripeSku == null) {
            return false;
        } else {
            return stripeSku.equals(participant.stripeSkuId);
        }
    }

}
