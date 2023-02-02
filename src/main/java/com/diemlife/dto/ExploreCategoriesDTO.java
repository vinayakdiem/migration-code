package com.diemlife.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExploreCategoriesDTO {
    @JsonProperty("categories")
    private List<CategoryDTO> categories;

    public List<CategoryDTO> getCategories() {
        return categories;
    }
}
