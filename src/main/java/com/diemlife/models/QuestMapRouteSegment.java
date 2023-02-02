package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Entity QuestMapRouteSegment
 * Created 30/11/2020
 *
 * @author SYushchenko
 */
@Entity(name = "QuestMapRouteSegment")
@Table(name = "quest_map_route_segment")
@Getter
@Setter
@NoArgsConstructor
public class QuestMapRouteSegment extends IdentifiedEntity {

    @Column(name = "quest_map_route_track_id", nullable = false)
    private Long questMapRouteTrackId;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;
}
