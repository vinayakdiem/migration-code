package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
public class TestDTO implements Serializable {

    public Long notificationsCount;
    @Id
    public Integer lastUserId;
    public String lastUserName;
    public Date createdDate;
    @Id
    public String notificationsType;
    @Id
    public Integer fromQuestId;
    public String fromQuestTitle;
    public Boolean isRead;
    public Long countFromUsers;
}
