package com.diemlife.dao;

import static java.util.Collections.emptyList;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.ContentReport;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

@Repository
public class ContentReportDAO extends TypedDAO<ContentReport> {
	
	@PersistenceContext
	EntityManager entityManager;

    public List<ContentReport> getSpecificQuestReportsByUser(final Quests accused, final User reporter) {
        if (reporter == null || reporter.getId() == null) {
            return emptyList();
        }
        return entityManager.createQuery("SELECT cr " +
                "FROM ContentReports cr " +
                "WHERE cr.reporter.id = :reporterId " +
                "AND cr.accusedQuest.id = :accusedQuestId", ContentReport.class)
                .setParameter("reporterId", reporter.getId())
                .setParameter("accusedQuestId", accused.getId())
                .getResultList();
    }

    public List<ContentReport> getAllCommentReportsByUser(final User reporter) {
        if (reporter == null || reporter.getId() == null) {
            return emptyList();
        }
        return entityManager.createQuery("SELECT cr " +
                "FROM ContentReports cr " +
                "WHERE cr.reporter.id = :reporterId " +
                "AND cr.accusedComment.id IS NOT NULL", ContentReport.class)
                .setParameter("reporterId", reporter.getId())
                .getResultList();
    }

    public List<ContentReport> getProfileReportsByUser(final User accused, final User reporter) {
        return entityManager.createQuery("SELECT cr " +
                "FROM ContentReports cr " +
                "WHERE cr.reporter.id = :reporterId " +
                "AND cr.accusedUser.id = :accusedUserId", ContentReport.class)
                .setParameter("reporterId", reporter.getId())
                .setParameter("accusedUserId", accused.getId())
                .getResultList();
    }

}
