package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "Addresses")
@Table(name = "address")
public class Address extends IdentifiedEntity {
    @Column(name = "country", nullable = false)
    public String country;

    @Column(name = "line_one")
    public String lineOne;

    @Column(name = "line_two")
    public String lineTwo;

    @Column(name = "city")
    public String city;

    @Column(name = "state")
    public String state;

    @Column(name = "zip")
    public String zip;

}
