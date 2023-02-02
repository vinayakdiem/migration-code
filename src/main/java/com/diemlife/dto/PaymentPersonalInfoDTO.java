package com.diemlife.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO payment personal info
 * Created 2/11/2020
 *
 * @author SYushchenko
 */
@Getter
@Setter
@AllArgsConstructor
public class PaymentPersonalInfoDTO {
    private final Long paymentTransactionId;
    private final String firstName;
    private final String lastName;
    private final String email;
}
