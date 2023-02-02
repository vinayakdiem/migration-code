package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Created by andrewcoleman on 4/3/16.
 */
@Entity
@Table(name = "s3file")
@Getter
@Setter
@NoArgsConstructor
public class S3File {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "bucket")
    private String bucket;
    @Column(name = "name")
    private String name;

    @Transient
    private File file;
    @Transient
    private ByteArrayInputStream contentData;
    @Transient
    private long contentLength;

    // Extra image attributes
    @Transient
    private Integer imgWidth;
    @Transient
    private Integer imgHeight;
    @Transient
    private String contentType;
}
