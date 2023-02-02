package com.diemlife.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

import static javax.persistence.FetchType.EAGER;

@Entity(name = "UserActivationCodes")
@Table(name = "user_activation_code")
public class UserActivationPinCode extends IdentifiedEntity {

    @Column(name = "pin_code", length = 4, nullable = false)
    public String pinCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on", nullable = false, updatable = false)
    public Date createdOn;

    @Column(name = "consumed", nullable = false)
    public boolean consumed;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "consumed_on")
    public Date consumedOn;

    @ManyToOne(optional = false, fetch = EAGER)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "user_activation_code_user_id_fk"))
    public User user;

}
