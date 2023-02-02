package com.diemlife.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity(name = "PushNotificationDevice")
@Table(name = "push_notification_device")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationDevice {

    @Id
    @Column(name = "token", nullable = false)
    private String token; // the token received from Firebase

    @Column(name = "user_Id", nullable = false)
    private Integer userId;

    @Column(name = "added_date", nullable = false)
    private Date addedDate;
}
