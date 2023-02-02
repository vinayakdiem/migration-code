package com.diemlife.constants;

public enum NotificationType {
    FRIEND_REQUEST("notification.friend.request.recieved"),
    COMMENT("notification.comment.quest"),
    COMMENT_LIKED("notification.comment.liked"),
    COMMENT_MENTION("notification.comment.mentioned"),
    COMMENT_REPLIED("notification.comment.replied"),
    QUEST_SAVED("notification.quest.saved"),
    QUEST_STARTED("notification.quest.started"),
    QUEST_ACHIEVED("notification.quest.achieved"),
    MILESTONE_ACHIEVED("notification.quest.milestone.achieved"),
    PHOTO_VIDEO_ADDED("notification.quest.photo.video.posted"),
    QUEST_BACKED("notification.quest.backing"),
    PROFILE_BACKED("notification.profile.backing"),
    FUNDRAISER_STARTED("notification.friend.fundraiser.started"),
    EVENT_STARTED("notification.friend.event.started"),
    FOLLOWED("notification.followed.started");

    private static final String GROUPED_SUFFIX = ".grouped";
    private static final String GROUPED_SINGLE_SUFFIX = ".grouped.single-from";
    private static final String SINGULAR_SUFFIX = ".singular";
    private static final String PLURAL_SUFFIX = ".plural";

    private final String message;

    NotificationType(String message) {
        this.message = message;
    }

    public static NotificationType from(String message) {
        return NotificationType.valueOf(message);
    }

    public String code() {
        return name();
    }

    public String message() {
        return this.message;
    }

    public String groupedMessage() {
        return this.message.concat(GROUPED_SUFFIX);
    }

    public String groupedSingleMessage() {
        return this.message.concat(GROUPED_SINGLE_SUFFIX);
    }

    public String singularMessage() {
        return this.message.concat(SINGULAR_SUFFIX);
    }

    public String pluralMessage() {
        return this.message.concat(PLURAL_SUFFIX);
    }

}
