package com.diemlife.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.EmbeddedVideo;

@Repository
public class EmbeddedVideoDAO extends TypedDAO<EmbeddedVideo> {

	@PersistenceContext
	EntityManager entityManager;

    public EmbeddedVideo findByURL(final String url) {
        return entityManager.createQuery("SELECT ev FROM EmbeddedVideos ev WHERE ev.url LIKE :url", EmbeddedVideo.class)
                .setParameter("url", url)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

}
