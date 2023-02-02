package com.diemlife.services.maproute;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.diemlife.dao.QuestMapRouteWaypointDAO;
import com.diemlife.dao.QuestTasksDAO;
import com.diemlife.dao.QuestTasksGroupDAO;
import com.diemlife.dao.QuestsDAO;
import me.himanshusoni.gpxparser.modal.GPX;
import me.himanshusoni.gpxparser.modal.Waypoint;
import com.diemlife.models.QuestMapRoute;
import com.diemlife.models.QuestMapRouteWaypoint;
import com.diemlife.models.QuestTasks;
import com.diemlife.models.QuestTasksGroup;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class WaypointQuestMapRouteService {

    private static final String GROUP_NAME = "Waypoints";

    private QuestMapRouteWaypointDAO questMapRouteWaypointDAO;

    /**
     * Waypoints handling, saving to table and create quest tasks
     *
     * @param gpxFile       {@link GPX}
     * @param questId       questId
     * @param questMapRoute {@link QuestMapRoute}
     */
    public void processingWptQuestMapRoute(final GPX gpxFile, final Integer questId, final QuestMapRoute questMapRoute, final EntityManager entityManager) {
        this.questMapRouteWaypointDAO = new QuestMapRouteWaypointDAO(entityManager);
        if (isNotEmpty(gpxFile.getWaypoints())) {
            final List<QuestMapRouteWaypoint> questMapRouteWaypoints = parsingGpxFile(gpxFile, questMapRoute);

            final List<QuestTasks> questTasks = QuestTasksDAO.getQuestTasksByQuestIdAndWaypointIsNotNull(questId, null, entityManager);
            if (isNotEmpty(questTasks)) {
                updateWaypointByGpxAndSave(questId, questMapRoute, questTasks, questMapRouteWaypoints, entityManager);
            } else {
                saveWaypointByGpx(questMapRouteWaypoints, questMapRoute, questId, entityManager);
            }
        }
    }

    private void saveWaypointByGpx(final List<QuestMapRouteWaypoint> questMapRouteWaypoints,
                                   final QuestMapRoute questMapRoute,
                                   final Integer questId,
                                   final EntityManager entityManager) {
        questMapRouteWaypointDAO.saveAll(questMapRouteWaypoints);

        List<QuestMapRouteWaypoint> questMapRouteWaypointSaved =
                questMapRouteWaypointDAO.findAllQuestMapRouteWaypoint(questMapRoute.id);

        Quests quests = QuestsDAO.findById(questId, entityManager);

        createQuestTasks(questId, quests, questMapRouteWaypointSaved, entityManager);
    }

    private void updateWaypointByGpxAndSave(final Integer questId,
                                            final QuestMapRoute questMapRoute,
                                            final List<QuestTasks> questTasks,
                                            final List<QuestMapRouteWaypoint> questMapRouteWaypoints,
                                            final EntityManager entityManager) {
        final List<QuestTasks> needToUpdate = new ArrayList<>();

        final List<QuestMapRouteWaypoint> needToCreate = new ArrayList<>();

        questMapRouteWaypointDAO.saveAll(questMapRouteWaypoints);

        List<QuestMapRouteWaypoint> questMapRouteWaypointSaved =
                questMapRouteWaypointDAO.findAllQuestMapRouteWaypoint(questMapRoute.id);

        final Map<Point, QuestMapRouteWaypoint> questMapRouteWaypointMap = getMapRouteWaypointMap(questMapRouteWaypointSaved);

        final Map<Point, QuestTasks> questTasksMap = getMapRouteQuestTasksMap(questTasks, entityManager);

        questMapRouteWaypointMap.forEach((key, value) -> {
            Point point = getEqualsPoint(questTasksMap.keySet(), key);
            if (point != null) {
                QuestTasks task = questTasksMap.get(point);
                task.setTask(value.getName());
                task.setQuestMapRouteWaypointId(value.getId());
                needToUpdate.add(task);
            } else {
                needToCreate.add(value);
            }
        });

        needToUpdate.forEach(entityManager::merge);

        Quests quests = QuestsDAO.findById(questId, entityManager);

        createQuestTasks(questId, quests,  needToCreate, entityManager);
    }

    private Map<Point, QuestTasks> getMapRouteQuestTasksMap(final List<QuestTasks> questTasks, final EntityManager entityManager) {
        final Map<Point, QuestTasks> questTasksResultMap = new HashMap<>();

        final List<Long> questMapRouteWaypointIds =
                questTasks.stream().map(QuestTasks::getQuestMapRouteWaypointId).collect(Collectors.toList());

        final Map<Integer, QuestTasks> mappingQuestTasksById = questTasks.stream().collect(toMap(QuestTasks::getId, k -> k));

        QuestTasksDAO.findAllGeometryByQuestWaypointId(questMapRouteWaypointIds, entityManager).forEach(l -> {
            QuestTasks task = mappingQuestTasksById.get(l.getQuestTaskId());
            questTasksResultMap.put(new GeometryFactory().createPoint(l.getGeometry().getCoordinate()), task);
        });
        return questTasksResultMap;
    }

    private Map<Point, QuestMapRouteWaypoint> getMapRouteWaypointMap(final List<QuestMapRouteWaypoint> questMapRouteWaypoints) {
        return questMapRouteWaypoints.stream().collect(toMap(QuestMapRouteWaypoint::getPoint, k -> k));
    }

    private void createQuestTasks(final Integer questId,
                                  final Quests quest,
                                  final List<QuestMapRouteWaypoint> questMapRouteWaypoints,
                                  final EntityManager entityManager) {
        QuestTasksGroup questTasksGroup = QuestTasksGroupDAO.addNewTasksGroup(quest.getUser(), quest, GROUP_NAME, entityManager);
        final int lastOrder = QuestTasksDAO.getLastTaskOrder(quest.getId(), questId, null, entityManager);
        AtomicInteger orderTasks = new AtomicInteger(lastOrder);
        questMapRouteWaypoints.forEach(l -> {
            QuestTasks questTask = creatQuestTasks(l, questId, quest.getUser(), questTasksGroup, orderTasks);
            entityManager.persist(questTask);
        });
        entityManager.flush();
    }

    private List<QuestMapRouteWaypoint> parsingGpxFile(final GPX gpxFile, final QuestMapRoute questMapRoute) {
        AtomicInteger countAtomicInteger = new AtomicInteger(1);
        return gpxFile.getWaypoints()
                .stream()
                .map(l -> mappingQuestMapRouteWaypoint(l, questMapRoute, countAtomicInteger))
                .collect(Collectors.toList());
    }

    private QuestMapRouteWaypoint mappingQuestMapRouteWaypoint(final Waypoint waypoint,
                                                               final QuestMapRoute questMapRoute,
                                                               final AtomicInteger counter) {
        QuestMapRouteWaypoint questMapRouteWaypoint = new QuestMapRouteWaypoint();
        questMapRouteWaypoint.setQuestMapRouteId(questMapRoute.id);
        questMapRouteWaypoint.setName(waypoint.getName());
        questMapRouteWaypoint.setDescription(waypoint.getDescription());
        questMapRouteWaypoint.setSequence(counter.getAndIncrement());

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
        Coordinate coordinate = new Coordinate(waypoint.getLatitude(), waypoint.getLongitude());
        questMapRouteWaypoint.setPoint(geometryFactory.createPoint(coordinate));
        return questMapRouteWaypoint;
    }

    private QuestTasks creatQuestTasks(final QuestMapRouteWaypoint questMapRouteWaypoint,
                                       final Integer questId,
                                       final User user,
                                       final QuestTasksGroup questTasksGroup,
                                       final AtomicInteger orderTasks) {
        final Date now = new Date();

        QuestTasks questTasks = new QuestTasks();
        questTasks.setQuestId(questId);
        questTasks.setUserId(user.getId());
        questTasks.setTask(questMapRouteWaypoint.getName());
        questTasks.setTaskCompleted(Boolean.FALSE.toString().toUpperCase());
        questTasks.setCreatedDate(now);
        questTasks.setCreatedBy(user.getId());
        questTasks.setLastModifiedDate(now);
        questTasks.setLastModifiedBy(user.getId());
        questTasks.setQuestTasksGroup(questTasksGroup);
        questTasks.setOrder(orderTasks.incrementAndGet());
        questTasks.setQuestMapRouteWaypointId(questMapRouteWaypoint.id);

        return questTasks;
    }

    public Point getEqualsPoint(final Set<Point> pointKeys, final Point point) {
        for (Point pointKey: pointKeys) {
            if (pointKey.equalsExact(point)) {
                return pointKey;
            }
        }
        return null;
    }
}
