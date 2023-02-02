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
public class ReverseGeocodingGeometryDTO {

    @JsonProperty("type")
    public String type;
    @JsonProperty("coordinates")
    public List<Double> coordinates = null;
}
