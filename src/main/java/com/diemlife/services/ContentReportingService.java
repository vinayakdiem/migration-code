package com.diemlife.services;

import com.diemlife.dao.ContentReportDAO;
import com.diemlife.models.ContentReport;
import com.diemlife.models.QuestComments;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class ContentReportingService {

    public void hideQuestForUser(final Quests quest, final User user) {
        final ContentReport report = new ContentReport();
        report.reporter = user;
        report.reportedOn = new Date(Instant.now().toEpochMilli());
        report.accusedQuest = quest;
        report.hiddenFlag = true;
        new ContentReportDAO().save(report, ContentReport.class);
    }

    public void hideCommentForUser(final QuestComments comment, final User user) {
        final ContentReport report = new ContentReport();
        report.reporter = user;
        report.reportedOn = new Date(Instant.now().toEpochMilli());
        report.accusedComment = comment;
        report.hiddenFlag = true;
        new ContentReportDAO().save(report, ContentReport.class);
    }

    public void reportQuest(final Quests quest, final User reporter) {
        final ContentReport report = new ContentReport();
        report.reporter = reporter;
        report.reportedOn = new Date(Instant.now().toEpochMilli());
        report.accusedQuest = quest;
        report.reportedFlag = true;
        new ContentReportDAO().save(report, ContentReport.class);
    }

    public void reportComment(final QuestComments comment, final User reporter) {
        final ContentReport report = new ContentReport();
        report.reporter = reporter;
        report.reportedOn = new Date(Instant.now().toEpochMilli());
        report.accusedComment = comment;
        report.reportedFlag = true;
        new ContentReportDAO().save(report, ContentReport.class);
    }

    public void reportUser(final User accused, final User reporter) {
        final ContentReport report = new ContentReport();
        report.reporter = reporter;
        report.reportedOn = new Date(Instant.now().toEpochMilli());
        report.accusedUser = accused;
        report.reportedFlag = true;
        new ContentReportDAO().save(report, ContentReport.class);
    }

}
