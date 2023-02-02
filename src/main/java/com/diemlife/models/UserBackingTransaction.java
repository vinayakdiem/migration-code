package com.diemlife.models;

import static com.diemlife.models.PaymentTransaction.StripeTransactionType.BACKING;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "UserBackingTransactions")
@DiscriminatorValue("U")
@Table(name = "payment_transaction")
public class UserBackingTransaction extends PaymentTransaction {

    @Override
    public StripeTransactionType getStripeTransactionType() {
        return BACKING;
    }

}
