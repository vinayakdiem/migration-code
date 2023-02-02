package com.diemlife.services;

import com.typesafe.config.Config;
import com.diemlife.dto.LinkPreviewDTO;
import org.apache.http.HttpStatus;
import play.Logger;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.libs.Json;
import play.libs.ws.WSClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;


import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Singleton
public class LinkPreviewService {

    private final Config config;
    private final WSClient client;
    private final SyncCacheApi cache;

    @Inject
    public LinkPreviewService(final Config config,
                              final WSClient client,
                              final @NamedCache("link-preview-cache") SyncCacheApi cache) {
        this.config = config;
        this.client = client;
        this.cache = cache;
    }

    public LinkPreviewDTO createLinkPreview(final String query) {
        final int linkTtl = config.getInt("application.preview.ttl");
        if (isNotBlank(query)) {
            return cache.getOrElseUpdate(query, () -> {
                final String apiUrl = config.getString("application.preview.url");
                final String apiKey = config.getString("application.preview.key");


                return client.url(apiUrl)
                        .addQueryParameter("key", apiKey)
                        .addQueryParameter("q", query)
                        .get()
                        .thenApply(response -> {
                            if (HttpStatus.SC_OK == response.getStatus()) {
                                try {
                                    return Json.mapper().readValue(response.getBody(), LinkPreviewDTO.class);
                                } catch (final IOException e) {
                                    Logger.error(format("Unable to parse link preview response for '%s', response body: %s", query, response.getBody()), e);
                                    return new LinkPreviewDTO(query);
                                }
                            } else {
                                Logger.error(format("createLinkPreview - Unable to get link preview response for '%s', response status %s, response body:\n%s", query, response.getStatus(), response.getBody()));
                                return null;
                            }
                        })
                        .toCompletableFuture()
                        .get();
            }, linkTtl);
        } else {
            return null;
        }
    }

}
