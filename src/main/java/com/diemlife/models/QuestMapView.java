package com.diemlife.models;

import com.bedatadriven.jackson.datatype.jts.serialization.GeometryDeserializer;
import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import com.vividsolutions.jts.geom.Point;

/**
 * Entity QuestMapView
 * Created 03/11/2020
 *
 * @author SYushchenko
 */
@Entity(name = "QuestMapView")
@Table(name = "quest_map_view")
@Getter
@Setter
@NoArgsConstructor
public class QuestMapView  {

    @Column(name = "id", nullable = false, unique = true)
    @Id
    private Integer id;

    @Column(name = "map_name")
    private String mapName;

    @Column(name = "map_pins_disabled")
    private boolean mapPinsDisabled = false;

    @Column(name = "geo_point")
    @JsonProperty
    @JsonSerialize(using = GeometrySerializer.class)
    @JsonDeserialize(using = GeometryDeserializer.class)
    private Point point;

    @Column(name = "geo_zoom")
    private Float geoZoom;

    @Column(name = "geo_point_radius_km")
    private Float geoRadiusInKm;

    @Column(name = "geo_point_icon")
    private String geoPointIcon;

    @Column(name = "geo_point_completed_icon")
    private String geoCompletedIcon;

    @Column(name = "geo_point_member_route_icon")
    private String geoMemberRouteIcon;

    @Column(name = "geo_point_team_route_icon")
    private String geoTeamRouteIcon;

    @Column(name = "geo_point_start_icon")
    private String geoStartIcon;

    @Column(name = "geo_point_end_icon")
    private String geoEndIcon;

    @Column(name = "geo_point_food_icon")
    private String geoFoodIcon;

    @Column(name = "geo_point_donation_icon")
    private String geoDonationIcon;

    @Column(name = "geo_point_firstaid_icon")
    private String geoFirstaidIcon;

    @Column(name = "geo_point_counter_icon")
    private String geoCounterIcon;

}
