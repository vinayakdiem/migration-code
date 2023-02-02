package com.diemlife.constants;

import lombok.Getter;

/**
 * Used to define which size the various types of images used are
 */
public enum ImageType {
    QUEST_IMAGE(2048, 2048),
    AVATAR(500, 500),
    COVER_PHOTO(750, 1250),
    TASK_IMAGE(440, 720);

    @Getter
    public int height;
    @Getter
    public int width;

    ImageType(int height, int width) {
        this.height = height;
        this.width = width;
    }
}
