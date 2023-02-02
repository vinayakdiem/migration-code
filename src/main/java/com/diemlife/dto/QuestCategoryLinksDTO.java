package com.diemlife.dto;

import static java.util.Collections.emptyList;
import static org.springframework.util.StringUtils.hasText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.diemlife.models.QuestCategory;
import com.diemlife.utils.URLUtils;

import lombok.Data;

@Data
public class QuestCategoryLinksDTO implements Serializable {

    private String category;
    private String link;

    public static List<QuestCategoryLinksDTO> toDTO(final QuestCategory questCategory, final String link) {
        List<QuestCategoryLinksDTO> dtos = new ArrayList<>();

        if (questCategory == null) {
            return emptyList();
        }

        List<String> categories = simplifyCategory(questCategory.getCategory());

        for (String cat : categories) {
            final QuestCategoryLinksDTO dto = new QuestCategoryLinksDTO();
            dto.setCategory(cat);
            dto.setLink(link + "/" + URLUtils.explorePageCategories(cat));
            dtos.add(dto);
        }

        return dtos;
    }

    private static List<String> simplifyCategory(final String category) {

        if (hasText(category) && category.contains("/")) {
            String[] categories = category.split("/");

            return new ArrayList<>(Arrays.asList(categories));
        } else {
            return new ArrayList<>(Collections.singletonList(category));
        }
    }

}
