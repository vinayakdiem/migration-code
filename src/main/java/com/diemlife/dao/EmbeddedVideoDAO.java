package com.diemlife.dao;

import models.EmbeddedVideo;

import javax.persistence.EntityManager;

public class EmbeddedVideoDAO extends TypedDAO<EmbeddedVideo> {

    public EmbeddedVideoDAO(final EntityManager entityManager) {
        super(entityManager);
    }

    public EmbeddedVideo findByURL(final String url) {
        return entityManager.createQuery("SELECT ev FROM EmbeddedVideos ev WHERE ev.url LIKE :url", EmbeddedVideo.class)
                .setParameter("url", url)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

}
