package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "StripeAccounts")
@DiscriminatorValue("A")
@Table(name = "stripe_customer")
public class StripeAccount extends StripeCustomer {

    @Column(name = "stripe_account_id")
    public String stripeAccountId;

    protected StripeAccount() {
        super();
    }

    public StripeAccount(final User user) {
        super(user);
    }

}
