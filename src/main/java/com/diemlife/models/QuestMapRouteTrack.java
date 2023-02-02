package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Entity QuestMapRouteTrack
 * Created 30/11/2020
 *
 * @author SYushchenko
 */
@Entity(name = "QuestMapRouteTrack")
@Table(name = "quest_map_route_track")
@Getter
@Setter
@NoArgsConstructor
public class QuestMapRouteTrack extends IdentifiedEntity {

    @JsonIgnore
    @Column(name = "quest_map_route_id", nullable = false)
    private Long questMapRouteId;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;
}
