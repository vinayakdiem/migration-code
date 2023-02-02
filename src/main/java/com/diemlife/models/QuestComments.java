package com.diemlife.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "quest_comments")
public class QuestComments {
    private Integer id;
    private Integer questId;
    private Integer userId;
    private Long questBackingId;
    private QuestComments inReplyTo;
    private String userName;
    private String comments;
    private Date createdDate;
    private Date lastModifiedDate;
    private String edited;
    private String deleted;
    private Date deletedDate;
    private User user;
    private List<QuestCommentLike> likes;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "quest_id")
    public Integer getQuestId() {
        return questId;
    }

    public void setQuestId(Integer questId) {
        this.questId = questId;
    }

    @Column(name = "user_id")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Column(name = "quest_backing_id")
    public Long getQuestBackingId() {
        return questBackingId;
    }

    public void setQuestBackingId(Long questBackingId) {
        this.questBackingId = questBackingId;
    }

    @ManyToOne(fetch = EAGER, targetEntity = QuestComments.class)
    @JoinColumn(name = "in_reply_to_comment_id", updatable = false)
    public QuestComments getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(final QuestComments inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    @Column(name = "user_name")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name = "comments")
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Column(name = "created_date")
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(name = "last_modified_date")
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Column(name = "edited")
    public String getEdited() {
        return edited;
    }

    public void setEdited(String edited) {
        this.edited = edited;
    }

    @Column(name = "deleted")
    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    @Column(name = "deleted_date")
    public Date getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
    }

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "Id", insertable = false, updatable = false)
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user == null) {
            setUserId(null);
        } else {
            setUserId(user.getId());
        }
    }

    @JsonManagedReference("questCommentReference")
    @OneToMany(cascade = ALL, orphanRemoval = true, fetch = EAGER)
    @JoinColumn(name = "quest_comment_id", updatable = false)
    @OrderBy("createdOn DESC")
    public List<QuestCommentLike> getLikes() {
        return likes;
    }

    public void setLikes(final List<QuestCommentLike> likes) {
        this.likes = likes;
    }

}
