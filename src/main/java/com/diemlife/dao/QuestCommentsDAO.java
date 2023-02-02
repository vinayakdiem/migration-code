package com.diemlife.dao;

import dto.UserWithFiendStatusDTO;
import exceptions.RequiredParameterMissingException;
import models.QuestCommentLike;
import models.QuestComments;
import models.Quests;
import models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Created by andrew on 5/24/17.
 */
public class QuestCommentsDAO {

    public void persist(QuestComments transientInstance, EntityManager entityManager) {

        try {
            entityManager.persist(transientInstance);
        } catch (RuntimeException e) {
            Logger.error("QuestCommentsDAO :: persist : error persisting quest => " + e, e);
        }
    }

    public void remove(QuestComments persistentInstance, EntityManager entityManager) {

        try {
            entityManager.remove(persistentInstance);
        } catch (RuntimeException e) {
            Logger.error("QuestCommentsDAO :: remove : error removing quest comment => " + e, e);
        }
    }

	public static Long getCommentImage(Connection c, long commentId) {
		Long ret = null;
		try (PreparedStatement ps = c.prepareStatement("select image_id from quest_comment_image where comment_id = ?")) {
			ps.setLong(1, commentId);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.first()) {
					ret = rs.getLong(1);
				}
			}
		} catch (Exception e) {
			Logger.error("getCommentImage - error", e);
			ret = null;
		}
		
		return ret;
	}
	
	public static boolean setCommentImage(Connection c, long commentId, long imageId) {
		boolean ret;
		// TODO: change this to delayed insert once InnoDB is ditched in favor of MyISAM
		try (PreparedStatement ps = c.prepareStatement("insert into quest_comment_image (comment_id, image_id) values (?, ?) on duplicate key update image_id = ?")) {
			ps.setLong(1, commentId);
			ps.setLong(2, imageId);
			ps.setLong(3, imageId);
			
			ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("addCommentImage - error", e);
			ret = false;
		}
		
		return ret;	
	}
	
	public static boolean removeCommentImage(Connection c, long commentId) {
		boolean ret;
		try (PreparedStatement ps = c.prepareStatement("delete from quest_comment_image where comment_id = ?")) {
			ps.setLong(1, commentId);
			ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("removeCommentImage - error", e);
			ret = false;
		}
		
		return ret;
	}

    public static QuestComments addCommentsToQuestByUserIdAndQuestId(final Integer userId,
                                                                     final Integer questId,
                                                                     final Integer inReplyTo,
                                                                     final String comment,
                                                                     final EntityManager em) {

        return addCommentsToQuestByUserIdAndQuestId(userId, questId, inReplyTo, comment, null, em);
    }
	
    public static QuestComments addCommentsToQuestByUserIdAndQuestId(final Integer userId,
                                                                     final Integer questId,
                                                                     final Integer inReplyTo,
                                                                     final String comment,
                                                                     final Long questBackingId,
                                                                     final EntityManager em) {
        if (userId == null || questId == null || isBlank(comment)) {
            Logger.warn("Cannot add comment : missing required arguments");

            return null;
        }

        try {
            final Date now = new Date();
            final QuestComments questComments = new QuestComments();
            questComments.setQuestId(questId);
            questComments.setUserId(userId);
            questComments.setComments(comment);
            questComments.setCreatedDate(now);
            questComments.setLastModifiedDate(now);
            questComments.setQuestBackingId(questBackingId);

            if (inReplyTo != null) {
                final QuestComments repliedComment = em.find(QuestComments.class, inReplyTo);
                if (repliedComment != null) {
                    questComments.setInReplyTo(repliedComment);
                }
            }

            em.persist(questComments);
            em.flush();

            final QuestComments addedComment = em.find(QuestComments.class, questComments.getId());
            addedComment.setUser(em.find(User.class, userId));

            em.merge(addedComment);

            updateQuestCommentsCount(questId, getCommentsCountForQuest(questId, em), em);

            return addedComment;
        } catch (final PersistenceException e) {
            Logger.error("QuestCommentsDAO :: addCommentsToQuestByUserIdAndQuestId : error adding new comment => " + e.getMessage(), e);

            return null;
        }
    }

    private static long getCommentsCountForQuest(final int questId, final EntityManager em) {
        return em.createQuery("SELECT COUNT(c) FROM QuestComments c WHERE c.questId = :questId", Long.class)
                .setParameter("questId", questId)
                .getSingleResult();
    }

    private static void updateQuestCommentsCount(final int questId, final long commentsCount, final EntityManager em) {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaUpdate<Quests> update = cb.createCriteriaUpdate(Quests.class);
        final Root<Quests> root = update.from(Quests.class);
        final int rowsUpdated = em.createQuery(update.set(root.get("commentCount"), commentsCount).where(cb.equal(root.get("id"), questId))).executeUpdate();
        if (rowsUpdated == 1) {
            Logger.info("Successfully update comments count to " + commentsCount + " on Quest with ID " + questId);
        }
    }

    public static List<QuestComments> getAllCommentsByQuestId(final Integer questId, final EntityManager em) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        try {
            return em.createQuery("SELECT c FROM QuestComments c " +
                            "WHERE c.questId = :questId " +
                            "AND c.deleted IS NULL " +
                            "ORDER BY c.createdDate DESC",
                    QuestComments.class)
                    .setParameter("questId", questId)
                    .getResultList();
        } catch (final PersistenceException ex) {
            Logger.error("QuestCommentsDAO :: getAllCommentsByQuestId : Error finding comments => " + ex, ex);
            return emptyList();
        }
    }

    public static boolean toggleCommentLike(final QuestComments comment, final User liker, final EntityManager em) {
        final List<QuestCommentLike> likes = em.createQuery(
                "SELECT l FROM QuestCommentLikes l " +
                        "WHERE l.liker.id = :likerId " +
                        "AND l.comment.id = :commentId",
                QuestCommentLike.class)
                .setParameter("commentId", comment.getId())
                .setParameter("likerId", liker.getId())
                .getResultList();
        if (likes.isEmpty()) {
            final QuestCommentLike like = new QuestCommentLike();
            like.comment = comment;
            like.liker = liker;
            like.createdOn = Calendar.getInstance().getTime();
            em.persist(like);
            return true;
        } else {
            likes.forEach(like -> {
                comment.getLikes().remove(like);
                em.remove(like);
            });
            em.merge(comment);
            return false;
        }
    }

    public static void deleteComment(Integer commentId, EntityManager em) {
        try {
            QuestComments comment = em.find(QuestComments.class, commentId);

            comment.setDeleted("Y");
            comment.setDeletedDate(new Date());

            em.merge(comment);

            //decrease comment count
            Quests quest = em.find(Quests.class, comment.getQuestId());
            quest.setCommentCount(quest.getCommentCount() - 1);
            em.merge(quest);
        } catch (PersistenceException pe) {
            Logger.info("QuestCommentsDAO :: deleteComment : error deleting comment => " + pe, pe);
        }
    }

    public static void editComment(Integer commentId, String text, EntityManager em) {
        try {
            QuestComments comment = em.find(QuestComments.class, commentId);

            comment.setComments(text);
            comment.setEdited("Y");
            comment.setLastModifiedDate(new Date());

            em.merge(comment);
        } catch (PersistenceException pe) {
            Logger.info("QuestCommentsDAO :: editComment : error editing comment => " + pe, pe);
        }
    }

    public static UserWithFiendStatusDTO getMentionedUser(final String userName, final EntityManager em) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.userName = :userName", User.class)
                    .setParameter("userName", userName)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .map(user -> new UserWithFiendStatusDTO(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getUserName(),
                            user.getMissionStatement(),
                            user.getProfilePictureURL()
                    ))
                    .orElse(null);
        } catch (final PersistenceException e) {
            Logger.warn("Can't get comment mentioned user " + userName, e);
            return null;
        }
    }

    public static QuestComments findById(final Integer id, final EntityManager entityManager) {
        if (id == null) {
            return null;
        }
        return entityManager.find(QuestComments.class, id);
    }

}
