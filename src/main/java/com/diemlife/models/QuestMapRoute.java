package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Entity QuestMapRoute
 * Created 25/11/2020
 *
 * @author SYushchenko
 */
@Entity(name = "QuestMapRoute")
@Table(name = "quest_map_route")
@Getter
@Setter
@NoArgsConstructor
public class QuestMapRoute extends IdentifiedEntity {

    @Column(name = "quest_id", nullable = false)
    private Integer questId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @JsonIgnore
    @Column(name = "active")
    private Boolean active;

    @Transient
    private Long distance;

}
