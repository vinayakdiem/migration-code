package com.diemlife.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Data
@NoArgsConstructor
@Entity
@Table(name = "fundraising_supplement", schema = "diemlife")
public class FundraisingSupplement {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Integer id;

    @Column(name = "fundraising_quest_id")
    private Integer questId;

    @Column(name = "fundraising_user_id")
    private Integer userId;

    @Column(name = "amount_cents")
    private Integer amount;

    @Column(name = "created_on")
    private Date createdOn;

    @Column(name = "comment")
    private String comment;
}
