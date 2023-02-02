package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "StripeCustomers")
@DiscriminatorValue("C")
@Table(name = "stripe_customer")
public class StripeCustomer extends StripeEntity {

    @Column(name = "stripe_customer_id")
    public String stripeCustomerId;

    protected StripeCustomer() {
        super();
    }

    public StripeCustomer(final User user) {
        this.user = user;
    }

}
