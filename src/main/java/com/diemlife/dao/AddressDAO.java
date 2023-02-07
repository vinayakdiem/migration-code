package com.diemlife.dao;

import org.springframework.stereotype.Repository;

import com.diemlife.models.Address;

@Repository
public class AddressDAO extends TypedSingletonDAO<Address> {

   
    public Address createAddress(String addLine1, String addLine2, String city, String state, String country, String zip) {
        final Address address = new Address();
      //FIXME Vinayak
//        address.setLineOne(addLine1);
//        address.setLineTwo(addLine2);
//        address.setCity(city);
//        address.setState(state);
//        address.setCountry(country);
//        address.setZip(zip);

        return save(address, Address.class);
    }
}
