package com.diemlife.models;

import static com.diemlife.models.PaymentTransaction.StripeTransactionType.PAYOUT;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "UserBackingTransactions")
@DiscriminatorValue("P")
@Table(name = "payment_transaction")
public class PayoutTransaction extends PaymentTransaction {

    @Override
    public StripeTransactionType getStripeTransactionType() {
        return PAYOUT;
    }

}
