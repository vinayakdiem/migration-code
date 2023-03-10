package com.diemlife.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.diemlife.dao.ExplorePlacesDAO;
import com.diemlife.dao.QuestTasksDAO;
import com.diemlife.dto.ReverseGeocodingDTO;
import com.diemlife.dto.ReverseGeocodingFeatureDTO;
import com.diemlife.forms.QuestActionPointForm;
import java.io.IOException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.diemlife.models.ExplorePlaces;
import com.diemlife.models.Quests;
import play.Logger;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;


import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class ReverseGeocodingService {

	@Autowired
    private Config config;
	
	@Autowired
    private WSClient wsClient;
	
	@Autowired
    private String mapboxBaseUrl = config.getString("mapbox.baseUrl");
	
	@Autowired
    private String mapboxAccessToken = config.getString("mapbox.placesGeocodingEndpoint");
	
	@Autowired
    private String mapboxPlacesEndoint = config.getString("mapbox.accessKey");

    public ReverseGeocodingDTO getQuestCityByGeopoint(final QuestActionPointForm form, final Quests quest) {

        Float latitude = form.getLatitude();
        Float longitude = form.getLongitude();
        String comma = ",";
        String jsonExtension = ".json";
        String tokenQueryParamName = "?access_token=";

        String url = mapboxBaseUrl + mapboxPlacesEndoint + longitude + comma + latitude + jsonExtension + tokenQueryParamName + mapboxAccessToken;

        final CompletionStage<String> verifyResponse = wsClient
                .url(url)
                .get()
                .thenApply(WSResponse::getBody);

        String jsonData = verifyResponse.toCompletableFuture().join();
        ObjectMapper objectMapper = new ObjectMapper();
        ReverseGeocodingDTO reverseGeocodingDTO = null;

        try {
            boolean hasPlace = false;
            reverseGeocodingDTO = objectMapper.readValue(jsonData, ReverseGeocodingDTO.class);
            for (ReverseGeocodingFeatureDTO dto : reverseGeocodingDTO.features) {
                if (dto.placeType.contains("place")) {
                    hasPlace = true;
                    try {
                        quest.setPlace(dto.placeName);
                        quest.setPoint(QuestTasksDAO.buildGeoPoint(form));
                        //FIXME vinayak
//                        em.merge(quest);

                        if (!ExplorePlacesDAO.doesPlaceExist(dto.placeName)) {

                            ExplorePlaces exploredPlaces = new ExplorePlaces();
                            exploredPlaces.setPlace(dto.placeName);
                            exploredPlaces.setIncluded(true);
                            exploredPlaces.setOrder(0);

                            em.persist(exploredPlaces);

                        }
                    } catch (Exception ex) {
                        Logger.error("ReverseGeocodingService :: getQuestCityByGeopoint :: Error => " + ex, ex);
                    }
                    break;
                }
            }
            if (!hasPlace) {
                Logger.error("ReverseGeocodingService :: getQuestCityByGeopoint :: MapBox response doesn't contain 'place' value");
                return null;
            }
        } catch (IOException e) {
            Logger.info("ReverseGeocodingService :: getQuestCityByGeopoint : error get city by geopoint: ", e);
        }
        return reverseGeocodingDTO;
    }

}
