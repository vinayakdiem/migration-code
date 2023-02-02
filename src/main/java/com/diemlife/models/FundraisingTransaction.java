package com.diemlife.models;

import static javax.persistence.FetchType.EAGER;
import static com.diemlife.models.PaymentTransaction.StripeTransactionType.FUNDRAISING;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name = "FundraisingTransactions")
@Table(name = "payment_transaction")
@DiscriminatorValue("F")
public class FundraisingTransaction extends QuestBackingTransaction {

    public FundraisingTransaction() {
        super();
    }

    public FundraisingTransaction(final User intermediary) {
        this.intermediary = intermediary;
    }

    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "doer_user_id", foreignKey = @ForeignKey(name = "doer_user_id_fk"))
    public User intermediary;

    @Override
    public StripeTransactionType getStripeTransactionType() {
        return FUNDRAISING;
    }

}
