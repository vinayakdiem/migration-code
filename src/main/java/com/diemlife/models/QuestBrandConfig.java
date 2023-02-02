package com.diemlife.models;

import static javax.persistence.FetchType.EAGER;

import java.io.Serializable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "QuestBrandConfig")
@Table(name = "quest_brand_config")
@Getter
@Setter
@NoArgsConstructor
public class QuestBrandConfig implements Serializable {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "questId", column = @Column(name = "quest_id")),
            @AttributeOverride(name = "userId", column = @Column(name = "brand_user_id"))
    })
    private QuestBrandConfigId id;

    @ManyToOne(fetch = EAGER, targetEntity = BrandConfig.class)
    @JoinColumn(name = "brand_user_id", nullable = false, insertable = false, updatable = false)
    private BrandConfig brandConfig;

    @ManyToOne(fetch = EAGER, targetEntity = Quests.class)
    @JoinColumn(name = "quest_id", nullable = false, insertable = false, updatable = false)
    private Quests quest;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "secondary_recipient", nullable = false)
    private boolean secondaryRecipient;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class QuestBrandConfigId implements Serializable {
        private Integer questId;
        private Integer userId;
    }

}
