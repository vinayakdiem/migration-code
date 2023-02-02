package com.diemlife.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class LinkPreviewDTO implements Serializable {

    private String title;
    private String description;
    private String image;
    private String url;

    public LinkPreviewDTO(final String url) {
        this.url = url;
    }

}
