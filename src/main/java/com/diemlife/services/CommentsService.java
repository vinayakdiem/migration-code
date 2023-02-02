package com.diemlife.services;

import com.diemlife.dao.ContentReportDAO;
import com.diemlife.dao.QuestBackingDAO;
import com.diemlife.dao.QuestCommentsDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dto.CommentsDTO;
import com.diemlife.dto.QuestImageDTO;
import com.diemlife.dto.UserWithFiendStatusDTO;
import models.*;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import com.diemlife.utils.BackerDisplayNameUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.sql.Connection;
import java.util.List;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.diemlife.dao.UserRelationshipDAO.checkForFriendshipStatus;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

@Singleton
public class CommentsService {

    private final JPAApi jpaApi;

    @Inject
    public CommentsService(final JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public QuestComments getById(final Integer id) {
        return jpaApi.em().find(QuestComments.class, id);
    }

    @Transactional(readOnly = true)
    public List<CommentsDTO> getQuestCommentsVisibleToUser(Connection c, final Quests quest, final User sessionUser) {
        final List<QuestComments> questComments = questCommentsVisibleToUser(quest.getId(), sessionUser);
        final Map<Long, QuestBacking> questBackingsMap = getMapQuestBackingByComments(questComments);
        return questComments.stream()
                .filter(Objects::nonNull)
                .filter(comment -> comment.getInReplyTo() == null)
                .map(comment -> CommentsDTO.toDTO(comment)
                        .withMentions(getCommentMentions(comment, sessionUser))
                        .withReplies(commentReplies(comment, questComments, sessionUser))
                        .withImage(getQuestCommentImage(c, comment)))
                .peek(comment -> {
                    Brand company = UserHome.getCompanyForUser(UserHome.findByEmail(comment.userEmail,jpaApi.em()), this.jpaApi);
                    if (comment.questBackingId != null && questBackingsMap.containsKey(comment.questBackingId)){
                        QuestBacking questBacking = questBackingsMap.get(comment.questBackingId);
                        comment.backerDisplayName = BackerDisplayNameUtils.getBackerDisplayName(questBacking);
                    }
                    if(company!=null) {
                        comment.userFirstName = company.getName();
                        comment.userLastName = "";
                    }
                })
                .collect(toList());
    }

	private QuestImageDTO getQuestCommentImage(Connection c, QuestComments comment) {
		Long imageId = QuestCommentsDAO.getCommentImage(c, comment.getId());
		QuestImage questImage = ((imageId == null) ? null : jpaApi.em().find(QuestImage.class, imageId.intValue()));
		return ((questImage == null) ? null : QuestImageDTO.toDTO(questImage));
	}
	
    private List<QuestComments> questCommentsVisibleToUser(final Integer questId, final User sessionUser) {
        final EntityManager entityManager = jpaApi.em();
        final List<ContentReport> reports = new ContentReportDAO(entityManager).getAllCommentReportsByUser(sessionUser);
        return QuestCommentsDAO.getAllCommentsByQuestId(questId, entityManager).stream()
                .filter(comment -> !hasUserReport(comment, reports))
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public List<CommentsDTO> getCommentReplies(final QuestComments comment, final User sessionUser) {
        return commentReplies(comment, questCommentsVisibleToUser(comment.getQuestId(), sessionUser), sessionUser);
    }

    private List<CommentsDTO> commentReplies(final QuestComments comment, List<QuestComments> questComments, final User sessionUser) {

        return questComments.stream()
                .filter(reply -> reply.getInReplyTo() != null)
                .filter(reply -> reply.getInReplyTo().getId().equals(comment.getId()))
                .map(reply -> CommentsDTO.toDTO(reply)
                        .withMentions(getCommentMentions(reply, sessionUser))
                        .withReplies(commentReplies(reply, questComments, sessionUser)))
                .peek(reply->{
                    Brand company = UserHome.getCompanyForUser(UserHome.findByEmail(reply.userEmail,jpaApi.em()), this.jpaApi);
                    if(company!=null){
                        reply.userFirstName = company.getName();
                        reply.userLastName = "";
                    }
                })
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public List<UserWithFiendStatusDTO> getCommentMentions(final QuestComments comment, final User sessionUser) {
        return commentMentions(comment, sessionUser);
    }

    private List<UserWithFiendStatusDTO> commentMentions(final QuestComments comment, final User sessionUser) {
        if (comment != null && isEmpty(comment.getDeleted()) && isNotEmpty(comment.getComments())) {
            final String[] tokens = trimToEmpty(comment.getComments())
                    .replaceAll("[\\r\\n]", " ")
                    .split("\\s+");
            if (tokens.length > 0) {
                final EntityManager em = jpaApi.em();
                return Stream.of(tokens)
                        .filter(token -> token.startsWith("@"))
                        .map(token -> removeStart(token, "@"))
                        .map(userName -> QuestCommentsDAO.getMentionedUser(userName, em))
                        .filter(Objects::nonNull)
                        .map(dto -> sessionUser == null
                                ? dto
                                : dto.withFriendStatus(checkForFriendshipStatus(sessionUser.getId(), dto.id, em)))
                        .collect(toList());
            }
        }
        return emptyList();
    }

    private static boolean hasUserReport(final QuestComments comment, final List<ContentReport> reports) {
        return reports.stream()
                .filter(report -> report.hiddenFlag)
                .anyMatch(report -> report.accusedComment != null && report.accusedComment.getId().equals(comment.getId()));
    }

    private Map<Long, QuestBacking> getMapQuestBackingByComments(final List<QuestComments> questComments) {
        final List<Long> questBackingIds = questComments.stream().map(QuestComments::getQuestBackingId).collect(Collectors.toList());
        return new QuestBackingDAO(jpaApi.em())
                .getQuestBackingsByQuestBackingIds(questBackingIds)
                .stream()
                .collect(toMap(IdentifiedEntity::getId, l -> l));
    }
}
