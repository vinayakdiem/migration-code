package com.diemlife.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;

import javax.annotation.Nonnull;

import com.diemlife.models.Quests;
import com.diemlife.models.User;
import com.diemlife.utils.ChainedComparator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestListDTO extends QuestDTO implements Comparable<QuestListDTO> {

    @JsonIgnore
    private QuestListDetailDTO detail;
    @JsonProperty
    private QuestListFilterDTO filter;
    @JsonIgnore
    private QuestPermissionsDTO questPermissions;

    private QuestListDTO(final Quests quest) {
        super(quest);
    }

    public QuestListDTO withDetail(final QuestListDetailDTO detail) {
        this.detail = detail;
        return this;
    }

    public QuestListDTO withFilter(final QuestListFilterDTO filter) {
        this.filter = filter;
        return this;
    }

    public QuestListDTO withPermissions(final QuestPermissionsDTO questPermissions) {
        this.questPermissions = questPermissions;
        return this;
    }

    @JsonProperty
    public Long getProgress() {
        if (detail == null) {
            return null;
        }
        if (detail.totalTasks == 0) {
            return null;
        }
        return new BigDecimal(detail.completeTasks)
                .multiply(new BigDecimal(100L))
                .divide(new BigDecimal(detail.totalTasks), RoundingMode.HALF_UP)
                .longValue();
    }

    @JsonGetter
    public QuestListFilterDTO getFilter() {
        return filter;
    }

    @JsonProperty
    public boolean getEditable() {
        return questPermissions.editable;
    }

    public static QuestListDTO toDTO(final Quests quest, final User currentUser) {
        if (quest == null) {
            return null;
        }
        return new QuestListDTO(quest).withPermissions(new QuestPermissionsDTO(quest, currentUser));
    }

    @Override
    public int compareTo(final @Nonnull QuestListDTO other) {
        return new ChainedComparator<QuestListDTO>(
                (left, right) -> Boolean.compare(left.isStarred(), right.isStarred()),
                Comparator.comparingLong(dto -> negativeForNull(dto.getProgress())),
                Comparator.comparing(dto -> pastForNull(dto.dateModified))
        ).compare(other, this);
    }

    private static long negativeForNull(final Long value) {
        return value == null ? -1L : value;
    }

    private static Date pastForNull(final Date value) {
        return value == null ? new Date(0) : value;
    }

}
