package com.diemlife.constants;

public enum DeploymentEnvironments {

    LOCAL("play.localhost.url"),
    DEV("play.dev.url"),
    STAGING("play.staging.url"),
    PROD("play.prod.url");

    private final String baseUrlKey;

    DeploymentEnvironments(final String baseUrlKey) {
        this.baseUrlKey = baseUrlKey;
    }

    public String getBaseUrlKey() {
        return baseUrlKey;
    }

}
