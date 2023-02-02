package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.diemlife.models.QuestCommentLike;
import com.diemlife.models.QuestComments;

public class CommentsDTO implements Serializable {

    public Integer id;
    public Integer questId;
    public Integer userId;
    public Long questBackingId;
    public String userName;
    public String userFirstName;
    public String userLastName;
    public String userEmail;
    public String backerDisplayName;
    public String userAvatarUrl;
    public String comment;
    public Date createdDate;
    public Date lastModifiedDate;
    public String edited;
    public String deleted;
    public Date deletedDate;
    public List<QuestCommentLike> likes;
    public List<UserWithFiendStatusDTO> mentions;
    public List<CommentsDTO> replies;
	public QuestImageDTO image;
	
    public static CommentsDTO toDTO(final QuestComments comment) {
        final CommentsDTO dto = new CommentsDTO();
        dto.id = comment.getId();
        dto.questId = comment.getQuestId();
        dto.userId = comment.getUserId();
        dto.questBackingId = comment.getQuestBackingId();
        dto.userName = comment.getUser().getUserName();
        dto.userFirstName = comment.getUser().getFirstName();
        dto.userLastName = comment.getUser().getLastName();
        dto.userEmail = comment.getUser().getEmail();
        dto.userAvatarUrl = comment.getUser().getProfilePictureURL();
        dto.comment = comment.getComments();
        dto.createdDate = comment.getCreatedDate();
        dto.lastModifiedDate = comment.getLastModifiedDate();
        dto.edited = comment.getEdited();
        dto.deleted = comment.getDeleted();
        dto.deletedDate = comment.getDeletedDate();
        if (comment.getLikes() != null && comment.getLikes().size() > 0) {
            dto.likes = comment.getLikes();
        }
        return dto;
    }

    public CommentsDTO withMentions(final List<UserWithFiendStatusDTO> mentions) {
        this.mentions = mentions;
        return this;
    }

    public CommentsDTO withReplies(final List<CommentsDTO> replies) {
        this.replies = replies;
        return this;
    }

	public CommentsDTO withImage(QuestImageDTO image) {
		this.image = image;
		return this;
	}
}
