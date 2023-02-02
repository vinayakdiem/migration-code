package com.diemlife.dto;

import java.io.Serializable;
import java.util.List;

import com.diemlife.models.ExploreCategories;
import com.diemlife.models.ExplorePlaces;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExploreDTO implements Serializable {

    public List<ExploreCategories> exploreCategories;
    public List<ExplorePlaces> explorePlaces;
    public List<String> explorePillars;

}
