package dao;

import dto.GPXQuestMapRoutePointsDTO;
import models.QuestMapRoutePoints;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Quest map route point DAO
 * Created 25/11/2002
 *
 * @author SYushchenko
 */
public class QuestMapRoutePointDAO extends TypedDAO<QuestMapRoutePoints> {

    /**
     * Constructor with parameters
     *
     * @param entityManager {@link EntityManager}
     */
    public QuestMapRoutePointDAO(EntityManager entityManager) {
        super(entityManager);
    }

    public Map<Long, List<GPXQuestMapRoutePointsDTO>> findAllQuestMapRoutesPointByQuestMapRouteId(Set<Long> questMapRoutesId) {
        if (questMapRoutesId.isEmpty()) {
            return Collections.emptyMap();
        }
        List<GPXQuestMapRoutePointsDTO> points =
                Stream.of(entityManager.createQuery("SELECT " +
                        " NEW dto.GPXQuestMapRoutePointsDTO ( " +
                        " qmr.id, " +
                        " qmrp.id, " +
                        " qmrp.geoPoint, " +
                        " qmrs.sequence, " +
                        " qmrt.name," +
                        " qmrt.description," +
                        " qmrp.altitude, " +
                        " qmrp.sequence ) " +
                        " FROM QuestMapRoute qmr " +
                        " JOIN QuestMapRouteTrack qmrt ON qmr.id = qmrt.questMapRouteId " +
                        " JOIN QuestMapRouteSegment qmrs ON qmrt.id = qmrs.questMapRouteTrackId " +
                        " JOIN QuestMapRoutePoints qmrp ON qmrs.id = qmrp.questMapRouteSegmentId " +
                        " WHERE qmr.id IN (:questId) AND qmr.active = TRUE ")
                        .setParameter("questId", questMapRoutesId)
                        .getResultList()
                        .toArray())
                        .map(GPXQuestMapRoutePointsDTO.class::cast)
                        .collect(Collectors.toList());

        return mappingPointByQuestMapRoute(points, questMapRoutesId);
    }

    private Map<Long, List<GPXQuestMapRoutePointsDTO>> mappingPointByQuestMapRoute(final List<GPXQuestMapRoutePointsDTO> points, final Set<Long> questMapRoutesIds) {
        final Map<Long, List<GPXQuestMapRoutePointsDTO>> pointsMap = new HashMap<>();
        for (final Long routeId : questMapRoutesIds) {
            if (routeId != null) {
                pointsMap.put(routeId, new ArrayList<>());
                for (final GPXQuestMapRoutePointsDTO point : points) {
                    if (routeId.equals(point.getQuestMapRouteId())) {
                        pointsMap.get(routeId).add(point);
                    }
                }
            }
        }
        return pointsMap;
    }
}
