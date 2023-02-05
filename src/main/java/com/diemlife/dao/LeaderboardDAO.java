package com.diemlife.dao;

import constants.LeaderboardMemberStatus;
import models.Address;
import models.Happening;
import models.LeaderboardAttribute;
import models.LeaderboardMember;
import models.LeaderboardMember2;
import models.LeaderboardScore;
import models.PersonalInfo;
import models.Quests;
import models.QuestLeaderboard;
import models.User;
import play.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.persistence.EntityManager;

import com.diemlife.constants.Util;

import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;

public class LeaderboardDAO {

    private final EntityManager entityManager;

    public LeaderboardDAO(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public LeaderboardAttribute getLeaderboardAttribute(Connection c, String slug) {
        LeaderboardAttribute result = null;
        
        try (PreparedStatement ps = c.prepareStatement("select name, ascending_scoring, quest_default, unit, creator_user_id, created_on from leaderboard_attribute where id_slug = ?")) {
            ps.setString(1, slug);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString(1);
                    boolean ascendingScoring = rs.getBoolean(2);
                    boolean questDefault = rs.getBoolean(3);
                    String unit = rs.getString(4);
                    long creator = rs.getLong(5);
                    Date createdOn = rs.getDate(6);
                    result = new LeaderboardAttribute(slug, name, ascendingScoring, questDefault, unit, creator, createdOn);
                }
            }
        } catch (Exception e) {
            Logger.error("getLeaderboardAttribute - error", e);
            result = null;
        }

        return result;
    }

    // Fetch all attributes
    public List<LeaderboardAttribute> getLeaderboardAttributeByQuest(Connection c, long questId) {
        return _getLeaderboardAttributeByQuest(c, questId, true, true);
    }

    // Fetch only non-aggregate attributes
    public List<LeaderboardAttribute> getNonAggregateLeaderboardAttributeByQuest(Connection c, long questId) {
        return _getLeaderboardAttributeByQuest(c, questId, false, true);
    }

    // Fetch only aggregate attributes
    public List<LeaderboardAttribute> getAggregateLeaderboardAttributeByQuest(Connection c, long questId) {
        return _getLeaderboardAttributeByQuest(c, questId, true, false);
    }

    private List<LeaderboardAttribute> _getLeaderboardAttributeByQuest(Connection c, long questId, boolean includeAggregates, boolean includeNonAggregates) {
        List<LeaderboardAttribute> result = new LinkedList<LeaderboardAttribute>();

        String whereCondition;
        if (includeAggregates && includeNonAggregates) {
            whereCondition = " and la.id_slug in (" +
                    "select distinct ql.leaderboard_attribute_id_slug " +
                    "FROM quest_leaderboard ql " +
                    "where ql.quest_id = ? " +
                    "and (ql.leaderboard_attribute_id_slug in (select distinct ls.leaderboard_attribute_id_slug from leaderboard_member lm join leaderboard_score ls on lm.id = ls.leaderboard_member_id where lm.quest_id = ? and ls.score > 0) " +
                    "or ql.attributes_tag in ('AGGREGATE_INDIVIDUAL', 'AGGREGATE_TEAM', 'AGGREGATE_TEAM_AVERAGE')))";
        } else if (includeNonAggregates) {
            // non-aggregates only
            whereCondition = " and ql.attributes_tag not in ('AGGREGATE_INDIVIDUAL', 'AGGREGATE_TEAM')";
        } else {
            // aggregates only
            whereCondition = " and ql.attributes_tag in ('AGGREGATE_INDIVIDUAL', 'AGGREGATE_TEAM')";
        }

        try (PreparedStatement ps = c.prepareStatement("select la.id_slug, la.name, la.ascending_scoring, la.quest_default, la.unit, la.creator_user_id, la.created_on from " +
            "leaderboard_attribute la join quest_leaderboard ql on la.id_slug = ql.leaderboard_attribute_id_slug where ql.quest_id = ?" + whereCondition))
        {
            ps.setLong(1, questId);
            if (includeAggregates && includeNonAggregates) {
                ps.setLong(2, questId);
                ps.setLong(3, questId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String slug = rs.getString(1);
                    String name = rs.getString(2);
                    boolean ascendingScoring = rs.getBoolean(3);
                    boolean questDefault = rs.getBoolean(4);
                    String unit = rs.getString(5);
                    long creator = rs.getLong(6);
                    Date createdOn = rs.getDate(7);
                    result.add( new LeaderboardAttribute(slug, name, ascendingScoring, questDefault, unit, creator, createdOn) );
                }
            }
        } catch (Exception e) {
            Logger.error("getLeaderboardAttributeByQuest - error", e);
            result.clear();
        }

        return result;
    }

    // Note: the schema allows for a search by attribute and event to pull up more than 1 sku because pri key is (attr, sku, event).
    // FIXME: the original code was just returning the first result so either the schema is wrong or only taking the first result is wrong.
    public String getStripeSkuForEvent(Connection c, LeaderboardAttribute attribute, Happening event) {
        String result = null;
        
        try (PreparedStatement ps = c.prepareStatement("select stripe_sku_id from event_leaderboard_config where leaderboard_attribute_id_slug = ? and event_id = ?")) {
            ps.setString(1, attribute.getId());

            // FIXME: this is an int in the DB but should be a long
            ps.setInt(2, (int) event.getId().longValue());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = rs.getString(1);
                }
            }
        } catch (Exception e) {
            Logger.error("getStripeSkuForEvent - error", e);
            result = null;
        }

        return result;
    }

    public Integer getLeaderboardMemberIdByUserId(Connection c, long questId, long userId, boolean includeHiddenMembers) {
        Integer result = null;
        
        try (PreparedStatement ps = c.prepareStatement("select id from leaderboard_member where quest_id = ? and platform_user_id = ?" + 
            (includeHiddenMembers ? "" : " and hidden = 0")))
        {
            ps.setLong(1, questId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            Logger.error("getLeaderboardMemberIdByUserId - error", e);
            result = null;
        }

        return result;
    }

    public Set<Long> getHiddenLeaderboadMembersForQuest(Connection c, long questId) {
        Set<Long> result = new HashSet<Long>();

        try (PreparedStatement ps = c.prepareStatement("select platform_user_id from leaderboard_member where quest_id = ? and hidden = 1")) {
            ps.setLong(1, questId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.add(rs.getLong(1));
                }
            }
        } catch (Exception e) {
            Logger.error("getHiddenLeaderboardMembersForQuest - error", e);
            result = null;
        }

        return result;
    }

    public LeaderboardMember getMember(final Long memberId) {
        return entityManager.find(LeaderboardMember.class, memberId);
    }

    // TODO: Remove this later in favor of the reader method.  Didn't have time to unwind this Hibernate bit as other code
    // relies on it to add members.
    // Note: this will return hidden members but I think that's fine given the one thing that calls it
    public List<LeaderboardMember> getAllMembers(final Quests quest) {
        if (quest == null) {
            return emptyList();
        }
        return entityManager.createQuery("" +
                "SELECT lm FROM LeaderboardMembers lm " +
                "WHERE lm.quest.id = :questId", LeaderboardMember.class)
                .setParameter("questId", quest.getId())
                .getResultList();
    }

    // Use reader to fetch LeaderboardMembers, including hidden members
    public List<LeaderboardMember2> getAllMembers2(Connection c, long questId) {
        List<LeaderboardMember2> result = new LinkedList<LeaderboardMember2>();
        
        try (PreparedStatement ps = c.prepareStatement("select id, platform_user_id, personal_info_id, address_id, hidden from leaderboard_member where quest_id = ?"))
        {
            ps.setLong(1, questId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    
                    Long platformUserId = rs.getLong(2);
                    if (rs.wasNull()) {
                        platformUserId = null;
                    }
                    
                    Integer personalInfoId = rs.getInt(3);
                    if (rs.wasNull()) {
                        personalInfoId = null;
                    }

                    Integer addressId = rs.getInt(4);
                    if (rs.wasNull()) {
                        addressId = null;
                    }

                    boolean hidden = rs.getBoolean(5);

                    result.add(new LeaderboardMember2(id, questId, platformUserId, personalInfoId, addressId, hidden));
                }
            }
        } catch (Exception e) {
            Logger.error("getAllMembers2 - error", e);
            result.clear();
        }

        return result;
    }
    
    public LeaderboardMember createLeaderboardMember(final Quests quest,
                                                     final User platformUser,
                                                     final PersonalInfo personalInfo,
                                                     final Address address) {
        if (quest == null) {
            throw new IllegalArgumentException("Quest must be specified in order to create a leaderboard member");
        }
        if (platformUser == null && personalInfo == null) {
            throw new IllegalArgumentException("One of platform user or personal info is required for leaderboard member");
        }
        final List<LeaderboardMember> existingWithUser = platformUser == null ? emptyList() : entityManager.createQuery("" +
                "SELECT lm FROM LeaderboardMembers lm " +
                "WHERE lm.quest.id = :questId " +
                "AND lm.platformUser.id = :userId", LeaderboardMember.class)
                .setParameter("questId", quest.getId())
                .setParameter("userId", platformUser.getId())
                .getResultList();
        final List<LeaderboardMember> existingWithPersonal = personalInfo == null ? emptyList() : entityManager.createQuery("" +
                "SELECT lm FROM LeaderboardMembers lm " +
                "WHERE lm.quest.id = :questId " +
                "AND lm.personalInfo.id = :personalId", LeaderboardMember.class)
                .setParameter("questId", quest.getId())
                .setParameter("personalId", personalInfo.getId())
                .getResultList();
        if (!Util.isEmpty(existingWithUser) || !Util.isEmpty(existingWithPersonal)) {
            final LeaderboardMember existing = !Util.isEmpty(existingWithUser)
                    ? existingWithUser.iterator().next()
                    : existingWithPersonal.iterator().next();
            existing.setQuest(quest);
            existing.setPlatformUser(platformUser == null ? existing.getPlatformUser() : platformUser);
            existing.setPersonalInfo(personalInfo == null ? existing.getPersonalInfo() : personalInfo);
            existing.setAddress(address == null ? existing.getAddress() : address);

            Logger.info("Updated existing leaderboard member with ID " + existing.getId());

            return entityManager.merge(existing);
        } else {
            final LeaderboardMember member = new LeaderboardMember();
            member.setQuest(quest);
            member.setPlatformUser(platformUser);
            member.setPersonalInfo(personalInfo);
            member.setAddress(address);
            entityManager.persist(member);

            Logger.info("Created new leaderboard member with ID " + member.getId());

            return entityManager.find(LeaderboardMember.class, member.getId());
        }
    }

    public LeaderboardScore setLeaderboardScore(Connection c, final LeaderboardAttribute attribute,
                                                final LeaderboardMember member,
                                                final Integer scoreValue,
                                                final LeaderboardMemberStatus status) {
        if (member == null || attribute == null) {
            return null;
        }
        return Optional.of(getOrCreateScore(c, member.getId(), attribute.getId()))
                .map(score -> {
                    score.setScore(scoreValue == null ? score.getScore() : scoreValue);
                    score.setMemberStatus(status == null ? score.getMemberStatus() : status.ordinal());
                    return score;
                })
                .get();
    }

    public List<LeaderboardScore> getScoresByQuest(Connection c, long questId, String attributeSlug, boolean ascScore, boolean includeImplicitScores, boolean includeHiddenMembers) {
        List<LeaderboardScore> result = new LinkedList<LeaderboardScore>();

        String query;
        if (includeImplicitScores) {
            // Fetches the score rows for the quest and creates an implicit 0 score for anyone in the member table who hasn't submitted a value yet
            query = "select lm.id as leaderboard_member_id, ifnull(ls.score, 0) as score, ifnull(ls.member_status, 0) as member_status, " +
                    "ifnull(ls.has_started, 0) as has_started, ifnull(ls.has_finished, 0) as has_finished, ifnull(ls.out_of_race, 0) as out_of_race from " +
                    "leaderboard_member lm left join (select leaderboard_member_id, score, member_status, has_started, has_finished, out_of_race from " +
                    "leaderboard_score where leaderboard_attribute_id_slug = ?) ls on lm.id = ls.leaderboard_member_id where lm.quest_id = ? " + 
                    (includeHiddenMembers ? "" : " and lm.hidden = 0 ") + "order by ls.member_status desc, ls.score " + (ascScore ? "asc" : "desc");
        } else {
            // Does not fabricate an implicit row for a leaderboard member without a score record
            query = "select ls.leaderboard_member_id, ls.score, ls.member_status, ls.has_started, ls.has_finished, ls.out_of_race from leaderboard_score ls " +
                    "join leaderboard_member lm on ls.leaderboard_member_id = lm.id where ls.leaderboard_attribute_id_slug = ? and lm.quest_id = ? " +
                    (includeHiddenMembers ? "" : " and lm.hidden = 0 ") + "order by ls.member_status desc, ls.score " + (ascScore ? "asc" : "desc");
        }
        
        try (PreparedStatement ps = c.prepareStatement(query)) {
            // Note: param ordering is same regardless of query chosen
            ps.setString(1, attributeSlug);
            ps.setLong(2, questId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int memberId = rs.getInt(1);
                    double score = rs.getDouble(2);
                    int status = rs.getInt(3);
                    boolean hasStarted = rs.getBoolean(4);
                    boolean hasFinished = rs.getBoolean(5);
                    boolean outOfRace = rs.getBoolean(6);

                    result.add(new LeaderboardScore(memberId, attributeSlug, score, status, hasStarted, hasFinished, outOfRace));
                }
            }
        } catch (Exception e) {
            Logger.error("getScoresByQuest - error", e);
            result.clear();
        }

        return result;
    }

    public LeaderboardScore getOrCreateScore(Connection c, long memberId, String attributeSlug) {
        LeaderboardScore ls = getScore(c, memberId, attributeSlug);
        if (ls == null) {
            ls = createScore(c, memberId, attributeSlug);
        }
        return ls;
    }

    public LeaderboardScore getScore(Connection c, long memberId, String attributeSlug) {
        LeaderboardScore result = null;

        try (PreparedStatement ps = c.prepareStatement("select score, member_status, has_started, has_finished, out_of_race from " +
            "leaderboard_score where leaderboard_member_id = ? and leaderboard_attribute_id_slug = ?"))
        {
            ps.setLong(1, memberId);
            ps.setString(2, attributeSlug);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double score = rs.getDouble(1);
                    int status = rs.getInt(2);
                    boolean hasStarted = rs.getBoolean(3);
                    boolean hasFinished = rs.getBoolean(4);
                    boolean outOfRace = rs.getBoolean(5);

                    result = new LeaderboardScore(memberId, attributeSlug, score, status, hasStarted, hasFinished, outOfRace);
                }
            }
        } catch (Exception e) {
            Logger.error("getScore - error", e);
            result = null;
        }

        return result;
    }

    public LeaderboardScore createScore(Connection c, long memberId, String attributeSlug) {
        LeaderboardScore result = null;
        try (PreparedStatement ps = c.prepareStatement("insert into leaderboard_score (leaderboard_member_id, leaderboard_attribute_id_slug) values (?, ?)")) {
            ps.setLong(1, memberId);
            ps.setString(2, attributeSlug);

            ps.executeUpdate();

            result = new LeaderboardScore(memberId, attributeSlug, 0.0, LeaderboardMemberStatus.Started.ordinal(), false, false, false);
        } catch (Exception e) {
            Logger.error("createScore - error", e);
            result = null;
        }

        return result;
    }

    public List<LeaderboardScore> getScoresByQuestAndUser(Connection c, long questId, long userId, boolean includeHiddenMembers) {
        List<LeaderboardScore> result = new LinkedList<LeaderboardScore>();

        try (PreparedStatement ps = c.prepareStatement("select leaderboard_member_id, leaderboard_attribute_id_slug, score, member_status, has_started, has_finished, out_of_race " +
            "from leaderboard_score ls join leaderboard_member lm on ls.leaderboard_member_id = lm.id where lm.quest_id = ? and lm.platform_user_id = ? " +
                    (includeHiddenMembers ? "" : " and lm.hidden = 0")))
        {
            ps.setLong(1, questId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int memberId = rs.getInt(1);
                    String attributeSlug = rs.getString(2);
                    double score = rs.getDouble(3);
                    int status = rs.getInt(4);
                    boolean hasStarted = rs.getBoolean(5);
                    boolean hasFinished = rs.getBoolean(6);
                    boolean outOfRace = rs.getBoolean(7);

                    result.add(new LeaderboardScore(memberId, attributeSlug, score, status, hasStarted, hasFinished, outOfRace));
                }
            }
        } catch (Exception e) {
            Logger.error("getScoresByQuestAndUser - error", e);
            result.clear();
        }

        return result;
    }    

    public static Set<String> getHiddenLeaderboardSlugsForQuest(Connection c, long questId) {
        Set<String> result = new HashSet<String>();
        
        try (PreparedStatement ps = c.prepareStatement("select leaderboard_attribute_id_slug from quest_leaderboard where quest_id = ? and hidden > 0")) {
            ps.setLong(1, questId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            Logger.error("getHiddenLeaderboardSlugsForQuest - error", e);
            result.clear();
        }

        return result;
    }

    public static QuestLeaderboard getQuestLeaderboard(Connection c, long questId, String leaderboardSlug) {
        QuestLeaderboard result = null;

        try (PreparedStatement ps = c.prepareStatement("select attributes_id, attributes_tag, conversion, hidden, ordinal from quest_leaderboard where " +
            "quest_id = ? and leaderboard_attribute_id_slug = ?"))
        {
            ps.setLong(1, questId);
            ps.setString(2, leaderboardSlug);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long attributesId = rs.getLong(1);
                    String attributesTag = rs.getString(2);
                    Double conversion = rs.getDouble(3);
                    if (rs.wasNull()) {
                        conversion = null;
                    }
                    int hidden = rs.getInt(4);
                    Integer ordinal = rs.getInt(5);
                    if (rs.wasNull()) {
                        ordinal = null;
                    }
                    result = new QuestLeaderboard(questId, leaderboardSlug, attributesId, attributesTag, conversion, (hidden != 0), ordinal);
                }
            }
        } catch (Exception e) {
            Logger.error("getQuestLeaderboard - error", e);
            result = null;
        }

        return result;
    }

    public static Map<String, Integer> getLeaderboardSlugOrderForQuest(Connection c, long questId) {
        Map<String, Integer> result = new HashMap<String, Integer>();

        try (PreparedStatement ps = c.prepareStatement("select leaderboard_attribute_id_slug, ordinal from quest_leaderboard where quest_id = ?")) {
            ps.setLong(1, questId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String slug = rs.getString(1);
                    Integer ordinal = rs.getInt(2);
                    if (!rs.wasNull()) {
                        result.put(slug, ordinal);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("getLeaderboardSlugOrderForQuest - error", e);
            result.clear();
        }

        return result;
    }

    public static List<QuestLeaderboard> getLeaderboardSlugForRealtimeAttribute(Connection c, long questId, long attributesId, String attributesTag) {
        List<QuestLeaderboard> result = new LinkedList<QuestLeaderboard>();

        // TODO: could move the non-indexed attribute filtering logic here, instead of the DB.
        try (PreparedStatement ps = c.prepareStatement("select leaderboard_attribute_id_slug, conversion, hidden, ordinal from quest_leaderboard where quest_id = ? " +
            "and attributes_id = ? and attributes_tag " + ((attributesTag == null) ? "is null" : "= ?")))
        {
            ps.setLong(1, questId);
            ps.setLong(2, attributesId);

            if (attributesTag != null) {
                ps.setString(3, attributesTag);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String slug = rs.getString(1);
                    Double conversion = rs.getDouble(2);
                    if (rs.wasNull()) {
                        conversion = null;
                    }
                    int hidden = rs.getInt(3);
                    Integer ordinal = rs.getInt(4);
                    if (rs.wasNull()) {
                        ordinal = null;
                    }
                    result.add(new QuestLeaderboard(questId, slug, attributesId, attributesTag, conversion, (hidden != 0), ordinal));
                }
            }
        } catch (Exception e) {
            Logger.error("getLeaderboardSlugForRealtimeAttribute - error", e);
            result.clear();
        }

        return result;
    }

    // Returns null if no record found
    public static Integer getLeaderboardMemberId(Connection c, long questId, long userId) {
        Integer ret = null;
        try (PreparedStatement ps = c.prepareStatement("select id from leaderboard_member where quest_id = ? and platform_user_id = ?")) {

            ps.setLong(1, questId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                // The query uses a unique key so this should be fine
                while (rs.next()) {
                    ret = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            Logger.error("getLeaderboardMemberId - error", e);
            ret = null;
        }

        return ret;
    }

    // Use a differential to avoid a select-then-update
    public static boolean updateLeaderboardScore(Connection c, int memberId, String slug, double scoreDifferential, boolean add) {
        boolean ret;
        try (PreparedStatement ps = c.prepareStatement(
                "insert into leaderboard_score (leaderboard_member_id, leaderboard_attribute_id_slug, score) values (?, ?, ?) on duplicate key update score = " + (add ? "score + ?" : "?")
            ))
        {
            ps.setLong(1, memberId);
            ps.setString(2, slug);
            ps.setDouble(3, scoreDifferential);
            ps.setDouble(4, scoreDifferential);

            ps.executeUpdate();
            ret = true;
        } catch (Exception e) {
            Logger.error("updateLeaderboardScore - error", e);
            ret = false;
        }

        return ret;
    }
}
