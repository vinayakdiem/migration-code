package com.diemlife.dto;

import java.io.Serializable;

public class QuestUserFlagsDTO implements Serializable {

    public final boolean followed;
    public final boolean saved;
    public final boolean starred;

    private QuestUserFlagsDTO(final boolean followed, final boolean saved, final boolean starred) {
        this.followed = followed;
        this.saved = saved;
        this.starred = starred;
    }

    public static QuestUserFlagsFollowingBuilder builder() {
        return following -> saved -> starred -> () -> new QuestUserFlagsDTO(following, saved, starred);
    }

    public interface QuestUserFlagsFollowingBuilder {
        QuestUserFlagsSavedBuilder withFollowing(final boolean following);
    }

    public interface QuestUserFlagsSavedBuilder {
        QuestUserFlagsStarredBuilder withSaved(final boolean saved);
    }

    public interface QuestUserFlagsStarredBuilder {
        QuestUserFlagsBuilder withStarred(final boolean starred);
    }

    public interface QuestUserFlagsBuilder {
        QuestUserFlagsDTO build();
    }

}
