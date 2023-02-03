package com.diemlife.forms;

import java.io.Serializable;

public class EmbeddedVideoForm implements Serializable {

    private String url;
    private String provider;
    private EmbeddedVideoThumbnailsForm thumbnails;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public EmbeddedVideoThumbnailsForm getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(final EmbeddedVideoThumbnailsForm thumbnails) {
        this.thumbnails = thumbnails;
    }

}
