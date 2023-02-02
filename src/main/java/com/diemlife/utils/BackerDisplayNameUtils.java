package com.diemlife.utils;

import static org.junit.platform.commons.util.StringUtils.isNotBlank;

import com.diemlife.models.QuestBacking;

public class BackerDisplayNameUtils {

    public static String getBackerDisplayName(final QuestBacking questBacking) {
        if (isNotBlank(questBacking.getBackerFirstName()) && isNotBlank(questBacking.getBackerLastName())) {
            return questBacking.getBackerFirstName() + " " + questBacking.getBackerLastName();
        }
        return questBacking.getBackerFirstName() != null ? questBacking.getBackerFirstName() : questBacking.getBackerLastName();
    }
}
