package com.diemlife.models;

import static com.diemlife.models.PaymentTransaction.StripeTransactionType.BACKING;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "QuestBackingTransactions")
@Table(name = "payment_transaction")
@DiscriminatorValue("Q")
public class QuestBackingTransaction extends QuestRelatedTransaction {

    @Override
    public StripeTransactionType getStripeTransactionType() {
        return BACKING;
    }

}
