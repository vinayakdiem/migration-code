package com.diemlife.constants;

public enum Interests {

    PHYSICAL("Physical", "favPhysical"),
    MENTAL("Mental", "favIntel"),
    SOCIAL("Social", "favSocial"),
    ENVIRONMENTAL("Environmental", "favEnv"),
    OCCUPATIONAL("Occupational", "favOcc"),
    FINANCIAL("Financial", "favFin");

    private final String label;
    private final String input;

    Interests(final String label, final String input) {
        this.label = label;
        this.input = input;
    }

    public String getValue() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getInput() {
        return input;
    }

}
