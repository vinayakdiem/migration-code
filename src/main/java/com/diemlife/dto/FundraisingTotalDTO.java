package com.diemlife.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO fundraising total
 * Created 13/11/2020
 *
 * @author SYushchenko
 */
@Getter
@AllArgsConstructor
public class FundraisingTotalDTO {
    private final int amount;
    private final String stripeTransactionId;

    /**
     * Create new {@link FundraisingTotalDTO}
     * @param fundraisingTotalResult result
     * @return new {@link FundraisingTotalDTO}
     */
    public static FundraisingTotalDTO from(Object[] fundraisingTotalResult) {
        return new FundraisingTotalDTO(((Number) fundraisingTotalResult[0]).intValue(),
                ((String) fundraisingTotalResult[1]));
    }
}
