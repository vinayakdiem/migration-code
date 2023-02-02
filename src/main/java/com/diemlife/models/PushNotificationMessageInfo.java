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

@Entity(name = "PushNotificationMessageInfo")
@Table(name = "push_notification_message_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationMessageInfo {

    @Id
    @Column(name = "message_id")
    private String messageId;

    @Column(name = "device")
    private String device;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "tracking_id")
    private String trackingId;

    @Column(name = "opened_date")
    private Date openedDate;

    @Column(name = "message_title")
    private String messageTitle;

    @Column(name = "message_body")
    private String messageBody;
}
