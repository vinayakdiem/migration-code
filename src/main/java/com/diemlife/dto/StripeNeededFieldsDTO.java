package com.diemlife.dto;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.Objects;

import com.diemlife.constants.StripeRequiredFields;
import com.diemlife.constants.Util;

import lombok.Data;

@Data
public class StripeNeededFieldsDTO {

    private final StripeRequiredFields stripeRequiredFields;
    private final int totalCount;

    public static StripeNeededFieldsDTO toDTO(final StripeRequiredFields requiredFields, final int totalCount) {
        if (requiredFields == null) {
            return null;
        }

        return new StripeNeededFieldsDTO(requiredFields, totalCount);
    }

    public static List<StripeNeededFieldsDTO> listToDTO(final List<String> fields) {
        if (Util.isEmpty(fields)) {
            return emptyList();
        }

        return fields.stream()
                .map(f -> StripeNeededFieldsDTO.toDTO(StripeRequiredFields.byFieldName(f), fields.size()))
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
