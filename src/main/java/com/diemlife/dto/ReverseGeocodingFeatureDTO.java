package com.diemlife.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReverseGeocodingFeatureDTO {

    @JsonProperty("id")
    public String id;
    @JsonProperty("type")
    public String type;
    @JsonProperty("place_type")
    public List<String> placeType = null;
    @JsonProperty("relevance")
    public Integer relevance;
    @JsonProperty("properties")
    public ReverseGeocodingPropertiesDTO properties;
    @JsonProperty("text")
    public String text;
    @JsonProperty("place_name")
    public String placeName;
    @JsonProperty("center")
    public List<Double> center = null;
    @JsonProperty("geometry")
    public ReverseGeocodingGeometryDTO geometry;
    @JsonProperty("address")
    public String address;
    @JsonProperty("context")
    public List<ReverseGeocodingContextDTO> context = null;
    @JsonProperty("bbox")
    public List<Double> bbox = null;
}
