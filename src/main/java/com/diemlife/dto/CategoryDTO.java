package com.diemlife.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryDTO {
    @JsonProperty("title")
    private String title;
    @JsonProperty("isActive")
    private boolean isActive;

    public boolean getIsActive() {
        return isActive;
    }

    public String getTitle() {
        return title;
    }
}
