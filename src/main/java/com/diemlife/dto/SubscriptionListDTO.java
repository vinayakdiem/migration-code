package com.diemlife.dto;

import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

import com.diemlife.models.Quests;
import com.diemlife.models.User;
import com.stripe.model.Subscription;

public class SubscriptionListDTO implements Serializable {

    public String id;
    public Date createdOn;
    public Date nextDueOn;
    public boolean cancelled;
    public QuestDTO quest;
    public UserDTO doer;

    public static SubscriptionListDTO toDTO(final Subscription subscription) {
        if (subscription == null) {
            return null;
        }
        final SubscriptionListDTO result = new SubscriptionListDTO();
        result.id = subscription.getId();
        result.createdOn = subscription.getCreated() == null ? null : new Date(Instant.ofEpochSecond(subscription.getCreated()).toEpochMilli());
        result.nextDueOn = subscription.getCurrentPeriodEnd() == null ? null : new Date(Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()).toEpochMilli());
        result.cancelled = "canceled".equals(subscription.getStatus());
        return result;
    }

    public SubscriptionListDTO withQuest(final Quests quest) {
        this.quest = QuestDTO.toDTO(quest);
        return this;
    }

    public SubscriptionListDTO withDoer(final User doer) {
        this.doer = UserDTO.toDTO(doer);
        return this;
    }

    public SubscriptionListDTO build(final String urlPrefix) {
        if (this.quest != null && this.doer != null) {
            this.quest.seoSlugs = publicQuestSEOSlugs(this.quest, this.doer, urlPrefix);
        }
        return this;
    }

}
