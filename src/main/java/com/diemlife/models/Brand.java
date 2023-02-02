package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.CascadeType;

@Getter
@Setter
@NoArgsConstructor
@Entity(name="Company")
@Table(name = "company")
public class Brand extends IdentifiedEntity {

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "address_id")
    public Address address;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "org_type", nullable = false)
    private String orgType;

    @Column(name = "website", nullable = false)
    private String website;

    @Column(name = "phone_number")
    private String phone;
}
