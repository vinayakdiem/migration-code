package com.diemlife.dao;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.geo.GeoDataManager;
import com.amazonaws.geo.GeoDataManagerConfiguration;
import com.amazonaws.geo.model.GeoPoint;
import com.amazonaws.geo.model.PutPointRequest;
import com.amazonaws.geo.model.PutPointResult;
import com.amazonaws.geo.model.QueryRadiusRequest;
import com.amazonaws.geo.model.QueryRadiusResult;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryResult;

import com.typesafe.config.Config;

import play.Logger;

public class ActivityGeoDAO {
    
    private static class ActivityGeoLevelDAO extends DynamoDAO {

        private static final String HASH_KEY = "hashKey";
        private static final String RANGE_KEY = "rangeKey";

        // unique id
        private static final String UID = "uid";

        private GeoDataManagerConfiguration geoConfig;
        private GeoDataManager geoDataManager;
        private int level;

        public ActivityGeoLevelDAO(Config conf, int level) {
            super(conf, "activity_geo" + level);

            this.level = level;

            // Note: the create tool I used has default attribute names so we don't need anything fancy here
            this.geoConfig = new GeoDataManagerConfiguration(this._client, this.tableName).withHashKeyLength(level);

            this.geoDataManager = new GeoDataManager(this.geoConfig);
        }

        public boolean insert(String uid, double lat, double lon) {
            GeoPoint geoPoint = new GeoPoint(lat, lon);
            AttributeValue rangeKeyValue = new AttributeValue().withS(uid);
            PutPointRequest putPointRequest = new PutPointRequest(geoPoint, rangeKeyValue);

            try {
                PutPointResult putPointResult = geoDataManager.putPoint(putPointRequest);
            } catch (Exception e) {
                Logger.error("insert - unable to create geo index for activity uid: " + uid , e);
                return false;
            }

            return true;
        }

        public Set<String> getActivityByGeo(double lat, double lon, double radius, Long ts, Integer limit) {
            Set<String> result = new HashSet<String>();
            
            GeoPoint geoPoint = new GeoPoint(lat, lon);
            QueryRadiusRequest queryRadiusRequest = new QueryRadiusRequest(geoPoint, radius);

            List<QueryResult> queryResult;
            try {
                QueryRadiusResult queryRadiusResult = this.geoDataManager.queryRadius(queryRadiusRequest);
                queryResult = queryRadiusResult.getQueryResults();
            } catch (Exception e) {
                Logger.error("getActivityByGeo - unable to query for level: " + level, e);
                return result;
            }

            Iterator<QueryResult> queryResultIt = queryResult.iterator();
            while(queryResultIt.hasNext()) {
                QueryResult item = queryResultIt.next();
                List<Map<String, AttributeValue>> items = item.getItems();

                Iterator<Map<String, AttributeValue>> itemsIt = items.iterator();
                while (itemsIt.hasNext()) {
                    Map<String, AttributeValue> attrs = itemsIt.next();
                    result.add(attrs.get(RANGE_KEY).getS());
                }
            }

            return result;
        }
    }

    private ActivityGeoLevelDAO activityGeoLevel2Dao;
    private ActivityGeoLevelDAO activityGeoLevel5Dao;

    public ActivityGeoDAO(Config conf) {
        activityGeoLevel2Dao = new ActivityGeoLevelDAO(conf, 2);
        activityGeoLevel5Dao = new ActivityGeoLevelDAO(conf, 5);
    }

    public boolean insert(String uid, double lat, double lon) {
        return activityGeoLevel2Dao.insert(uid, lat, lon) && activityGeoLevel5Dao.insert(uid, lat, lon);
    }

    public Set<String> getActivityByGeo(int level, double lat, double lon, double radius, Long ts, Integer limit) {
        ActivityGeoLevelDAO geoDao;
        switch (level) {
            case 2:
                geoDao = activityGeoLevel2Dao;
                break;
            case 5:
                geoDao = activityGeoLevel5Dao;
                break;
            default:
                return new HashSet<String>();
        }

        return geoDao.getActivityByGeo(lat, lon, radius, ts, limit);
    }
}
