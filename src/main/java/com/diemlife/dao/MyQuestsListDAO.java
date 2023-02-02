package com.diemlife.dao;

import constants.QuestActivityStatus;
import constants.QuestMode;
import constants.QuestUserFlagKey;
import dto.MyQuestDTO;
import play.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MyQuestsListDAO {
    
    /*
     Note: here is the schema that the query creates:

                questId, BIGINT
                questCreatedDate, DATETIME
                questActivityExists, INT
                questActivityMode, VARCHAR
                questActivityStatus, VARCHAR
                questInProgress, BIGINT
                questCompleted, BIGINT
                questSaved, BIGINT
                questStarred, INT
                questFollowed, INT
                questRepeatable, INT
                questCompleteCounter, BIGINT
                userId, BIGINT
                tasksCompletedDoer, BIGINT
                tasksTotalDoer, BIGINT
                tasksCompletedCreator, BIGINT
                tasksTotalCreator, BIGINT
    */
    private static final String LOOKUP_QUERY = "" +
                "SELECT * " +
                "FROM (SELECT q.quest_id                                                          AS questId, " +
                "             MAX(qf.created_date)                                                AS questCreatedDate, " +
                "             IF(COALESCE(qaf.id, 0) = 0, 0, 1)                                   AS questActivityExists, " +
                "             MAX(q.quest_activity_mode)                                          AS questActivityMode, " +
                "             MAX(q.quest_activity_status)                                        AS questActivityStatus, " +
                "             MAX(q.quest_in_progress)                                            AS questInProgress, " +
                "             MAX(q.quest_completed)                                              AS questCompleted, " +
                "             MAX(q.quest_saved)                                                  AS questSaved, " +
                "             IF(MAX(qus.flag_value) = TRUE, 1, 0)                                AS questStarred, " +
                "             IF(MAX(quf.flag_value) = TRUE, 1, 0)                                AS questFollowed, " +
                "             IF(SUM(COALESCE(e.id, 0)) = 0, 1, 0)                                AS questRepeatable, " +
                "             MAX(q.quest_complete_counter)                                       AS questCompleteCounter, " +
                "             IF(COALESCE(qaf.id, 0) = 0, qf.created_by, q.user_id)               AS userId, " +
                "             COUNT(DISTINCT IF((qt_d.task_completed = '" + Boolean.TRUE.toString().toUpperCase() + "'), qt_d.id, NULL))    AS tasksCompletedDoer, " +
                "             COUNT(DISTINCT qt_d.id)                                                                                     AS tasksTotalDoer, " +
                "             COUNT(DISTINCT IF((qt_c.task_completed = '" + Boolean.TRUE.toString().toUpperCase() + "'), qt_c.id, NULL))    AS tasksCompletedCreator, " +
                "             COUNT(DISTINCT qt_c.id)                                                                                     AS tasksTotalCreator " +
                "      FROM (SELECT my_quests.user_id                     AS user_id, " +
                "                   my_quests.quest_id                    AS quest_id, " +
                "                   MAX(my_quests.quest_activity_mode)    AS quest_activity_mode, " +
                "                   MAX(my_quests.quest_activity_status)  AS quest_activity_status, " +
                "                   MAX(my_quests.quest_in_progress)      AS quest_in_progress, " +
                "                   MAX(my_quests.quest_completed)        AS quest_completed, " +
                "                   MAX(my_quests.quest_complete_counter) AS quest_complete_counter, " +
                "                   MAX(my_quests.quest_saved)            AS quest_saved " +
                "            FROM ( " +
                "                     SELECT qa.user_id                                                AS user_id, " +
                "                            qa.quest_id                                               AS quest_id, " +
                "                            qa.mode                                                                                           AS quest_activity_mode, " +
                "                            qa.status                                                                                         AS quest_activity_status, " +
                "                            IF(qa.status = '" + QuestActivityStatus.IN_PROGRESS.name() + "', 1, 0)                              AS quest_in_progress, " +
                "                            IF(qa.status = '" + QuestActivityStatus.COMPLETE.name() + "' OR qa.cycles_counter > 0, 1, 0)        AS quest_completed, " +
                "                            qa.cycles_counter                                                                                 AS quest_complete_counter, " +
                "                            0                                                                                                 AS quest_saved " +
                "                     FROM quest_activity qa " +
                "                     WHERE qa.user_id = ? " +
                "                     UNION ALL " +
                "                     SELECT qs.user_id  AS user_id, " +
                "                            qs.quest_id AS quest_id, " +
                "                            NULL        AS quest_activity_mode, " +
                "                            NULL        AS quest_activity_status, " +
                "                            0           AS quest_in_progress, " +
                "                            0           AS quest_completed, " +
                "                            0           AS quest_complete_counter, " +
                "                            1           AS quest_saved " +
                "                     FROM quest_saved qs " +
                "                     WHERE qs.user_id = ? " +
                "                 ) my_quests " +
                "            GROUP BY my_quests.quest_id, my_quests.user_id) q " +
                "               INNER JOIN quest_feed qf ON qf.id = q.quest_id " +
                "               LEFT OUTER JOIN quest_activity qaf ON qaf.quest_id = qf.id AND qaf.user_id = ? " +
                "               LEFT OUTER JOIN event e ON e.quest_id = q.quest_id " +
                "               LEFT OUTER JOIN quest_user_flags qus ON qus.quest_id = q.quest_id AND qus.user_id = q.user_id AND qus.flag_key = '" + QuestUserFlagKey.STARRED.name() + "' " +
                "               LEFT OUTER JOIN quest_user_flags quf ON quf.quest_id = q.quest_id AND quf.user_id = q.user_id AND quf.flag_key = '" + QuestUserFlagKey.FOLLOWED.name() + "' " +
                "               LEFT OUTER JOIN quest_tasks qt_d ON qt_d.quest_id = q.quest_id AND qt_d.user_id = q.user_id " +
                "               LEFT OUTER JOIN quest_tasks qt_c ON qt_c.quest_id = qf.id AND qt_c.user_id = qf.created_by " +
                "      GROUP BY q.quest_id) q_unsorted " +
                "ORDER BY q_unsorted.questStarred DESC, " +
                "         q_unsorted.questInProgress DESC, " +
                "         q_unsorted.questCompleted DESC, " +
                "         q_unsorted.questSaved DESC, " +
                "         q_unsorted.questCompleteCounter DESC, " +
                "         (q_unsorted.tasksCompletedDoer / q_unsorted.tasksTotalDoer) DESC, " +
                "         (q_unsorted.tasksCompletedCreator / q_unsorted.tasksTotalCreator) DESC, " +
                "         questFollowed DESC, " +
                "         questCreatedDate DESC";

    public List<MyQuestDTO> loadMyQuests(Connection c, long userId) {
        
        try (PreparedStatement ps = c.prepareStatement(LOOKUP_QUERY)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);

            LinkedList<MyQuestDTO> result = new LinkedList<MyQuestDTO>();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
					MyQuestDTO qDto = new MyQuestDTO();
                    qDto.questId = rs.getLong(1);
                    qDto.userId = rs.getLong(13);
                    qDto.questActivityExists = rs.getBoolean(3);
                    
                    // TODO: why is this null?
                    String questMode = rs.getString(4);
                    qDto.questActivityMode = ((questMode == null) ? null : QuestMode.valueOf(questMode));
                    
                    // TODO: why is this null?
                    String questActivityStatus = rs.getString(5);
                    qDto.questActivityStatus = ((questActivityStatus == null) ? null : QuestActivityStatus.valueOf(questActivityStatus));
                    
                    qDto.questStarred = rs.getBoolean(9);
                    qDto.questInProgress = rs.getBoolean(6);
                    qDto.questCompleted = rs.getBoolean(7);
                    qDto.questSaved = rs.getBoolean(8);
                    qDto.questFollowed = rs.getBoolean(10);
                    qDto.questRepeatable = rs.getBoolean(11);
                    qDto.questCompleteCounter = rs.getLong(12);
                    qDto.tasksCompletedDoer = rs.getLong(14);
                    qDto.tasksTotalDoer = rs.getLong(15);
                    qDto.tasksCompletedCreator = rs.getLong(16);
                    qDto.tasksTotalCreator = rs.getLong(17);

                    result.add(qDto);
				}
			}

            return result;

        } catch (Exception e) {
			Logger.error("loadMyQuests - error", e);
			return Collections.emptyList();
		}
    }
}
