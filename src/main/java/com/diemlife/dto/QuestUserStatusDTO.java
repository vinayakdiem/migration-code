package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("unused")
public class QuestUserStatusDTO implements Serializable {

    public final String activityStatus;
    public final String activityMode;
    public final Date activitySince;
    public final boolean completed;
    public final boolean doing;
    public final boolean supporting;
    public final boolean following;
    public final boolean fundraising;
    public final boolean starred;
    public final boolean saved;
    public final boolean editable;

    private QuestUserStatusDTO(final String activityStatus,
                               final String activityMode,
                               final Date activitySince,
                               final boolean completed,
                               final boolean doing,
                               final boolean supporting,
                               final boolean following,
                               final boolean fundraising,
                               final boolean starred,
                               final boolean saved,
                               final boolean editable) {
        this.activityStatus = activityStatus;
        this.activityMode = activityMode;
        this.activitySince = activitySince;
        this.completed = completed;
        this.doing = doing;
        this.supporting = supporting;
        this.following = following;
        this.fundraising = fundraising;
        this.starred = starred;
        this.saved = saved;
        this.editable = editable;
    }

    public static QuestUserStatusActivityBuilder builder() {
        return status -> mode -> since -> completed -> doing -> supporting -> following -> fundraising -> starred -> saved -> editable ->
                () -> new QuestUserStatusDTO(status, mode, since, completed, doing, supporting, following, fundraising, starred, saved, editable);
    }

    public interface QuestUserStatusActivityBuilder {
        QuestUserStatusModeBuilder withStatus(final String status);
    }

    public interface QuestUserStatusModeBuilder {
        QuestUserStatusSinceBuilder withMode(final String mode);
    }

    public interface QuestUserStatusSinceBuilder {
        QuestUserStatusCompletedBuilder withSince(final Date since);
    }

    public interface QuestUserStatusCompletedBuilder {
        QuestUserStatusDoingBuilder withCompleted(final boolean completed);
    }

    public interface QuestUserStatusDoingBuilder {
        QuestUserStatusSupportingBuilder withDoing(final boolean doing);
    }

    public interface QuestUserStatusSupportingBuilder {
        QuestUserStatusFollowingBuilder withSupporting(final boolean supporting);
    }

    public interface QuestUserStatusFollowingBuilder {
        QuestUserStatusFundraisingBuilder withFollowing(final boolean following);
    }

    public interface QuestUserStatusFundraisingBuilder {
        QuestUserStatusStarredBuilder withFundraising(final boolean fundraising);
    }

    public interface QuestUserStatusStarredBuilder {
        QuestUserStatusSavedBuilder withStarred(final boolean starred);
    }

    public interface QuestUserStatusSavedBuilder {
        QuestUserStatusEditableBuilder withSaved(final boolean saved);
    }

    public interface QuestUserStatusEditableBuilder {
        QuestUserStatusFinalBuilder withEditable(final boolean editable);
    }

    public interface QuestUserStatusFinalBuilder {
        QuestUserStatusDTO build();
    }

}
