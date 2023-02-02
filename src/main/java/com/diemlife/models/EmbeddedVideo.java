package com.diemlife.models;

import static javax.persistence.EnumType.STRING;
import static javax.persistence.FetchType.EAGER;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.diemlife.constants.VideoProvider;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

@Entity(name = "EmbeddedVideos")
@Table(name = "embedded_video")
public class EmbeddedVideo extends IdentifiedEntity {

    @Column(name = "url")
    public String url;

    @Enumerated(STRING)
    @Column(name = "provider")
    public VideoProvider provider;

    @Embeddable
    public static class ThumbnailKey implements Serializable {
        @Column(name = "video_id", insertable = false, updatable = false)
        public Long videoId;
        @Column(name = "size")
        public String size;

        protected ThumbnailKey() {
            super();
        }

        public ThumbnailKey(final @NotNull Long videoId, final @NotNull String size) {
            this.videoId = videoId;
            this.size = size;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            final ThumbnailKey that = (ThumbnailKey) other;
            return new EqualsBuilder()
                    .append(videoId, that.videoId)
                    .append(size, that.size)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(videoId)
                    .append(size)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return size;
        }
    }

    @ElementCollection(fetch = EAGER, targetClass = String.class)
    @CollectionTable(name = "embedded_video_thumbnail", joinColumns = {
            @JoinColumn(name = "video_id")
    })
    @MapKeyClass(ThumbnailKey.class)
    @MapKeyJoinColumns({
            @MapKeyJoinColumn(name = "video_id"),
            @MapKeyJoinColumn(name = "size")
    })
    @Column(name = "url")
    @Access(AccessType.FIELD)
    public Map<ThumbnailKey, String> thumbnails;

    public static class VideoUrlSerializer extends JsonSerializer<EmbeddedVideo> {
        @Override
        public void serialize(final EmbeddedVideo value,
                              final JsonGenerator generator,
                              final SerializerProvider serializers) throws IOException {
            generator.writeString(value.url);
        }
    }

    public enum ThumbnailSizes {
        XS, SM, MD;
    }

}
