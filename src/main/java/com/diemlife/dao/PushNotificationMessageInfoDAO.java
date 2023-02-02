package dao;

import lombok.NonNull;
import models.PushNotificationMessageInfo;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.Optional;

public class PushNotificationMessageInfoDAO {

    private final JPAApi jpaApi;

    @Inject
    public PushNotificationMessageInfoDAO(@NonNull JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public void addMessageInfo(@NonNull PushNotificationMessageInfo messageInfo) {
        jpaApi.em().persist(messageInfo);
    }

    public void updateMessageInfo(@NonNull PushNotificationMessageInfo messageInfo) {
        jpaApi.em().merge(messageInfo);
    }

    public Optional<PushNotificationMessageInfo> findByMessageId(final String messageId) {
        return jpaApi.em().createQuery("SELECT pmi FROM PushNotificationMessageInfo pmi " +
            "WHERE pmi.messageId = :messageId", PushNotificationMessageInfo.class)
            .setParameter("messageId", messageId)
            .getResultList()
            .stream()
                .findFirst();
    }

}
