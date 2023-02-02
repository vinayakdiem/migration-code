package com.diemlife.constants;

import lombok.Getter;

public enum ImageDimensions {

    QUEST_IMAGE_ORIGINAL(1920, 1440),
    QUEST_IMAGE_LARGE(1920, 1440),
    QUEST_IMAGE_MEDIUM(1600, 1200),
    QUEST_IMAGE_SMALL(1024, 768),

    AVATAR_ORIGINAL(500, 500),
    AVATAR_LARGE(500, 500),
    AVATAR_MEDIUM(200, 200),
    AVATAR_SMALL(90, 90),

    COVER_PHOTO_ORIGINAL(2048, 1152),
    COVER_PHOTO_LARGE(2048, 1152),
    COVER_PHOTO_MEDIUM(1600, 900),
    COVER_PHOTO_SMALL(1024, 576),

    TASK_IMAGE_ORIGINAL(700, 434),
    TASK_IMAGE_LARGE(700, 434),
    TASK_IMAGE_MEDIUM(414, 257),
    TASK_IMAGE_SMALL(237, 146);

    @Getter
    public int height;
    @Getter
    public int width;

    ImageDimensions(int width, int height) {
        this.height = height;
        this.width = width;
    }

    public enum Dimensions {
        _ORIGINAL(false), _LARGE(true), _MEDIUM(true), _SMALL(true);

        @Getter
        private boolean addSuffix;

        Dimensions(boolean addSuffix) {
            this.addSuffix = addSuffix;
        }
    }
}
