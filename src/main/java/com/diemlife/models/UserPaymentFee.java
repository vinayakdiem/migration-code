package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity UserPaymentFee
 * Created 23/11/2020
 *
 * @author SYushchenko
 */
@Entity(name = "UserPaymentFee")
@Table(name = "user_payment_fee")
@Getter
@Setter
@NoArgsConstructor
public class UserPaymentFee {
    @Id
    @Column(name = "user_id", nullable = false, unique = true)
    private long userId;

    @Column(name = "fee", nullable = false)
    private double fee;
}
