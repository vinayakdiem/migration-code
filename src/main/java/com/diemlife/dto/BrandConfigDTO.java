package com.diemlife.dto;

import static lombok.AccessLevel.PRIVATE;

import java.io.Serializable;

import com.diemlife.models.BrandConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
public class BrandConfigDTO implements Serializable {

    private final int id;
    private final String name;
    private final boolean nonProfit;
    private final String logoUrl;
    private final String siteUrl;
    private final boolean secondaryRecipient;

    public static BrandConfigDTO toDto(final BrandConfig entity) {
        if (entity == null) {
            return null;
        }
        return BrandConfigDTO.builder()
                .id(entity.getUserId())
                .name(entity.getFullName())
                .nonProfit(entity.isNonProfit())
                .logoUrl(entity.getLogoUrl())
                .siteUrl(entity.getSiteUrl())
                .build();
    }

}
