package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications", schema = "diemlife")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "type")
    private String type;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "is_read")
    private boolean isRead;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "from_user")
    private Integer fromUser;

    @Column(name = "from_quest")
    private Integer fromQuest;

    @Column(name = "from_comment")
    private Integer fromComment;

    @Column(name = "time_read")
    private Date readDate;

    @Transient
    private String message;
    @Transient
    private String avatar;

}
