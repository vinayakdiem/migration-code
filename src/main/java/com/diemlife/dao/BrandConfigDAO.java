package com.diemlife.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.BrandConfig;
import com.diemlife.models.QuestBrandConfig;
import com.diemlife.models.QuestBrandConfig.QuestBrandConfigId;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

@Repository
public class BrandConfigDAO {

	@PersistenceContext
	private EntityManager entityManager;
  
    public List<BrandConfig> getLandingBrands() {
        return entityManager
                .createQuery("SELECT bc FROM BrandConfig bc WHERE bc.onLanding = TRUE ORDER BY bc.landingOrder", BrandConfig.class)
                .getResultList();
    }

    public BrandConfig findBrandConfigBySiteUrl(final String siteUrl) {
        return entityManager
                .createQuery("SELECT bc FROM BrandConfig bc WHERE bc.siteUrl = :siteUrl", BrandConfig.class)
                .setParameter("siteUrl", siteUrl)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<QuestBrandConfig> findAllQuestBrandConfigsByUser(final User user) {
        return entityManager
                .createQuery("SELECT qbc FROM QuestBrandConfig qbc WHERE qbc.id.userId = :userId", QuestBrandConfig.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    public List<QuestBrandConfig> findAllQuestBrandConfigsByQuest(final Quests quest) {
        return entityManager
                .createQuery("SELECT qbc FROM QuestBrandConfig qbc WHERE qbc.id.questId = :questId", QuestBrandConfig.class)
                .setParameter("questId", quest.getId())
                .getResultList();
    }

    public boolean exists(final QuestBrandConfigId id) {
    	return false;
    	//FIXME Vinayak
//        if (id == null || id.getQuestId() == null || id.getUserId() == null) {
//            return false;
//        }
//        return entityManager
//                .createQuery("SELECT COUNT(qbc) FROM QuestBrandConfig qbc WHERE qbc.id.questId = :questId AND qbc.id.userId = :userId", Long.class)
//                .setParameter("questId", id.getQuestId())
//                .setParameter("userId", id.getUserId())
//                .getSingleResult() > 0;
    }

    public void enableForQuest(final BrandConfig brandConfig, final Quests quest) {
        if (brandConfig == null || quest == null) {
            throw new IllegalArgumentException("Both brand config and Quest are required");
        }

        final QuestBrandConfig questBrandConfig = new QuestBrandConfig();
      //FIXME Vinayak
//        final QuestBrandConfigId id = new QuestBrandConfigId(quest.getId(), brandConfig.getUserId());
//        questBrandConfig.setId(id);
//        questBrandConfig.setQuest(quest);
//        questBrandConfig.setBrandConfig(brandConfig);
//        questBrandConfig.setEnabled(true);

        entityManager.persist(questBrandConfig);
    }

}
