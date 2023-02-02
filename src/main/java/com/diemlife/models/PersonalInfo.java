package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.util.Date;

@Entity(name = "PersonalInfo")
@Table(name = "personal_info")
public class PersonalInfo extends IdentifiedEntity {

    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name", nullable = false)
    public String lastName;

    @Column(name = "email", nullable = false)
    public String email;

    @Column(name = "home_phone")
    public String homePhone;

    @Column(name = "cell_phone")
    public String cellPhone;

    @Column(name = "gender", length = 1)
    public Character gender;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "birth_date")
    public Date birthDate;

    @Column(name = "age")
    public Integer age;

    @Column(name = "shirt_size", length = 15)
    public String shirtSize;

    @Column(name = "burger_temp")
    public String burgerTemp;

    @Column(name = "with_cheese")
    public String withCheese;

    @Column(name = "special_requests")
    public String specialRequests;

	@Column(name = "team_id")
	public Long teamId;

    @Transient
    public String getName() {
        return firstName + " " + lastName;
    }

}
