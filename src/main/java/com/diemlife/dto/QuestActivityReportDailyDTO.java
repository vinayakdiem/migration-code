package com.diemlife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PUBLIC;

@Getter
@Setter
@NoArgsConstructor
public class QuestActivityReportDailyDTO implements Serializable {

    private LocalDate date;
    private QuestActivityNotificationDTO notification;
    private QuestActivityCompletionDTO completion;
    private List<QuestActivityCommentDTO> comments;
    private List<QuestActivityImageDTO> images;
    private List<QuestActivityPromptDTO> prompts;

    public QuestActivityReportDailyDTO(final LocalDate date) {
        this.date = date;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PUBLIC)
    public static class QuestActivityNotificationDTO implements Serializable {
        private Integer userId;
        private Boolean notificationOpened;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PUBLIC)
    public static class QuestActivityCommentDTO implements Serializable {
        private Integer userId;
        private String newsFeedComment;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PUBLIC)
    public static class QuestActivityImageDTO implements Serializable {
        private Integer userId;
        private String imageCaption;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PUBLIC)
    public static class QuestActivityCompletionDTO implements Serializable {
        private Integer userId;
        private Boolean questCompleted;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PUBLIC)
    public static class QuestActivityPromptDTO implements Serializable {
        private Integer userId;
        private String promptEventName;
        private String promptQuestion;
        private String promptAnswer;
        private Time promptTime;
    }

}
