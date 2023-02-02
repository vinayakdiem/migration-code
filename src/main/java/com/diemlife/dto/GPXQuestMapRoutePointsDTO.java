package com.diemlife.dto;

import com.bedatadriven.jackson.datatype.jts.serialization.GeometryDeserializer;
import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.*;
import lombok.Getter;

/**
 * DTO GPXQuestMapRoutePointsDTO
 * Created 30/11/2020
 *
 * @author SYushchenko
 */
@Getter
public class GPXQuestMapRoutePointsDTO {
    @JsonIgnore
    private final Long questMapRouteId;
    private final Long questMapRoutePointId;
    @JsonProperty
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private final Point point;
    private final Integer segment;
    private final QuestMapRouteTrackDTO track;
    private final Float altitude;
    private final Integer sequence;

    /**
     * Constructor with parameters
     *
     * @param questMapRouteId      questMapRouteId
     * @param questMapRoutePointId questMapRoutePointId
     * @param geometry             {@link Geometry}
     * @param segment              segment
     * @param name                 track name
     * @param description          track description
     * @param altitude             altitude
     * @param sequence             sequence
     */
    public GPXQuestMapRoutePointsDTO(Long questMapRouteId,
                                     Long questMapRoutePointId,
                                     Geometry geometry,
                                     Integer segment,
                                     String name,
                                     String description,
                                     Float altitude,
                                     Integer sequence) {
        this.questMapRouteId = questMapRouteId;
        this.questMapRoutePointId = questMapRoutePointId;

        Coordinate coordinate = new Coordinate(geometry.getCoordinate().x, geometry.getCoordinate().y);
        this.point = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING)).createPoint(coordinate);
        this.segment = segment;
        this.track = new QuestMapRouteTrackDTO(name, description);
        this.altitude = altitude;
        this.sequence = sequence;
    }

    /**
     * DTO QuestMapRouteTrackDTO
     */
    private static class QuestMapRouteTrackDTO {
        private final String name;
        private final String description;

        /**
         * Constructor with parameters
         *
         * @param name        name
         * @param description description
         */
        public QuestMapRouteTrackDTO(final String name,
                                     final String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}

