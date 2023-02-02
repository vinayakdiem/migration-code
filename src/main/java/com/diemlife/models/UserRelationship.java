package com.diemlife.models;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by acoleman1 on 10/16/2016.
 */
@Entity
@Table(name = "user_relationship", schema = "diemlife", catalog = "")
public class UserRelationship {
    private int id;
    private int userOneId;
    private int userTwoId;
    private int status;
    private int actionUserId;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "user_one_id")
    public int getUserOneId() { return userOneId; }

    public void setUserOneId(int userOneId) {
        this.userOneId = userOneId;
    }

    @Column(name = "user_two_id")
    public int getUserTwoId() {
        return userTwoId;
    }

    public void setUserTwoId(int userTwoId) {
        this.userTwoId = userTwoId;
    }

    @Basic
    @Column(name = "status")
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Basic
    @Column(name = "action_user_id")
    public int getActionUserId() {
        return actionUserId;
    }

    public void setActionUserId(int actionUserId) {
        this.actionUserId = actionUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserRelationship that = (UserRelationship) o;

        if (id != that.id) return false;
        if (status != that.status) return false;
        if (actionUserId != that.actionUserId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + status;
        result = 31 * result + actionUserId;
        return result;
    }
}
