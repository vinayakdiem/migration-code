package dao;

import models.ContentReport;
import models.Quests;
import models.User;

import javax.persistence.EntityManager;
import java.util.List;

import static java.util.Collections.emptyList;

public class ContentReportDAO extends TypedDAO<ContentReport> {

    public ContentReportDAO(final EntityManager entityManager) {
        super(entityManager);
    }

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
