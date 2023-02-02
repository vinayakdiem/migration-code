package com.diemlife.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.ok;

public class RecaptchaService {

    private final Config config;
    private final WSClient wsClient;

    @Inject
    public RecaptchaService(Config config, WSClient wsClient) {
        this.config = checkNotNull(config, "config");
        this.wsClient = checkNotNull(wsClient, "wsClient");
    }

    public CompletionStage<Result> verifyRecaptcha(final String response) {
        final String secret = config.getString("recaptcha.secret");
        final JsonNode params = Json.newObject().put("secret", secret)
                .put("response", response);

        final CompletionStage<WSResponse> verifyResponse = wsClient
                .url("https://www.google.com/recaptcha/api/siteverify")
                .post(params);

        return verifyResponse.thenApplyAsync(r -> ok("status = " + r.getStatus()));
    }

}
