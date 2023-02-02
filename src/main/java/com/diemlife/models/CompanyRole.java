package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name="CompanyRole")
@Table(name = "company_role")
public class CompanyRole extends IdentifiedEntity {
    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "company_id")
    private Brand company;

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "role", nullable = false)
    private String role;

    /*@Column(name="is_representative",nullable = false)
    private Integer isRepresentative;*/
}
