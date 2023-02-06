package com.diemlife.dao;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.diemlife.constants.ActivityEventType;
import com.diemlife.constants.ActivityUnit;
import com.diemlife.models.Activity;
import com.diemlife.models.ActivityRaw;
import com.typesafe.config.Config;

import play.Logger;

public class ActivityDAO extends DynamoDAO {

    // unique id
    private static final String UID = "uid";

    // begin: activity_raw copied attributes
        // Timestamp in ms
    private static final String TS = "ts";
   
        // What happened
    private static final String EVENTTYPE = "eventType";

        // Src Event ID Type
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

        // INTERNAL DIAGNOSTIC USE ONLY: front end will have to construct the actual displayed message for multi-language reasons AND because quest names can be edited.
    private static final String MSG = "msg";
    // end: activity_raw copied attributes

    // Event type specific attributes.  Not all of these may be present.
        // A comment's or reply's text
    private static final String COMMENT = "comment";
        // A comment's image
    private static final String COMMENT_IMG_URL = "commentImgUrl";
        // If the user wants to delete a comment, we will "soft" delete it by adding this tag to keep auditing
    private static final String DELETED = "deleted";
        // Edit history
    private static final String COMMENT_HISTORY = "commentHistory";
    private static final String COMMENT_IMG_URL_HISTORY = "commentImgUrlHistory";

        // Realtime quest details
    private static final String QUANTITY = "quantity";
    private static final String UNIT = "unit";
    private static final String TAG = "tag";

        // The activity record being liked or replied
    private static final String TARGET_ACTIVITY_UID = "targetActivityUid";
        // number of likes for this activity
    private static final String CHEER_COUNT = "cheerCount";
        // postal code for the activity
    private static final String POSTAL_CODE = "postalCode";

    // Organize results by day, hour
    private static final String CREATED_DAY = "createdDay";
    private static final String CREATED_HOUR = "createdHour";

    // Primary key: uid
    // Lookup an individual activity item
    private static final String HASH_KEY = UID;

    // GSI: (username, ts)
    // Lookup all activity for a user after a point in time
    private static final String USERNAME_TS_GSI = "username-ts-index";

    // GSI: (questId, ts)
    // Lookup all activity for a quest after a point in time
    private static final String QUEST_ID_TS_GSI = "questId-ts-index";

    // GSI: (teamdId, ts)
    // Lookup all activity for a team after a point in time
    private static final String TEAM_ID_TS_GSI = "teamId-ts-index";

    // GSI: (taskId, ts)
    // Lookup all activity for a task after a point in time
    private static final String TASK_ID_TS_GSI = "taskId-ts-index";

    // GSI: (createdDay, ts)
    // Lookup all activity on a particular day
    private static final String CREATED_DAY_TS_GSI = "createdDay-ts-index";

    // GSI: (createdHour, ts)
    // Lookup all activity on a particular hour
    private static final String CREATED_HOUR_TS_GSI = "createdHour-ts-index";

    // GSI: (targetActivityUid, eventType)
    // Lookup meta activity for an eventType that targets an individual activity item
    private static final String TARGET_ACTIVITY_UID_EVENTTYPE_GSI = "targetActivityUid-eventType-index";

    // GSI: (targetActivityUid, username)
    // Lookup all activity for a user that targets an individual activity item
    private static final String TARGET_ACTIVITY_UID_USERNAME_GSI = "targetActivityUid-username-index";

    private static DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.US).withZone(ZoneOffset.UTC);
    private static DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH").withLocale(Locale.US).withZone(ZoneOffset.UTC);

    Index usernameTsGsi;
    Index questIdTsGsi;
    Index teamIdTsGsi;
    Index taskIdTsGsi;
    Index createdDayTsGsi;
    Index createdHourTsGsi;
    Index targetActivityUidEventTypeGsi;
    Index targetActivityUidUsernameGsi;

    public ActivityDAO(Config conf) {
        super(conf, "activity");

        // TODO: need a separate table to manage cheers for better performance. It would be something like (targetActivityUid, username) and (username, targetActivityUid) for
        // primary key and GSI

        this.usernameTsGsi = this.table.getIndex(USERNAME_TS_GSI);
        this.questIdTsGsi = this.table.getIndex(QUEST_ID_TS_GSI);
        this.teamIdTsGsi = this.table.getIndex(TEAM_ID_TS_GSI);
        this.taskIdTsGsi = this.table.getIndex(TASK_ID_TS_GSI);
        this.createdDayTsGsi = this.table.getIndex(CREATED_DAY_TS_GSI);
        this.createdHourTsGsi = this.table.getIndex(CREATED_HOUR_TS_GSI);
        this.targetActivityUidEventTypeGsi = this.table.getIndex(TARGET_ACTIVITY_UID_EVENTTYPE_GSI);
        this.targetActivityUidUsernameGsi = this.table.getIndex(TARGET_ACTIVITY_UID_USERNAME_GSI);
    }

    // Converts a raw activity to an activity record and inserts it
    //
    // On success, returns the new Activity.  On failure returns null
    public Activity insert(ActivityRaw activityRaw) {
        return insert(activityRaw.getTs(), activityRaw.getIdType(), activityRaw.getSequence(), activityRaw.getEventType(), activityRaw.getMsg(), activityRaw.getUsername(), activityRaw.getTeamId(),
            activityRaw.getQuestId(), activityRaw.getTaskId(), activityRaw.getLat(), activityRaw.getLon(), activityRaw.getComment(), activityRaw.getCommentImgUrl(), null, null, 
            activityRaw.getQuantity(), activityRaw.getUnit(), activityRaw.getTag());
    }

    // On success, returns the new Activity.  On failure returns null
    public Activity insert(long ts, String idType, int sequence, ActivityEventType eventType, String msg, String username, Long teamId, Long questId, Long taskId, Double lat, Double lon, String comment,
        String commentImgUrl, String targetActivityUid, String postalCode, Double quantity, ActivityUnit unit, String tag)
    {
        // Grab a UUID and strip off the '-' chars
        String uid = UUID.randomUUID().toString().replace("-", "");

        Item item = new Item();
        item.withPrimaryKey(HASH_KEY, uid);

        item.withString(UID, uid);
        item.withNumber(TS, ts);
        item.withString(IDTYPE, idType);
        item.withNumber(SEQUENCE, sequence);
        item.withString(EVENTTYPE, eventType.toString());
        item.withString(MSG, msg);

        Instant instant = Instant.ofEpochMilli(ts);
        item.withString(CREATED_DAY, dayFormat.format(instant));
        item.withString(CREATED_HOUR, hourFormat.format(instant));

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

        if (targetActivityUid != null) {
            item.withString(TARGET_ACTIVITY_UID, targetActivityUid);
        }

        if (postalCode != null) {
            item.withString(POSTAL_CODE, postalCode);
        }

        PutItemSpec putItemSpec = new PutItemSpec();
        putItemSpec.withItem(item);

        try {
            PutItemOutcome putItemOutcome = this.table.putItem(putItemSpec);
        } catch (Exception e) {
            Logger.error("insert - failed", e);
            return null;
        }

        return new Activity(uid, ts, eventType, idType, sequence, username, teamId, questId, taskId, lat, lon, msg, comment, commentImgUrl, targetActivityUid, null, postalCode, null, quantity, unit, tag);
    }

    private boolean delete(Activity activity) {
        return delete(activity.getUid());
    }

    // Deletes an activity.  Returns true on success, false otherwise
    // TODO: should this cascade and delete all items attached to it?  That should be a background process anyways.
    private boolean delete(String uid) {
        boolean result = false;

        DeleteItemSpec deleteItemSpec = new DeleteItemSpec();
        deleteItemSpec.withPrimaryKey(HASH_KEY, uid);
        try {
            DeleteItemOutcome deleteItemOutcome = this.table.deleteItem(deleteItemSpec);
            result = true;
            // Consider checking to see if the delete actually found an item, maybe we don't care
        } catch (Exception e) {
            Logger.error("delete - failed", e);
        }

        return result;
    }

    private boolean softDelete(Activity activity) {
        return softDelete(activity.getUid());
    }

    // Tags an activity as deleted ... allows us to keep the record for auditing OR allows us to do what Twitter does by just not showing
    // things marked for delete, yet you can still see replies to a deleted thing.
    private boolean softDelete(String uid) {
        boolean result = false;

        UpdateItemSpec updateItemSpec = new UpdateItemSpec();
        updateItemSpec.withPrimaryKey(HASH_KEY, uid);
        updateItemSpec.withAttributeUpdate(new AttributeUpdate(DELETED).put(true));
        try {
            UpdateItemOutcome updateItemOutcome = this.table.updateItem(updateItemSpec);
            result = true;
        } catch (Exception e) {
            Logger.error("softDelete - failed", e);
        }

        return result;
    }

    // Returns null on error, a one-item (or empty) list on success
    public List<Activity> get(String uid) {
        Item item;
        try {
            item = this.table.getItem(HASH_KEY, uid);
        } catch (Exception e) {
            Logger.error("get - failed", e);
            return null;
        }

        List<Activity> result = new LinkedList<Activity>();
        if (item != null) {
            result.add(itemToActivity(item));
        }

        return result;
    }

    public List<Activity> getBatch(Set<String> uids) {
        List<Activity> result = new LinkedList<Activity>();
        try {
            // TODO: in the future, it would be better to do batch getItem to avoid so many round trips
            Iterator<String> uidsIt = uids.iterator();
            while (uidsIt.hasNext()) {
                result.addAll(get(uidsIt.next()));
            }
        } catch (Exception e) {
            Logger.error("getBatch - failed", e);
            result.clear();
        }

        return result;
    }

    private static QuerySpec buildQuerySpec(String hashKey, Object hashKeyValue, Long ts, Integer limit) {
        return buildQuerySpec(hashKey, hashKeyValue, ts, limit, true);
    }

    private static QuerySpec buildQuerySpec(String hashKey, Object hashKeyValue, Long ts, Integer limit, boolean lookForward) {
        QuerySpec querySpec = new QuerySpec();
        querySpec.withHashKey(hashKey, hashKeyValue);
        if (ts != null) {
            if (lookForward) {
                querySpec.withRangeKeyCondition(new RangeKeyCondition(TS).ge(ts));
            } else {
                querySpec.withRangeKeyCondition(new RangeKeyCondition(TS).le(ts));
                querySpec.withScanIndexForward(false);
            }
        }
        if (limit != null) {
            querySpec.withMaxResultSize(limit);
        }
        return querySpec;
    }

    // order is ASC
    private static List<Activity> orderByTs(ItemCollection<QueryOutcome> items) {
        TreeMap<Long, Activity> orderedResults = new TreeMap<Long, Activity>();
        Iterator<Item> itemsIt = items.iterator();
        while (itemsIt.hasNext()) {
            Activity activity = itemToActivity(itemsIt.next());

            // To construct an in order "unique" timestamp, multiply timestamp by 100000 which effectively converts millisecond TS to nanoseconds, the add the sequence value.
            // This should work because the query based lookups are using a particular id type
            orderedResults.put((activity.getTs() * 1000000 + activity.getSequence()), activity);
        }

        return new LinkedList<Activity>(orderedResults.values());
    }

    private static List<Activity> itemsToActivities(ItemCollection<QueryOutcome> items) {
        LinkedList<Activity> result = new LinkedList<Activity>();
        Iterator<Item> itemsIt = items.iterator();
        while (itemsIt.hasNext()) {
            result.add(itemToActivity(itemsIt.next()));
        }
        return result;
    } 

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getByUser(String username, Long ts, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.usernameTsGsi.query(buildQuerySpec(USERNAME, username, ts, limit));
        } catch (Exception e) {
            Logger.error("getByUser - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getRecentByUser(String username, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.usernameTsGsi.query(buildQuerySpec(USERNAME, username, System.currentTimeMillis(), limit, false));
        } catch (Exception e) {
            Logger.error("getRecentByUser - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getByTeam(long teamId, Long ts, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.teamIdTsGsi.query(buildQuerySpec(TEAM_ID, teamId, ts, limit));
        } catch (Exception e) {
            Logger.error("getByTeam - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getRecentByTeam(long teamId, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.teamIdTsGsi.query(buildQuerySpec(TEAM_ID, teamId, System.currentTimeMillis(), limit, false));
        } catch (Exception e) {
            Logger.error("getRecentByTeam - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getByQuest(long questId, Long ts, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.questIdTsGsi.query(buildQuerySpec(QUEST_ID, questId, ts, limit));
        } catch (Exception e) {
            Logger.error("getByQuest - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getRecentByQuest(long questId, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.questIdTsGsi.query(buildQuerySpec(QUEST_ID, questId, System.currentTimeMillis(), limit, false));
        } catch (Exception e) {
            Logger.error("getRecentByQuest - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getByTask(long taskId, Long ts, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.taskIdTsGsi.query(buildQuerySpec(TASK_ID, taskId, ts, limit));
        } catch (Exception e) {
            Logger.error("getByTask - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getRecentByTask(long taskId, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            items = this.taskIdTsGsi.query(buildQuerySpec(TASK_ID, taskId, System.currentTimeMillis(), limit, false));
        } catch (Exception e) {
            Logger.error("getRecentByTask - failed", e);
            return null;
        }

        return orderByTs(items);
    }

    // Returns null on error, a populated (or empty) list on success
    public List<Activity> getByTs(long ts, Integer limit) {
        ItemCollection<QueryOutcome> items;
        try {
            // TODO: using the day view for now.  Consider adding more code here later to check if day is too full to move up to hour and vice versa.
            Instant instant = Instant.ofEpochMilli(ts);
            items = this.createdDayTsGsi.query(buildQuerySpec(CREATED_DAY, dayFormat.format(instant), ts, limit, false));
        } catch (Exception e) {
            Logger.error("getByTs - failed", e);
            return null;
        }

        return orderByTs(items);
    } 

    // TODO: consider adding a filter for the eventType
    public List<Activity> getByTargetActivityAndEventType(String targetActivityUid, ActivityEventType eventType, Integer limit, boolean orderResult) {
        ItemCollection<QueryOutcome> items;
        try {
            QuerySpec querySpec = new QuerySpec();
            querySpec.withHashKey(TARGET_ACTIVITY_UID, targetActivityUid);
            querySpec.withRangeKeyCondition(new RangeKeyCondition(EVENTTYPE).eq(eventType));
            if (limit != null) {
                querySpec.withMaxResultSize(limit);
            }
            items = this.targetActivityUidEventTypeGsi.query(querySpec);
        } catch (Exception e) {
            Logger.error("getByTargetActivityAndEventType - failed", e);
            return null;
        }

        return (orderResult ? orderByTs(items) : itemsToActivities(items));
    }

    // TODO: consider adding a filter for the user
    private List<Activity> getByTargetActivityAndUsername(String targetActivityUid, String username, Integer limit, boolean orderResult) {
        ItemCollection<QueryOutcome> items;
        try {
            QuerySpec querySpec = new QuerySpec();
            querySpec.withHashKey(TARGET_ACTIVITY_UID, targetActivityUid);
            querySpec.withRangeKeyCondition(new RangeKeyCondition(USERNAME).eq(username));
            if (limit != null) {
                querySpec.withMaxResultSize(limit);
            }
            items = this.targetActivityUidUsernameGsi.query(querySpec);
        } catch (Exception e) {
            Logger.error("getByTargetActivityAndUsername - failed", e);
            return null;
        }

        return (orderResult ? orderByTs(items) : itemsToActivities(items));
    }

    public String cheerActivity(Activity targetActivity, String username) {
        String result = null;

        String targetActivityUid = targetActivity.getUid();

        // See if a cheer already exits
        // Lookup all the meta activity for this activity item for a given user.  This should be a short list.
        // TODO: it would be better to use an insert-fail-if-exists pattern to check this
        List<Activity> metaActivityList = getByTargetActivityAndUsername(targetActivityUid, username, null, false);
        for (Activity metaActivity : metaActivityList) {
            if (ActivityEventType.CHEER.equals(metaActivity.getEventType())) {
                // short circuit
                Logger.debug("Uncheering activity " + targetActivityUid + " for user " + username);
                uncheerActivity(targetActivity, username);
                return "UNCHEERED";
            }
        }

        // Add the record for the cheer
        String msg = username + " cheered activity " + targetActivityUid;
        Activity activity = insert(System.currentTimeMillis(), ActivityRawDAO.IDTYPE_USER, 0, ActivityEventType.CHEER, msg, username, null, null, null, null, null, null, null, targetActivityUid, null,
            null, null, null);
        if (activity != null) {

            // Increment the cheer count and don't panic if it fails
            UpdateItemSpec updateItemSpec = new UpdateItemSpec();
            updateItemSpec.withPrimaryKey(HASH_KEY, targetActivityUid);
            updateItemSpec.withAttributeUpdate(new AttributeUpdate(CHEER_COUNT).addNumeric(1));
            try {
                UpdateItemOutcome updateItemOutcome = this.table.updateItem(updateItemSpec);
                result = "CHEERED";
            } catch (Exception e) {
                Logger.error("update - failed", e);
            }
        } else {
            Logger.warn("cheerActivity - warn unable to create cheer activity for " + targetActivityUid + ", skipping counter increment.");
        }

        return result;
    }

    public boolean uncheerActivity(Activity targetActivity, String username) {
        boolean result = false;

        String targetActivityUid = targetActivity.getUid();

        // See if a cheer already exits
        // Lookup all the meta activity for this activity item for a given user.  This should be a short list.
        // TODO: it would be better to just do a delete on a primary key and check if the result count > 0
        boolean didDelete = false;
        List<Activity> metaActivityList = getByTargetActivityAndUsername(targetActivityUid, username, null, false);
        for (Activity metaActivity : metaActivityList) {
            if (ActivityEventType.CHEER.equals(metaActivity.getEventType())) {
                // delete it
                if (delete(metaActivity)) {
                    didDelete = true;
                }
            }
        }

        // Remove the record for the cheer
        if (didDelete) {

            // Decrement the cheer count and don't panic if it fails
            UpdateItemSpec updateItemSpec = new UpdateItemSpec();
            updateItemSpec.withPrimaryKey(HASH_KEY, targetActivityUid);
            updateItemSpec.withAttributeUpdate(new AttributeUpdate(CHEER_COUNT).addNumeric(-1));
            try {
                // TODO: check for new field value < 0, if so, set it to 0 and log a warning.
                UpdateItemOutcome updateItemOutcome = this.table.updateItem(updateItemSpec);
                result = true;
            } catch (Exception e) {
                Logger.error("update - failed", e);
            }
        } else {
            Logger.warn("uncheerActivity - warn unable to delete cheer activity for " + targetActivityUid + ", skipping counter increment.");
        }

        return result;
    }

    // TODO: it would be better to do a direct GetItem with a primary key
    public boolean isCheeredByUser(Activity targetActivity, String username) {
        String targetActivityUid = targetActivity.getUid();

        List<Activity> metaActivityList = getByTargetActivityAndUsername(targetActivityUid, username, null, false);
        for (Activity metaActivity : metaActivityList) {
            if (ActivityEventType.CHEER.equals(metaActivity.getEventType())) {
                // short circuit
                return true;
            }
        }

        // no cheer found
        return false;
    }

    public boolean updateComment(Activity targetActivity, String newComment, String newCommentImgUrl, String addToCommentHistory, String addToCommentImgUrlHistory) {

        String targetActivityUid = targetActivity.getUid();

        UpdateItemSpec updateItemSpec = new UpdateItemSpec();
        updateItemSpec.withPrimaryKey(HASH_KEY, targetActivityUid);

        if (newComment != null) {
            updateItemSpec.withAttributeUpdate(new AttributeUpdate(COMMENT).put(newComment));
            if (addToCommentHistory != null) {
                updateItemSpec.withAttributeUpdate(new AttributeUpdate(COMMENT_HISTORY).addElements(addToCommentHistory));
            }
        }

        if (newCommentImgUrl != null) {
            updateItemSpec.withAttributeUpdate(new AttributeUpdate(COMMENT_IMG_URL).put(newCommentImgUrl));
            if (addToCommentImgUrlHistory != null) {
                updateItemSpec.withAttributeUpdate(new AttributeUpdate(COMMENT_IMG_URL_HISTORY).addElements(addToCommentImgUrlHistory));
            }
        }

        boolean result;
        try {
            UpdateItemOutcome updateItemOutcome = this.table.updateItem(updateItemSpec);
            result = true;
        } catch (Exception e) {
            Logger.error("update - failed", e);
            result = false;
        }

        return result;
    }

    public boolean deleteComment(Activity targetActivity) {
        return softDelete(targetActivity);
    }

    private static Activity itemToActivity(Item item) {
        return new Activity(
            item.getString(UID),
            item.getLong(TS),
            ActivityEventType.valueOf(item.getString(EVENTTYPE)),
            item.getString(IDTYPE),
            item.getInt(SEQUENCE),
            (item.isPresent(USERNAME) ? item.getString(USERNAME) : null),
            (item.isPresent(TEAM_ID) ? item.getLong(TEAM_ID) : null),
            (item.isPresent(QUEST_ID) ? item.getLong(QUEST_ID) : null),
            (item.isPresent(TASK_ID) ? item.getLong(TASK_ID) : null),
            (item.isPresent(LAT) ? item.getDouble(LAT) : null),
            (item.isPresent(LON) ? item.getDouble(LON) : null),
            item.getString(MSG),
            (item.isPresent(COMMENT) ? item.getString(COMMENT) : null),
            (item.isPresent(COMMENT_IMG_URL) ? item.getString(COMMENT_IMG_URL) : null),
            (item.isPresent(TARGET_ACTIVITY_UID) ? item.getString(TARGET_ACTIVITY_UID) : null),
            (item.isPresent(CHEER_COUNT) ? item.getLong(CHEER_COUNT) : null),
            (item.isPresent(POSTAL_CODE) ? item.getString(POSTAL_CODE) : null),
            (item.isPresent(DELETED) ? item.getBoolean(DELETED) : null),
            (item.isPresent(QUANTITY) ? item.getDouble(QUANTITY) : null),
            (item.isPresent(UNIT) ? ActivityUnit.valueOf(item.getString(UNIT)) : null),
            (item.isPresent(TAG) ? item.getString(TAG) : null)
        );
    }
    
}
