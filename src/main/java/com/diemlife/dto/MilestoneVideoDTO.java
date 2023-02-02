package com.diemlife.dto;

import static com.diemlife.models.EmbeddedVideo.ThumbnailSizes.SM;

import java.io.Serializable;
import java.util.Optional;

import com.diemlife.models.EmbeddedVideo;
import com.diemlife.models.EmbeddedVideo.ThumbnailKey;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MilestoneVideoDTO implements Serializable {

    private Integer videoId;
    private String videoUrl;
    private String thumbnailUrl;

    public static MilestoneVideoDTO toDto(final EmbeddedVideo video) {
        if (video == null) {
            return null;
        }
        final MilestoneVideoDTO dto = new MilestoneVideoDTO();
        dto.setVideoId(video.getId().intValue());
        dto.setVideoUrl(video.url);
        dto.setThumbnailUrl(Optional.ofNullable(video.thumbnails)
                .map(thumbnails -> thumbnails.get(new ThumbnailKey(video.getId(), SM.name().toLowerCase())))
                .orElse(null));
        return dto;
    }

}
