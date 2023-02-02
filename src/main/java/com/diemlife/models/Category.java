package com.diemlife.models;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by andrewcoleman on 3/1/16.
 */
@Entity
@Table(name="category")
public class Category {

    private int id;
    private String code;
    private String name;
    private int status;
    private int createdBy;
    private Date dateCreated;
    private int modifiedBy;
    private Date dateModified;
    private int updateCount;
    private int isCourse;

    @Id
    @Column(name="Id")
    public int getId() {
        return this.id;
    }

    @Column(name = "Code")
    public String getCode() {
        return this.code;
    }

    @Column(name = "Name")
    public String getName(){
        return this.name;
    }

    @Column(name = "Status")
    public int getStatus() {
        return this.status;
    }

    @Column(name = "CreatedBy")
    public int getCreatedBy() {
        return this.createdBy;
    }

    @Column(name = "DateCreated", columnDefinition = "datetime")
    public Date getDateCreated() {
        return this.dateCreated;
    }

    @Column(name = "ModifiedBy")
    public int getModifiedBy() {
        return this.createdBy;
    }

    @Column(name = "DateModified", columnDefinition = "datetime")
    public Date getDateModified() {
        return this.dateModified;
    }

    @Column(name = "UpdateCount")
    public int getUpdateCount() {
        return this.updateCount;
    }

    @Column(name = "IsCourse")
    public int getIsCourse() {
        return this.isCourse;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setModifiedBy(int modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public void setIsCourse(int isCourse) {
        this.isCourse = isCourse;
    }
}
