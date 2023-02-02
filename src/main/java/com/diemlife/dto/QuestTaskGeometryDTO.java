package com.diemlife.dto;


import com.vividsolutions.jts.geom.Geometry;

public class QuestTaskGeometryDTO {
    private final Integer questTaskId;
    private final Geometry geometry;

    public QuestTaskGeometryDTO(Integer questTaskId, Geometry geometry) {
        this.questTaskId = questTaskId;
        this.geometry = geometry;
    }

    public Integer getQuestTaskId() {
        return questTaskId;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}
