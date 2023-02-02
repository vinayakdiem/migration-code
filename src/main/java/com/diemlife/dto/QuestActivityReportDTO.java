package com.diemlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class QuestActivityReportDTO implements Serializable {
    @Id
    private Integer userId;
    private String userEmail;
    private String userName;
    private String userFirstName;
    private String userLastName;
    private String activityStatus;
    private String activityGroup;
    private Integer activityCompletedCount;
    private Timestamp activityAddedDate;
    private Timestamp activityModifiedDate;
    private Timestamp pushAddedDate;
    private Integer leaderboardMemberId;

    @Transient
    private Map<LocalDate, QuestActivityReportDailyDTO> calendar;
}
