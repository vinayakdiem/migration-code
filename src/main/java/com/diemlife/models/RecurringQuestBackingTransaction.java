package com.diemlife.models;

import static com.diemlife.models.PaymentTransaction.StripeTransactionType.SUBSCRIPTION;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "RecurringQuestBackingTransactions")
@Table(name = "payment_transaction")
@DiscriminatorValue("S")
public class RecurringQuestBackingTransaction extends QuestBackingTransaction {

    @Override
    public StripeTransactionType getStripeTransactionType() {
        return SUBSCRIPTION;
    }

}
