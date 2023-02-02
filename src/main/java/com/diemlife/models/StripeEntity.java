package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity(name = "StripeUsers")
@Table(name = "stripe_customer")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "stripe_user_type", length = 1)
@NamedNativeQuery(name = "StripeEntity.changeUserType", query = "UPDATE stripe_customer SET stripe_user_type = :type WHERE id = :id")
public abstract class StripeEntity extends IdentifiedEntity {

    @OneToOne(optional = false, targetEntity = User.class)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_stripe_customer_user_id"))
    @JsonBackReference("UserToStripeEntity")
    public User user;

    protected StripeEntity() {
        super();
    }

    public StripeEntity(final User user) {
        this.user = user;
    }

}
