package com.diemlife.constants;

import java.util.Arrays;

public enum BrandImportAction {

    CREATE('C'), DELETE('D'), NOOP('N');

    private final char action;

    BrandImportAction(final char action) {
        this.action = action;
    }

    public char getAction() {
        return action;
    }

    public static BrandImportAction from(final String action) {
        return Arrays.stream(BrandImportAction.values())
                .filter(value -> Character.valueOf(value.action).toString().equalsIgnoreCase(action))
                .findFirst()
                .orElse(NOOP);
    }

}
