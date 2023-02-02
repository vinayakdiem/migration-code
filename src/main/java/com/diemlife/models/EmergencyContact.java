package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "EmergencyContacts")
@Table(name = "emergency_contact")
public class EmergencyContact extends IdentifiedEntity {

    @Column(name = "name")
    public String name;

    @Column(name = "phone")
    public String phone;

    @Column(name = "email")
    public String email;

}
