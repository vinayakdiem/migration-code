package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import com.vividsolutions.jts.geom.Point;

@Entity(name = "QuestMapRouteWaypoint")
@Table(name = "quest_map_route_waypoint")
@Getter
@Setter
@NoArgsConstructor
public class QuestMapRouteWaypoint extends IdentifiedEntity {

    @Column(name = "quest_map_route_id", nullable = false)
    private Long questMapRouteId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "geo_point", nullable = false)
    private Point point;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "distance")
    private Integer distance;
}
