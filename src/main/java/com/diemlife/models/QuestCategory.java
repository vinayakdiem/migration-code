package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "QuestCategory")
@Table(name = "quest_category")
@Getter
@Setter
@NoArgsConstructor
public class QuestCategory extends IdentifiedEntity {

    @Column(name = "quest_id")
    private int questId;

    @Column(name = "category")
    private String category;

    @Column(name = "confidence")
    private float confidence;

    @Column(name = "user_modified")
    private boolean userModified;

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;

        if (!(other instanceof QuestCategory)) return false;

        final QuestCategory that = (QuestCategory) other;

        return new EqualsBuilder()
                .append(questId, that.questId)
                .append(confidence, that.confidence)
                .append(category, that.category)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(questId)
                .append(category)
                .append(confidence)
                .toHashCode();
    }

}
