package com.diemlife.dao;

import com.diemlife.models.QuestEdge;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.LinkedList;
import java.util.List;

import play.Logger;

import constants.ActivityUnit;


public class QuestEdgeDAO {

	private static QuestEdgeDAO sInstance = new QuestEdgeDAO();

    private QuestEdgeDAO() {

    }

    public static QuestEdgeDAO getInstance() {
        return sInstance;
    }

	public QuestEdge getEdge(Connection c, long questSrc, String type, long questDst) {

		try (PreparedStatement ps = c.prepareStatement("select tags from quest_edge where quest_src = ? and type = ? and quest_dst = ?")) {

			ps.setLong(1, questSrc);
            ps.setString(2, type);
            ps.setLong(3, questDst);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					String tags = rs.getString(1);

					return new QuestEdge(questSrc, type, questDst, tags);
				}
			}
		} catch (Exception e) {
			Logger.error("getEdge - error", e);
		}

		return null;	
	}

	public QuestEdge getQuestForEdge(Connection c, long questDst, String type) {

		try (PreparedStatement ps = c.prepareStatement("select quest_src, tags from quest_edge where quest_dst = ? and type = ?")) {

			ps.setLong(1, questDst);
			ps.setString(2, type);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Long questSrc = rs.getLong(1);
					String tags = rs.getString(2);

					return new QuestEdge(questSrc, type, questDst, tags);
				}
			}
		} catch (Exception e) {
			Logger.error("getEdge - error", e);
		}

		return null;
	}

	public List<QuestEdge> getEdgesByType(Connection c, long questSrc, String type) {
		List<QuestEdge> result = new LinkedList<QuestEdge>();

		try (PreparedStatement ps = c.prepareStatement("select quest_dst, tags from quest_edge where quest_src = ? and type = ?")) {

			ps.setLong(1, questSrc);
			ps.setString(2, type);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long questDst = rs.getLong(1);
					String tags = rs.getString(2);

					result.add(new QuestEdge(questSrc, type, questDst, tags));
				}
			}
		} catch (Exception e) {
			Logger.error("getEdgesByType - error", e);
			result.clear();
		}

		return result;
    }
}
