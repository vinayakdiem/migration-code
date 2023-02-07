package com.diemlife.dao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

import com.diemlife.models.PushNotificationDevice;

import lombok.NonNull;
import play.Logger;

@Repository
public class PushNotificationDeviceDAO {

	 @PersistenceContext
	 private EntityManager entityManager;

   

    public void addNewDevice(PushNotificationDevice device) {
        try {
        	entityManager.persist(device);
        } catch (ConstraintViolationException e) {
        	//FIXME Vinayak
//            Logger.warn("device token already exists, not saving again for user [{}]", device.getUserId());
        }
    }

    public boolean isDeviceRegistered(@NonNull final String token) {
        return entityManager.createQuery("SELECT COUNT(pnd) FROM PushNotificationDevice pnd WHERE pnd.token = :token", Long.class)
                .setParameter("token", token)
                .getSingleResult() > 0;
    }

    public List<PushNotificationDevice> findForUser(final int userId) {
        return new ArrayList<>(entityManager
            .createQuery("SELECT pnd FROM PushNotificationDevice pnd WHERE pnd.userId = :userId", PushNotificationDevice.class)
            .setParameter("userId", userId)
            .getResultList());
    }

    public void updateDeviceToken(PushNotificationDevice device) {
    	entityManager.merge(device);
    }

    public PushNotificationDevice findById(PushNotificationDevice device) {
    	//FIXME Vinayak
//        return entityManager.find(PushNotificationDevice.class, device.getToken());
    	return null;
    }
}
