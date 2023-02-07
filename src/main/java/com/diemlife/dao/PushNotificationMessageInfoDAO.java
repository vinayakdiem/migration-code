package com.diemlife.dao;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.PushNotificationMessageInfo;

import lombok.NonNull;

@Repository
public class PushNotificationMessageInfoDAO {

	 @PersistenceContext
	 private EntityManager entityManager;

   

    public void addMessageInfo(@NonNull PushNotificationMessageInfo messageInfo) {
    	entityManager.persist(messageInfo);
    }

    public void updateMessageInfo(@NonNull PushNotificationMessageInfo messageInfo) {
    	entityManager.merge(messageInfo);
    }

    public Optional<PushNotificationMessageInfo> findByMessageId(final String messageId) {
        return entityManager.createQuery("SELECT pmi FROM PushNotificationMessageInfo pmi " +
            "WHERE pmi.messageId = :messageId", PushNotificationMessageInfo.class)
            .setParameter("messageId", messageId)
            .getResultList()
            .stream()
                .findFirst();
    }

}
