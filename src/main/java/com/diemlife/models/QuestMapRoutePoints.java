package com.diemlife.models;

import com.vividsolutions.jts.geom.Point;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Entity QuestMapRoutePoints
 * Created 25/11/2020
 *
 * @author SYushchenko
 */
@Entity(name = "QuestMapRoutePoints")
@Table(name = "quest_map_route_points")
@Getter
@Setter
@NoArgsConstructor
public class QuestMapRoutePoints extends IdentifiedEntity{

    @Column(name = "quest_map_route_segment_id", nullable = false)
    private Long questMapRouteSegmentId;

    @Column(name = "geo_point")
    private Point geoPoint;

    @Column(name = "altitude")
    private Float altitude;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;
}
