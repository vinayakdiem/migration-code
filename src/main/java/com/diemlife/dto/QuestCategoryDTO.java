package com.diemlife.dto;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.diemlife.models.QuestCategory;

import lombok.Getter;

public class QuestCategoryDTO implements Serializable {

    public Long id;
    public Integer questId;
    public String category;
    public float confidence;
    @Getter
    public List<QuestCategoryLinksDTO> links;

    public static QuestCategoryDTO toDTO(final QuestCategory category, String link) {
        if (category == null) {
            return null;
        }
        final QuestCategoryDTO dto = new QuestCategoryDTO();
        dto.id = category.id;
        dto.questId = category.getQuestId();
        dto.category = category.getCategory();
        dto.confidence = category.getConfidence();
        dto.links = QuestCategoryLinksDTO.toDTO(category, link);

        return dto;
    }

    public static List<QuestCategoryDTO> listToDTO(final List<QuestCategory> categories, final String link) {
        if (isEmpty(categories)) {
            return emptyList();
        }
        return categories.stream().map(c -> toDTO(c, link)).filter(Objects::nonNull).collect(toList());
    }
}
