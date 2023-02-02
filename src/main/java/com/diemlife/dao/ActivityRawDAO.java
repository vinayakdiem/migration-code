package dao;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;

import com.typesafe.config.Config;

import constants.ActivityEventType;
import constants.ActivityUnit;
import models.ActivityRaw;

import play.Logger;

public class ActivityRawDAO extends DynamoDAO {

    // ID types to use in sort key
    // note: also used by ActivityDAO
    public static final String IDTYPE_USER = "USER";
    public static final String IDTYPE_TEAM = "TEAM";
    public static final String IDTYPE_QUEST = "QUEST";

    // Timestamp in ms
    private static final String TS = "ts";
    
    // Identifier plus Identifier Type and sequence
    private static final String ID_IDTYPE_SEQUENCE = "id_idType_sequence";
    
    // Identifier
    private static final String ID = "id";

    // Type: { "USER", "TEAM", "QUEST" }
    private static final String IDTYPE = "idType";

    // Sequence (number)
    private static final String SEQUENCE = "sequence";

    // Not all of these may be present
    private static final String USERNAME = "username";
    private static final String TEAM_ID = "teamId";
    private static final String QUEST_ID = "questId";
    private static final String TASK_ID = "taskId";

    // Save geo location 
    private static final String LAT = "lat";
    private static final String LON = "lon";

    // Top-level comment details
        // A comment's text
    private static final String COMMENT = "comment";
        // A comment's image
    private static final String COMMENT_IMG_URL = "commentImgUrl";

    // Realtime quest details
    private static final String QUANTITY = "quantity";
    private static final String UNIT = "unit";
    private static final String TAG = "tag";

    // What happened
    private static final String EVENTTYPE = "eventType";

    // INTERNAL DIAGNOSTIC USE ONLY: front end will have to construct the actual displayed message for multi-language reasons AND because quest names can be edited.
    private static final String MSG = "msg";

    private static final String HASH_KEY = TS;
    private static final String RANGE_KEY = ID_IDTYPE_SEQUENCE;

    // note: also used by ActivityDAO
    public static String selectId(String idType, String username, Long teamId, Long questId) {
        String ret;
        switch (idType) {
            case IDTYPE_QUEST:
                ret = questId.toString();
                break;
            case IDTYPE_TEAM:
                ret = teamId.toString();
                break;
            case IDTYPE_USER:
            default:
                ret = username;
                break;
        }

        return ret;
    }

    private static String constructRangeKey(String id, String idType, int sequence) {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append('_');
        sb.append(idType);
        sb.append('_');
        sb.append(sequence);
        return sb.toString();
    }

    public ActivityRawDAO(Config conf) {
        super(conf, "activity_raw");
    }

    // TODO: turn the insert code into perhaps a builder pattern to avoid the mess of adding new attributes and having to update things everywhere.

    // A very crude way of getting around the fact that you could have more than one of the same event type per millisecond for a host of reasons.  There shouldn't be more than
    // 100 events of the same time within a single millisecond, even with very high load.  This could break down if, say, 10,000 - 100,000 people joined a quest in the same second.
    //
    // Returns null on error, the inserted activity if successful
    public ActivityRaw insert(long ts, String idType, ActivityEventType eventType, String msg, String username, Long teamId, Long questId, Long taskId, Double lat, Double lon, 
        String comment, String commentImgUrl, Double quantity, ActivityUnit unit, String tag)
    {
        for (int i = 0; i < 100; i++) {
            if (insert(ts, idType, i, eventType, msg, username, teamId, questId, taskId, lat, lon, comment, commentImgUrl, quantity, unit, tag)) {
                return new ActivityRaw(ts, idType, i, eventType, msg, username, teamId, questId, taskId, lat, lon, comment, commentImgUrl, quantity, unit, tag);
            }
        }

        // Ran out of chances
        return null;
    }

    private boolean insert(long ts, String idType, int sequence, ActivityEventType eventType, String msg, String username, Long teamId, Long questId, Long taskId, Double lat, Double lon,
        String comment, String commentImgUrl, Double quantity, ActivityUnit unit, String tag)
    {
        String id = selectId(idType, username, teamId, questId);

        Item item = new Item();
        item.withPrimaryKey(HASH_KEY, ts, RANGE_KEY, constructRangeKey(id, idType, sequence));

        item.withString(ID, id);
        item.withString(IDTYPE, idType);
        item.withNumber(SEQUENCE, sequence);
        item.withString(EVENTTYPE, eventType.toString());
        item.withString(MSG, msg);

        if (username != null) {
            item.withString(USERNAME, username);
        }
        
        if (teamId != null) {
            item.withNumber(TEAM_ID, teamId);
        }

        if (questId != null) {
            item.withNumber(QUEST_ID, questId);
        }

        if (taskId != null) {
            item.withNumber(TASK_ID, taskId);
        }

        if (lat != null) {
            item.withNumber(LAT, lat);
        }

        if (lon != null) {
            item.withNumber(LON, lon);
        }

        if (comment != null) {
            item.withString(COMMENT, comment);
        }

        if (commentImgUrl != null) {
            item.withString(COMMENT_IMG_URL, commentImgUrl);
        }

        if (quantity != null) {
            item.withNumber(QUANTITY, quantity);
        }

        if (unit != null) {
            item.withString(UNIT, unit.toString());
        }

        if (tag != null) {
            item.withString(TAG, tag);
        }

        PutItemSpec putItemSpec = new PutItemSpec();
        putItemSpec.withItem(item);
        putItemSpec.withConditionExpression("attribute_not_exists(" + HASH_KEY + ") AND attribute_not_exists(" + RANGE_KEY + ")");

        try {
            PutItemOutcome putItemOutcome = this.table.putItem(putItemSpec);
            // TODO: catch specifics of key collision so that we can quietly increment our sequence and try again rather than always (rudely) printing an error message
            // for a situation we can handle
        } catch (Exception e) {
            // IIRC, this throws a runtime exception on any error.  FIXME: We would need to check for exceptions to know about throughput exceeeded or any other issues
            Logger.warn("insert - failed, though this is not necessarily an error to worry about.  Look at stacktrace details: ", e);
            return false;
        }

        return true;
    }
}
