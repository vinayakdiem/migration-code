package com.diemlife.dao;

import models.Address;
import models.Brand;
import org.opengis.filter.expression.Add;
import play.Logger;
import play.db.jpa.JPAApi;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddressDAO extends TypedSingletonDAO<Address> {

    public AddressDAO(JPAApi jpaApi) {
        super(jpaApi);
    }

    public Address createAddress(String addLine1, String addLine2, String city, String state, String country, String zip) {
        final Address address = new Address();
        address.setLineOne(addLine1);
        address.setLineTwo(addLine2);
        address.setCity(city);
        address.setState(state);
        address.setCountry(country);
        address.setZip(zip);

        return save(address, Address.class);
    }
}
