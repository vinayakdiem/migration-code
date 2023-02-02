package com.diemlife.dao;

import lombok.NonNull;
import models.PushNotificationDevice;
import org.hibernate.exception.ConstraintViolationException;
import play.Logger;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PushNotificationDeviceDAO {

    private final JPAApi jpaApi;

    @Inject
    public PushNotificationDeviceDAO(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public void addNewDevice(PushNotificationDevice device) {
        try {
            jpaApi.em().persist(device);
        } catch (ConstraintViolationException e) {
            Logger.warn("device token already exists, not saving again for user [{}]", device.getUserId());
        }
    }

    public boolean isDeviceRegistered(@NonNull final String token) {
        return jpaApi.em().createQuery("SELECT COUNT(pnd) FROM PushNotificationDevice pnd WHERE pnd.token = :token", Long.class)
                .setParameter("token", token)
                .getSingleResult() > 0;
    }

    public List<PushNotificationDevice> findForUser(final int userId) {
        return new ArrayList<>(jpaApi.em()
            .createQuery("SELECT pnd FROM PushNotificationDevice pnd WHERE pnd.userId = :userId", PushNotificationDevice.class)
            .setParameter("userId", userId)
            .getResultList());
    }

    public void updateDeviceToken(PushNotificationDevice device) {
        jpaApi.em().merge(device);
    }

    public PushNotificationDevice findById(PushNotificationDevice device) {
        return jpaApi.em().find(PushNotificationDevice.class, device.getToken());
    }
}
