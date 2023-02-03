package com.diemlife.forms;

import java.io.Serializable;

public class EmbeddedVideoThumbnailsForm implements Serializable {

    private String xs;
    private String sm;
    private String md;

    public EmbeddedVideoThumbnailsForm() {
        super();
    }

    public String getXs() {
        return xs;
    }

    public void setXs(final String xs) {
        this.xs = xs;
    }

    public String getSm() {
        return sm;
    }

    public void setSm(final String sm) {
        this.sm = sm;
    }

    public String getMd() {
        return md;
    }

    public void setMd(final String md) {
        this.md = md;
    }

}
