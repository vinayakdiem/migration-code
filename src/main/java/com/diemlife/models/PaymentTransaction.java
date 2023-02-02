package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Date;

@Entity(name = "PaymentTransactions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "stripe_transaction_target", length = 1)
@Table(name = "payment_transaction")
public abstract class PaymentTransaction extends IdentifiedEntity {

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on", nullable = false)
    public Date createdOn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_from_customer_id"))
    public StripeEntity from;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_to_customer_id"))
    public StripeAccount to;

    @Column(name = "valid", nullable = false)
    public boolean valid;

    @Column(name = "stripe_transaction_id", nullable = false)
    public String stripeTransactionId;

    @Column(name = "coupon_used")
    public String couponUsed;

    @Column(name = "is_anonymous")
    public boolean isAnonymous;

    @Column(name = "mailing")
    public boolean isMailing;

    public abstract StripeTransactionType getStripeTransactionType();

    @PrePersist
    void prePersist() {
        if (createdOn == null) {
            createdOn = Calendar.getInstance().getTime();
        }
    }

    public enum StripeTransactionType {
        BACKING, SUBSCRIPTION, ORDER, PAYOUT, FUNDRAISING
    }

}
