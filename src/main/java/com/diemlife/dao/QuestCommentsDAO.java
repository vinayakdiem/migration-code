package com.diemlife.dao;

import com.diemlife.dto.UserWithFiendStatusDTO;
import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.QuestCommentLike;
import com.diemlife.models.QuestComments;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

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

@Repository
public class QuestCommentsDAO {

	 @PersistenceContext
	 private EntityManager entityManager;
	 
    public void persist(QuestComments transientInstance) {

        try {
            entityManager.persist(transientInstance);
        } catch (RuntimeException e) {
            Logger.error("QuestCommentsDAO :: persist : error persisting quest => " + e, e);
        }
    }

    public void remove(QuestComments persistentInstance) {

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
	
	public boolean setCommentImage(Connection c, long commentId, long imageId) {
		boolean ret;
		
		//FIXME Raj
		// TODO: change this to delayed insert once InnoDB is ditched in favor of MyISAM
		/*try (PreparedStatement ps = c.prepareStatement("insert into quest_comment_image (comment_id, image_id) values (?, ?) on duplicate key update image_id = ?")) {
			ps.setLong(1, commentId);
			ps.setLong(2, imageId);
			ps.setLong(3, imageId);
			
			ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("addCommentImage - error", e);
			ret = false;
		}
	*/	
		return false;	
	}
	
	public boolean removeCommentImage(Connection c, long commentId) {
		boolean ret = false;
		//FIXME Raj
		/*try (PreparedStatement ps = c.prepareStatement("delete from quest_comment_image where comment_id = ?")) {
			ps.setLong(1, commentId);
			ps.executeUpdate();
			ret = true;
		} catch (Exception e) {
			Logger.error("removeCommentImage - error", e);
			ret = false;
		}
		*/
		return ret;
	}

    public QuestComments addCommentsToQuestByUserIdAndQuestId(final Integer userId,
                                                                     final Integer questId,
                                                                     final Integer inReplyTo,
                                                                     final String comment                                                                     ) {

        return addCommentsToQuestByUserIdAndQuestId(userId, questId, inReplyTo, comment, null);
    }
	
    public QuestComments addCommentsToQuestByUserIdAndQuestId(final Integer userId,
                                                                     final Integer questId,
                                                                     final Integer inReplyTo,
                                                                     final String comment,
                                                                     final Long questBackingId
                                                                     ) {
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
                final QuestComments repliedComment = entityManager.find(QuestComments.class, inReplyTo);
                if (repliedComment != null) {
                    questComments.setInReplyTo(repliedComment);
                }
            }

            entityManager.persist(questComments);
            entityManager.flush();

            final QuestComments addedComment = entityManager.find(QuestComments.class, questComments.getId());
            addedComment.setUser(entityManager.find(User.class, userId));

            entityManager.merge(addedComment);

            updateQuestCommentsCount(questId, getCommentsCountForQuest(questId));

            return addedComment;
        } catch (final PersistenceException e) {
            Logger.error("QuestCommentsDAO :: addCommentsToQuestByUserIdAndQuestId : error adding new comment => " + e.getMessage(), e);

            return null;
        }
    }

    private long getCommentsCountForQuest(final int questId) {
        return entityManager.createQuery("SELECT COUNT(c) FROM QuestComments c WHERE c.questId = :questId", Long.class)
                .setParameter("questId", questId)
                .getSingleResult();
    }

    private void updateQuestCommentsCount(final int questId, final long commentsCount) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<Quests> update = cb.createCriteriaUpdate(Quests.class);
        final Root<Quests> root = update.from(Quests.class);
        final int rowsUpdated = entityManager.createQuery(update.set(root.get("commentCount"), commentsCount).where(cb.equal(root.get("id"), questId))).executeUpdate();
        if (rowsUpdated == 1) {
            Logger.info("Successfully update comments count to " + commentsCount + " on Quest with ID " + questId);
        }
    }

    public List<QuestComments> getAllCommentsByQuestId(final Integer questId) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        try {
            return entityManager.createQuery("SELECT c FROM QuestComments c " +
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

    public boolean toggleCommentLike(final QuestComments comment, final User liker) {
        final List<QuestCommentLike> likes = entityManager.createQuery(
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
            entityManager.persist(like);
            return true;
        } else {
            likes.forEach(like -> {
                comment.getLikes().remove(like);
                entityManager.remove(like);
            });
            entityManager.merge(comment);
            return false;
        }
    }

    public void deleteComment(Integer commentId) {
        try {
            QuestComments comment = entityManager.find(QuestComments.class, commentId);

            comment.setDeleted("Y");
            comment.setDeletedDate(new Date());

            entityManager.merge(comment);

            //decrease comment count
            Quests quest = entityManager.find(Quests.class, comment.getQuestId());
            quest.setCommentCount(quest.getCommentCount() - 1);
            entityManager.merge(quest);
        } catch (PersistenceException pe) {
            Logger.info("QuestCommentsDAO :: deleteComment : error deleting comment => " + pe, pe);
        }
    }

    public void editComment(Integer commentId, String text) {
        try {
            QuestComments comment = entityManager.find(QuestComments.class, commentId);

            comment.setComments(text);
            comment.setEdited("Y");
            comment.setLastModifiedDate(new Date());

            entityManager.merge(comment);
        } catch (PersistenceException pe) {
            Logger.info("QuestCommentsDAO :: editComment : error editing comment => " + pe, pe);
        }
    }

    public UserWithFiendStatusDTO getMentionedUser(final String userName) {
        try {
            return entityManager.createQuery("SELECT u FROM User u WHERE u.userName = :userName", User.class)
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

    public QuestComments findById(final Integer id) {
        if (id == null) {
            return null;
        }
        return entityManager.find(QuestComments.class, id);
    }

}
