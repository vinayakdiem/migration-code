package com.diemlife.services;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.diemlife.dao.QuestMapRouteDAO;
import com.diemlife.dao.QuestMapRoutePointDAO;
import com.diemlife.dao.QuestMapRouteSegmentDAO;
import com.diemlife.dao.QuestMapRouteTrackDAO;
import com.diemlife.dao.QuestMapRouteWaypointDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dto.GPXQuestMapDTO;
import com.diemlife.dto.GPXQuestMapRoutePointsDTO;
import lombok.Getter;
import me.himanshusoni.gpxparser.GPXParser;
import me.himanshusoni.gpxparser.modal.GPX;
import me.himanshusoni.gpxparser.modal.Track;
import me.himanshusoni.gpxparser.modal.TrackSegment;
import me.himanshusoni.gpxparser.modal.Waypoint;
import com.diemlife.models.QuestMapRoute;
import com.diemlife.models.QuestMapRoutePoints;
import com.diemlife.models.QuestMapRouteSegment;
import com.diemlife.models.QuestMapRouteTrack;
import com.diemlife.models.QuestMapRouteWaypoint;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import com.diemlife.services.maproute.WaypointQuestMapRouteService;
import com.diemlife.utils.QuestSecurityUtils;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toMap;

/**
 * Service for working with map route
 * Created 25/11/2020
 *
 * @author SYushchenko
 */
@Service
public class QuestMapRouteService {

    private static final String FORM_NAME = "name";

    private static final String FORM_DESCRIPTION = "description";

    @Autowired
    private WaypointQuestMapRouteService waypointQuestMapRouteService;

    /**
     * Upload file gpx to data base
     *
     * @param gpxFile  {@link Http.MultipartFormData.FilePart}
     * @param formData for data from request
     * @param questId  questId
     * @return {@link Result}
     */
    public Result uploadGPXFile(final Http.MultipartFormData.FilePart<File> gpxFile,
                                final Map<String, String[]> formData,
                                final Integer questId) {
        if (validateParameters(gpxFile, formData, questId)) {
            return Results.badRequest();
        }
        processingGpxFile(gpxFile.getFile(), formData, questId);
        return Results.ok();
    }

    /**
     * Get Quest map route by quest id
     *
     * @param questId quest id
     * @return collection {@link GPXQuestMapDTO}
     */
    public Result getQuestMapRoutesByQuest(Integer questId) {
        Quests quests = QuestsDAO.findById(questId);
        if (quests == null) {
            return Results.notFound();
        }
        final QuestMapRouteWaypointDAO waypointDao = new QuestMapRouteWaypointDAO();

        Map<Long, QuestMapRoute> questMapRoutes =
                new QuestMapRouteDAO().findAllQuestMapRoutesByQuest(quests.getId())
                        .stream()
                        .peek(map -> map.setDistance(waypointDao.findAllQuestMapRouteWaypoint(map.id).stream()
                                .map(QuestMapRouteWaypoint::getDistance)
                                .mapToLong(value -> value == null ? 0 : value.longValue())
                                .max()
                                .orElse(0)))
                        .collect(toMap(l -> l.id, k -> k));
        Map<Long, List<GPXQuestMapRoutePointsDTO>> points =
                new QuestMapRoutePointDAO()
                        .findAllQuestMapRoutesPointByQuestMapRouteId(questMapRoutes.keySet());
        return mappingQuestMapRouteToResult(questMapRoutes, points);
    }

    /**
     * Disable quest map route by quest id if exist
     *
     * @param questId quest id
     * @param user    {@link User}
     * @return {@link Result}
     */
    public Result toggleAllQuestMapRoute(Integer questId, User user, Boolean flag) {
        Quests quests = QuestsDAO.findById(questId);
        if (quests == null) {
            return Results.notFound();
        }
        if (!QuestSecurityUtils.canEditQuest(quests, user)) {
            return Results.forbidden();
        }
        new QuestMapRouteDAO().toggleAllQuestMapRoute(questId, flag);
        return Results.ok();
    }

    public Result toggleStatusQuestMapRoute(Integer questId) {
        Quests quests = QuestsDAO.findById(questId);
        if (quests == null) {
            return Results.notFound();
        }
        Boolean aBoolean = new QuestMapRouteDAO().toggleStatusQuestMapRoute(questId);
        return Results.ok(Json.toJson(aBoolean));
    }

    private Result mappingQuestMapRouteToResult(final Map<Long, QuestMapRoute> questMapRoutes,
                                                final Map<Long, List<GPXQuestMapRoutePointsDTO>> points) {
        List<GPXQuestMapDTO> gpxQuestMapDTOS = new ArrayList<>();
        points.forEach((key, value) -> gpxQuestMapDTOS.add(new GPXQuestMapDTO(questMapRoutes.get(key), value)));
        return Results.ok(Json.toJson(gpxQuestMapDTOS));
    }

    private void processingGpxFile(final File gpxFile,
                                   final Map<String, String[]> formData,
                                   final Integer questId) {
        Logger.info("Start upload file, questId: " + questId + " file name: " + gpxFile.getName());
        try {
            final GPX gpx = loadGpx(gpxFile);
            if (gpx != null) {
                final GpxLogger gpxLogger = new GpxLogger();
                jpaApi.withTransaction(entityManager -> {
                    final AtomicInteger countTrack = new AtomicInteger(1);
                    QuestMapRouteDAO questMapRouteDAO = new QuestMapRouteDAO(entityManager);
                    questMapRouteDAO.toggleAllQuestMapRoute(questId, false);
                    final QuestMapRoute questMapRoute = questMapRouteDAO.save(createQuestMapRoute(formData, questId), QuestMapRoute.class);

                    Logger.info("Quest map route saved with ID " + questMapRoute.getId());

                    waypointQuestMapRouteService.processingWptQuestMapRoute(gpx, questId, questMapRoute, entityManager);

                    Logger.info("Waypoints saved for Quest map route with ID " + questMapRoute.getId());

                    gpxLogger.incrementQuestMapRoute();

                    gpx.getTracks().forEach(track -> {
                        Logger.info("Processing track " + track.getName());

                        processingTrack(track, questMapRoute, countTrack, entityManager, gpxLogger);
                    });

                    return entityManager;
                });
                Logger.info("Successfully processed " + gpxLogger.getQuestMapRoutePoint()
                        + " points in " + gpxLogger.getQuestMapRouteSegment()
                        + " segments of " + gpxLogger.getQuestMapRouteTrack()
                        + " tracks of file " + gpxFile.getName());
            }
        } catch (final Exception exception) {
            Logger.error("Processing upload gpx file fail! questId: " + questId + " fileName: " + gpxFile.getName(), exception);
        }
    }

    private GPX loadGpx(final File gpxFile) {
        try (final FileInputStream fileInputStream = new FileInputStream(gpxFile)) {
            return new GPXParser().parseGPX(fileInputStream);
        } catch (final Exception e) {
            Logger.error("Failed to load GPX file: " + gpxFile.getName(), e);

            return null;
        }
    }

    private void processingTrack(final Track track,
                                 final QuestMapRoute questMapRoute,
                                 final AtomicInteger countTrack,
                                 final GpxLogger gpxLogger) {
        QuestMapRouteTrack questMapRouteTrack =
                new QuestMapRouteTrackDAO()
                        .save(createQuestMapRouteTrack(track, questMapRoute, countTrack), QuestMapRouteTrack.class);

        Logger.info("Track saved with ID " + questMapRouteTrack.getId());

        gpxLogger.incrementQuestMapRouteTrack();
        final AtomicInteger countSegment = new AtomicInteger(1);
        track.getTrackSegments().forEach(l -> processingSegment(l, questMapRouteTrack.id, countSegment, entityManager, gpxLogger));
    }

    private void processingSegment(final TrackSegment trackSegment,
                                   final Long questMapRouteTrackId,
                                   final AtomicInteger countSegment,
                                   final GpxLogger gpxLogger) {

        QuestMapRouteSegment questMapRouteSegmentSaved =
                new QuestMapRouteSegmentDAO()
                        .save(createQuestMapRouteSegment(questMapRouteTrackId, countSegment), QuestMapRouteSegment.class);

        Logger.info("Track segment saved with ID " + questMapRouteSegmentSaved.getId());

        gpxLogger.incrementQuestMapRouteSegment();
        final AtomicInteger countPoint = new AtomicInteger(1);
        trackSegment.getWaypoints().forEach(l -> {
            final QuestMapRoutePoints point = createQuestMapRoutePoint(l, countPoint, questMapRouteSegmentSaved.id);

            entityManager.persist(point);

            Logger.debug("Track point saved with ID " + point.getId());

            gpxLogger.incrementQuestMapRoutePoint();
        });
    }

    private QuestMapRoutePoints createQuestMapRoutePoint(final Waypoint waypoint,
                                                         final AtomicInteger countPoint,
                                                         final Long questMapRouteSegmentId) {
        QuestMapRoutePoints questMapRoutePoints = new QuestMapRoutePoints();
        questMapRoutePoints.setQuestMapRouteSegmentId(questMapRouteSegmentId);
        questMapRoutePoints.setSequence(countPoint.getAndIncrement());

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
        Coordinate coordinate = new Coordinate(waypoint.getLatitude(), waypoint.getLongitude());
        questMapRoutePoints.setGeoPoint(geometryFactory.createPoint(coordinate));
        questMapRoutePoints.setAltitude(Double.valueOf(waypoint.getElevation()).floatValue());
        return questMapRoutePoints;
    }

    private QuestMapRouteSegment createQuestMapRouteSegment(final Long questMapRouteTrackId,
                                                            final AtomicInteger countSegment) {
        QuestMapRouteSegment questMapRouteSegment = new QuestMapRouteSegment();
        questMapRouteSegment.setSequence(countSegment.getAndIncrement());
        questMapRouteSegment.setQuestMapRouteTrackId(questMapRouteTrackId);
        return questMapRouteSegment;
    }

    private QuestMapRouteTrack createQuestMapRouteTrack(Track track,
                                                        QuestMapRoute questMapRoute,
                                                        AtomicInteger countTrack) {
        QuestMapRouteTrack questMapRouteTrack = new QuestMapRouteTrack();
        questMapRouteTrack.setName(track.getName());
        questMapRouteTrack.setDescription(track.getDescription());
        questMapRouteTrack.setSequence(countTrack.get());
        questMapRouteTrack.setQuestMapRouteId(questMapRoute.id);
        return questMapRouteTrack;
    }

    private QuestMapRoute createQuestMapRoute(Map<String, String[]> formData, Integer questId) {
        QuestMapRoute questMapRoute = new QuestMapRoute();
        questMapRoute.setQuestId(questId);
        questMapRoute.setName(formData.get(FORM_NAME) != null ? formData.get(FORM_NAME)[0] : null);
        questMapRoute.setDescription(formData.get(FORM_DESCRIPTION) != null ? formData.get(FORM_DESCRIPTION)[0] : null);
        questMapRoute.setActive(true);
        return questMapRoute;
    }

    private boolean validateParameters(final Http.MultipartFormData.FilePart<File> gpxFile,
                                       final Map<String, String[]> formData,
                                       final Integer questId) {
        return gpxFile == null || formData == null || questId == null;
    }

    @Getter
    private static class GpxLogger {
        private int questMapRoute;
        private int questMapRouteTrack;
        private int questMapRouteSegment;
        private int questMapRoutePoint;

        public void incrementQuestMapRoute() {
            this.questMapRoute++;
        }

        public void incrementQuestMapRouteTrack() {
            this.questMapRouteTrack++;
        }

        public void incrementQuestMapRouteSegment() {
            this.questMapRouteSegment++;
        }

        public void incrementQuestMapRoutePoint() {
            this.questMapRoutePoint++;
        }
    }

}
