package com.diemlife.dao;

import com.diemlife.models.QuestMapRoute;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Quest map route DAO
 * Created 25/11/2002
 *
 * @author SYushchenko
 */
@Repository
public class QuestMapRouteDAO extends TypedDAO<QuestMapRoute> {
    
	@PersistenceContext
	EntityManager entityManager;

    /**
     * Find all quest map route by quest
     *
     * @param questId quest id
     * @return collection {@link QuestMapRoute}
     */
    public List<QuestMapRoute> findAllQuestMapRoutesByQuest(final Integer questId) {
        return entityManager.createQuery("SELECT qmr FROM QuestMapRoute qmr " +
                " WHERE qmr.questId = :questId AND qmr.active = TRUE", QuestMapRoute.class)
                .setParameter("questId", questId)
                .getResultList();
    }

    /**
     * Toggle all quest map route
     *
     * @param questId questId
     * @param flag    flag (true | false)
     */
    public void toggleAllQuestMapRoute(final Integer questId, final Boolean flag) {
        Long questMapRouteId = getMaxQuestMapRouteIdByQuest(questId);

        Query query = entityManager.createQuery(" UPDATE QuestMapRoute qmp " +
                "SET qmp.active = :flag " +
                "WHERE qmp.questId = :questId "
                + (flag ? "AND qmp.id = :id" : ""))
                .setParameter("flag", flag)
                .setParameter("questId", questId);

        if (flag) {
            query.setParameter("id", questMapRouteId);
        }

        query.executeUpdate();
    }

    /**
     * Get toggle status by quest id
     *
     * @param questId quest id
     * @return Status
     */
    public Boolean toggleStatusQuestMapRoute(final Integer questId) {
        Long questMapRouteId = getMaxQuestMapRouteIdByQuest(questId);
        return entityManager.createQuery(" SELECT qmp.active FROM QuestMapRoute qmp " +
                "WHERE qmp.questId = :questId AND qmp.id = :id", Boolean.class)
                .setParameter("id", questMapRouteId)
                .setParameter("questId", questId)
                .getSingleResult();
    }

    private Long getMaxQuestMapRouteIdByQuest(final Integer questId) {
        return entityManager.createQuery("SELECT max(qmr.id) " +
                " FROM QuestMapRoute qmr WHERE qmr.questId = :questId", Long.class)
                .setParameter("questId", questId)
                .getSingleResult();
    }
}
