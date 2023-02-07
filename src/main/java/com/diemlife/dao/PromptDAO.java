package com.diemlife.dao;

import com.diemlife.constants.PromptType.PromptEventType;
import com.diemlife.models.PromptUser;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import java.util.TreeMap;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Repository
public class PromptDAO {

    private static PromptDAO sInstance = new PromptDAO();

    private PromptDAO() {
		
    }

    public static PromptDAO getInstance() {
        return sInstance;
    }

	public String[] getPromptOption(Connection c, String msg) {
		TreeMap<Integer, String> puList = new TreeMap<Integer, String>();
		try (PreparedStatement ps = c.prepareStatement("select option_msg, option_order from prompt_option where msg = ?")) {
			
			ps.setString(1, msg);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String optionMsg = rs.getString(1);
					
					// TODO -- this is DEFAULT NULL in the DB which is probably a bug
					Integer optionOrder = rs.getInt(2);
					if (!rs.wasNull()) {
						puList.put(optionOrder, optionMsg);
					}
				}
			}
		} catch (Exception e) {
			Logger.error("_getPromptOption - error", e);
			puList.clear();
		}
		
		return puList.values().toArray(new String[0]);
	}
	
	public List<PromptUser> getUserPrompt(Connection c, String username, long questId) {
		TreeMap<Integer, PromptUser> puList = new TreeMap<Integer, PromptUser>();
		try (PreparedStatement ps = c.prepareStatement("select prompt_event, msg, msg_type, prompt_order, has_custom_options from prompt_user where username = ? and quest_id = ?")) {
			
			ps.setString(1, username);
			ps.setLong(2, questId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long promptEvent = rs.getLong(1);
					String msg = rs.getString(2);
					int msgType = rs.getInt(3);
					
					// TODO -- this should default to an INT in the DB
					Integer promptOrder = rs.getInt(4);
					if (rs.wasNull()) {
						promptOrder = Integer.MIN_VALUE;
					}
					boolean hasCustomOptions = rs.getBoolean(5);
					
					if (hasCustomOptions) {
						puList.put(promptOrder, new PromptUser(username, questId, promptEvent, msg, msgType, getPromptOption(c, msg)));
					} else {
						puList.put(promptOrder, new PromptUser(username, questId, promptEvent, msg, msgType));
					}
				}
			}
		} catch (Exception e) {
			Logger.error("_getUserPrompt - error", e);
			puList.clear();
		}
		
		return new LinkedList<PromptUser>(puList.values());
	}

	public boolean insertUserPrompt(Connection c, String username, long questId, long promptEvent, String msg, int msgType) {
		boolean ret;
		// TODO: change this to delayed insert once InnoDB is ditched in favor of MyISAM
		try (PreparedStatement ps = c.prepareStatement("insert into prompt_user (username, quest_id, prompt_event, msg, msg_type) values (?, ?, ?, ?, ?)")) {
			ps.setString(1, username);
			ps.setLong(2, questId);
			ps.setLong(3, promptEvent);
			ps.setString(4, msg);
			ps.setInt(5, msgType);
			
			int result = ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("insertUserPrompt - error", e);
			ret = false;
		}
		
		return ret;	
	}
	
	public boolean deleteUserPrompt(Connection c, String username, long questId, long promptEvent, String msg) {
		boolean ret;
		try (PreparedStatement ps = c.prepareStatement("delete from prompt_user where username = ? and quest_id = ? and prompt_event = ? and msg = ?")) {
			ps.setString(1, username);
			ps.setLong(2, questId);
			ps.setLong(3, promptEvent);
			ps.setString(4, msg);
			int result = ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("deleteUserPrompt - error", e);
			ret = false;
		}
		
		return ret;
	}

	public boolean insertPromptResult(Connection c, String username, long questId, long ts, long promptEvent, String msg, int msgType, String response, Double lat, Double lon) {
		boolean ret;
		// TODO: change this to delayed insert once InnoDB is ditched in favor of MyISAM
		try (PreparedStatement ps = c.prepareStatement("insert into prompt_result (username, quest_id, ts, prompt_event, msg, msg_type, response, lat, lon) values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			ps.setString(1, username);
			ps.setLong(2, questId);
			ps.setTimestamp(3, new java.sql.Timestamp(ts));
			ps.setLong(4, promptEvent);
			ps.setString(5, msg);
			ps.setInt(6, msgType);
			ps.setString(7, response);
			
			if (lat == null) {
				ps.setNull(8, java.sql.Types.DOUBLE);
			} else {
				ps.setDouble(8, lat);
			}
			
			if (lon == null) {
				ps.setNull(9, java.sql.Types.DOUBLE);
			} else {
				ps.setDouble(9, lon);
			}
			
			int result = ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("insertPromptResult - error", e);
			ret = false;
		}
		
		return ret;
	}
	
	// Returns true if a prompt is found, false if a prompt is not found, null on error
	// TODO: consider removing try-catch and having it throw an exception on error
	private Boolean userPromptExists(Connection c, String username, long questId, long promptEvent /*, String msg, int msgType */) {
		Boolean ret;
		// Note: the original hibernate version of this was a doing a count(*) and checking if the result was of at least one row.  Doing a "select 1 from foo limit 1" I think achieves that in a
		// simpler way.
		//
		// Also, the msg field was specified in the original hibernate query string but then never had a parameter assigned to it.  Unclear what the intention was.  I have left it
		// emulating the in effect behavior.
		try (PreparedStatement ps = c.prepareStatement("select 1 from prompt_user where username = ? and quest_id = ? and prompt_event = ?" /* + " and msg = ?" */ + " limit 1" )) {
			ps.setString(1, username);
			ps.setLong(2, questId);
			ps.setLong(3, promptEvent);
			
			try (ResultSet rs = ps.executeQuery()) {
				ret = rs.first();
			}
		} catch (Exception e) {
			Logger.error("userPromptExists - error", e);
			ret = null;
		}
		
		return ret;
	}
	
    public boolean pushPromptToUser(Connection c, final User user, final Quests quest, final PromptEventType eventType, final String question, final int questionType) {
		String username = user.getUserName();
		long questId = quest.getId().longValue();
		long promptEvent = eventType.getCode();
		
		// Use RW connection here so that we can get a "consistent" read of the existance of the row as we are making write decisions based on this.
		Boolean promptExists = userPromptExists(c, username, questId, promptEvent);
		if (promptExists == null) {
			Logger.error(format("Unable to push prompt to user %s and Quest with ID %s", username, questId));
			return false;
		} else if (promptExists) {
			Logger.warn(format("Prompt already exists for user '%s', Quest [%s] and prompt event [%s]", username, questId, promptEvent));
			return false;
		} else {
			return insertUserPrompt(c, username, questId, promptEvent, question, questionType);
		} 
    }
}

