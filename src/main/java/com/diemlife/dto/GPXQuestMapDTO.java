package com.diemlife.dto;

import java.util.List;

import com.diemlife.models.QuestMapRoute;

/**
 * DTO Gpx quest map
 * Created 27/11/2020
 *
 * @author SYushchenko
 */
public class GPXQuestMapDTO {

    private final QuestMapRoute questMapRoute;

    private final List<GPXQuestMapRoutePointsDTO> questMapRoutePoints;

    /**
     * Constructor with parameters
     *
     * @param questMapRoute       {@link QuestMapRoute}
     * @param questMapRoutePoints collection {@link GPXQuestMapRoutePointsDTO}
     */
    public GPXQuestMapDTO(final QuestMapRoute questMapRoute,
                          final List<GPXQuestMapRoutePointsDTO> questMapRoutePoints) {
        this.questMapRoute = questMapRoute;
        this.questMapRoutePoints = questMapRoutePoints;
    }

    public QuestMapRoute getQuestMapRoute() {
        return questMapRoute;
    }

    public List<GPXQuestMapRoutePointsDTO> getQuestMapRoutePoints() {
        return questMapRoutePoints;
    }
}
