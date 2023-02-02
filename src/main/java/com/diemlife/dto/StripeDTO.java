package com.diemlife.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;

public abstract class StripeDTO implements Serializable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(SNAKE_CASE)
            .setSerializationInclusion(NON_NULL);

    public Map<String, Object> metadata = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        try {
            return OBJECT_MAPPER.readValue(toJson(), Map.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
