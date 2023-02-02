package dao;

import models.Attribute;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import play.Logger;

import javax.persistence.EntityManager;

import constants.ActivityUnit;


public class AttributesDAO {

	private static AttributesDAO sInstance = new AttributesDAO();

    private AttributesDAO() {

    }

    public static AttributesDAO getInstance() {
        return sInstance;
    }

	public Attribute getAttributeById(Connection c, long id) {

		try (PreparedStatement ps = c.prepareStatement("select quest_id, attribute_name, created_by, added_date, tags, unit from attributes where id = ?")) {

			ps.setLong(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					long questId = rs.getLong(1);
					String attributeName = rs.getString(2);
					long createdBy = rs.getLong(3);
					java.sql.Timestamp addedDate = rs.getTimestamp(4);
					String tags = rs.getString(5);
					String unit = rs.getString(6);

					return new Attribute(id, attributeName, questId, createdBy, new Date(addedDate.getTime()), tags, ((unit == null) ? null : ActivityUnit.valueOf(unit)));
				}
			}
		} catch (Exception e) {
			Logger.error("getAttributeById - error", e);
		}

		return null;	
	}

	public List<Attribute> getAttributesByQuestId(Connection c, long questId) {
		List<Attribute> result = new LinkedList<Attribute>();

		try (PreparedStatement ps = c.prepareStatement("select id, attribute_name, created_by, added_date, tags, unit from attributes where quest_id = ?")) {

			ps.setLong(1, questId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long id = rs.getLong(1);
					String attributeName = rs.getString(2);
					long createdBy = rs.getLong(3);
					java.sql.Timestamp addedDate = rs.getTimestamp(4);
					String tags = rs.getString(5);
					String unit = rs.getString(6);

					result.add(new Attribute(id, attributeName, questId, createdBy, new Date(addedDate.getTime()), tags, ((unit == null) ? null : ActivityUnit.valueOf(unit))));
				}
			}
		} catch (Exception e) {
			Logger.error("getAttributesByQuestId - error", e);
			result.clear();
		}

		return result;
    }

    public boolean insertAttributeValue(Connection c, long attributeId, String attributeValue, long userId, String tag) {
		boolean ret;
		try (PreparedStatement ps = c.prepareStatement("insert into attribute_values (attribute_id, attribute_value, created_by, added_date, tag) values (?, ?, ?, ?, ?)")) {
			ps.setLong(1, attributeId);
			ps.setString(2, attributeValue);
			ps.setLong(3, userId);
			ps.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));

            if (tag == null) {
			    ps.setNull(5, java.sql.Types.VARCHAR);
			} else {
                ps.setString(5, tag);
            }

			ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("insertAttributeValue - error", e);
			ret = false;
		}

		return ret;
	}

	public static Double getDistanceAttributeValueByQuestId(final Integer questId, final Integer userId, final String distanceAttributeName, final EntityManager entityManager) {
    	return (Double) entityManager.createNativeQuery("" +
				"SELECT SUM(attr_v.attribute_value) AS distance " +
				"FROM attributes attr " +
				"INNER JOIN attribute_values attr_v ON attr.id = attr_v.attribute_id " +
				"LEFT OUTER JOIN quest_teams qt ON qt.quest_id = attr.quest_id AND qt.creator_user_id = :userId " +
				"WHERE attr.attribute_name = :attributeName " +
				"  AND attr.quest_id = :questId " +
				"  AND attr_v.created_by IN (SELECT :userId UNION SELECT qtm.member_id FROM quest_team_members qtm WHERE qtm.team_id = qt.id)")
					.setParameter("questId", questId)
					.setParameter("userId", userId)
					.setParameter("attributeName", distanceAttributeName)
					.getSingleResult();
	}

	// TODO: aggregation should perhaps be based around leaderboard_attribute.unit somehow.  That would be better than this mess.

	// attributes id implies quest id ... conversionOrNot says whether to aggregate values with a conversion or not ==> not a nice approach, needs improvement
	public Map<Long, Double> aggregateValuesByUser(Connection c, long attributeId, boolean conversionOrNot) {
		Map<Long, Double> result = new HashMap<Long, Double>();

		// Note: there is an implicit assumption that, despite being a string field, the attribute values are numeric, which is why there is a conversion
        // to Double below.  Also, the query here is intentionally joining these tables for the tag=NULL scenario.
		try (PreparedStatement ps = c.prepareStatement("select av.created_by, av.attribute_value, ql.conversion from attribute_values av join quest_leaderboard ql on " +
			"av.attribute_id = ql.attributes_id and ((isnull(av.tag) and isnull(ql.attributes_tag)) or (av.tag = ql.attributes_tag)) where av.attribute_id = ? and " +
            "ql.conversion " + (conversionOrNot ? "is not null" : "is null")))
		{
			ps.setLong(1, attributeId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long userId = rs.getLong(1);
					String value = rs.getString(2);
					Double _value;
					try {
                        // Is the attribute value of the form hh:mm:ss?
                        if (value.contains(":")) {
                            String[] parts = value.split(":");
                            _value = (double) (Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]));
                        } else {
                            _value = Double.parseDouble(value);
                        }
					} catch (Exception e) {
                        Logger.warn("aggregateValuesByUser - attribute value is not hh:mm:ss or a Double: " + value);
						continue;
                    }
					Double conversion = rs.getDouble(3);
					if (rs.wasNull()) {
						conversion = null;
                    }
					_value = ((conversion == null) ? _value : (conversion * _value));

					Double curr = result.get(userId);
					result.put(userId, (curr == null) ? _value : (curr + _value));
				}
			}
		} catch (Exception e) {
			Logger.error("aggregateValuesByUser - error", e);
			result.clear();
		}

		return result;
    }
}
