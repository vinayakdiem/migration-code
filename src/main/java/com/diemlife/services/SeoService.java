package com.diemlife.services;

import com.typesafe.config.Config;
import org.apache.http.HttpStatus;
import play.Logger;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class SeoService {

    private final Config config;
    private final WSClient client;

    @Inject
    public SeoService(Config config, WSClient client) {
        this.config = config;
        this.client = client;
    }

    public boolean capturePage(String uri) {
        if ((uri == null) || uri.isEmpty()) {
            return false;
        }
        
        String apiUrl = config.getString("application.seo4ajax.api_url");
        String apiKey = config.getString("application.seo4ajax.api_key");
        String siteKey = config.getString("application.seo4ajax.site_key");

        WSRequest req = client.url(apiUrl + "/" + siteKey + "/pendings");
        req.addHeader("x-api-key", apiKey);
        req.setContentType("application/json");
        
        try {
            req.post("{ \"paths\" : [ \"" + uri + "\" ] }")
                .thenApply(response -> {
                    if (HttpStatus.SC_OK == response.getStatus()) {
                        Logger.debug(String.format("capturePage - successfully submitted " + uri));
                        return true;
                    } else {
                        Logger.error(String.format("capturePage - Unable to submit page for capture '%s', response status %s, response body:\n%s", uri, response.getStatus(), response.getBody()));
                        return false;
                    }
                })
                .toCompletableFuture()
                .get();
                
            return true;
            
        } catch (Exception e) {
            Logger.error("capture - error", e);
            return false;
        }

    }

    public void capturePageBackground(final String uri) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    capturePage(uri);
                } catch (Exception e) {
                    Logger.error("capturePageBackground - error", e);
                }
            }
        });
        t.start();
        Logger.debug("capturePageBackground - launched thread for uri: " + uri);
    }
}
