package com.diemlife.dao;

import com.diemlife.models.Address;

public class AddressDAO extends TypedSingletonDAO<Address> {

   
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
