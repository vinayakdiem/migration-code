package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.vividsolutions.jts.geom.Point;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity PostalCodeUs
 * Created 19/11/2020
 *
 * @author SYushchenko
 */
@Entity(name = "PostalCodeUs")
@Table(name = "postal_code_us")
@Getter
@Setter
@NoArgsConstructor
public class PostalCodeUs {

    @Id
    @Column(name = "zip", nullable = false, unique = true)
    private Character zip;

    @Column(name = "city")
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "latlong")
    private String latLong;

    @Column(name = "point")
    private Point point;
}
