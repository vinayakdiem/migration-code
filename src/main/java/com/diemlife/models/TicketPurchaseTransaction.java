package com.diemlife.models;

import static com.diemlife.models.PaymentTransaction.StripeTransactionType.ORDER;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity(name = "TicketPurchaseTransactions")
@DiscriminatorValue("E")
@Table(name = "payment_transaction")
public class TicketPurchaseTransaction extends PaymentTransaction {

    @OneToOne
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(name = "fk_transaction_event_id"))
    public Happening event;

    @Override
    public StripeTransactionType getStripeTransactionType() {
        return ORDER;
    }

}
