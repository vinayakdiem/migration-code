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
public class ReverseGeocodingDTO {

    @JsonProperty("type")
    public String type;
    @JsonProperty("query")
    public List<Double> query = null;
    @JsonProperty("features")
    public List<ReverseGeocodingFeatureDTO> features = null;
    @JsonProperty("attribution")
    public String attribution;

}
