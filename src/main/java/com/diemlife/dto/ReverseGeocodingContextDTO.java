package com.diemlife.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReverseGeocodingContextDTO {

    @JsonProperty("id")
    public String id;
    @JsonProperty("text")
    public String text;
    @JsonProperty("wikidata")
    public String wikidata;
    @JsonProperty("short_code")
    public String shortCode;
}
