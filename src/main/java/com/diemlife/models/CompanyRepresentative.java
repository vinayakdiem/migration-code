package com.diemlife.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity(name= "CompanyRepresentative")
@Table(name = "company_representative")
public class CompanyRepresentative extends IdentifiedEntity{

   /* @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;*/

    @Column(name = "email")
    private String email;

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "company_id")
    private Brand company;

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "stripe_customer_id")
    public String stripeCustomerId;

}
