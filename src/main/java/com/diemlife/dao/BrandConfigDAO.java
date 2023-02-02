package com.diemlife.dao;

import models.BrandConfig;
import models.QuestBrandConfig;
import models.QuestBrandConfig.QuestBrandConfigId;
import models.Quests;
import models.User;
import play.db.jpa.JPAApi;

import java.util.List;

public class BrandConfigDAO {

    private final JPAApi jpaApi;

    public BrandConfigDAO(final JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public List<BrandConfig> getLandingBrands() {
        return jpaApi.em()
                .createQuery("SELECT bc FROM BrandConfig bc WHERE bc.onLanding = TRUE ORDER BY bc.landingOrder", BrandConfig.class)
                .getResultList();
    }

    public BrandConfig findBrandConfigBySiteUrl(final String siteUrl) {
        return jpaApi.em()
                .createQuery("SELECT bc FROM BrandConfig bc WHERE bc.siteUrl = :siteUrl", BrandConfig.class)
                .setParameter("siteUrl", siteUrl)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<QuestBrandConfig> findAllQuestBrandConfigsByUser(final User user) {
        return jpaApi.em()
                .createQuery("SELECT qbc FROM QuestBrandConfig qbc WHERE qbc.id.userId = :userId", QuestBrandConfig.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    public List<QuestBrandConfig> findAllQuestBrandConfigsByQuest(final Quests quest) {
        return jpaApi.em()
                .createQuery("SELECT qbc FROM QuestBrandConfig qbc WHERE qbc.id.questId = :questId", QuestBrandConfig.class)
                .setParameter("questId", quest.getId())
                .getResultList();
    }

    public boolean exists(final QuestBrandConfigId id) {
        if (id == null || id.getQuestId() == null || id.getUserId() == null) {
            return false;
        }
        return jpaApi.em()
                .createQuery("SELECT COUNT(qbc) FROM QuestBrandConfig qbc WHERE qbc.id.questId = :questId AND qbc.id.userId = :userId", Long.class)
                .setParameter("questId", id.getQuestId())
                .setParameter("userId", id.getUserId())
                .getSingleResult() > 0;
    }

    public void enableForQuest(final BrandConfig brandConfig, final Quests quest) {
        if (brandConfig == null || quest == null) {
            throw new IllegalArgumentException("Both brand config and Quest are required");
        }

        final QuestBrandConfig questBrandConfig = new QuestBrandConfig();
        final QuestBrandConfigId id = new QuestBrandConfigId(quest.getId(), brandConfig.getUserId());
        questBrandConfig.setId(id);
        questBrandConfig.setQuest(quest);
        questBrandConfig.setBrandConfig(brandConfig);
        questBrandConfig.setEnabled(true);

        jpaApi.em().persist(questBrandConfig);
    }

}
