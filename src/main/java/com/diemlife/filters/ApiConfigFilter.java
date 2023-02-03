package com.diemlife.filters;

import akka.stream.Materializer;
import com.typesafe.config.Config;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.lowerCase;

public class ApiConfigFilter extends Filter {

    private final String stripePublicKey;
    private final String plaidPublicKey;
    private final String plaidEnvironment;

    @Inject
    public ApiConfigFilter(final Materializer materializer, final Config configuration) {
        super(materializer);
        this.stripePublicKey = configuration.getString("stripe.pub.key");
        this.plaidPublicKey = configuration.getString("plaid.publicKey");
        this.plaidEnvironment = configuration.getString("plaid.environment");
    }

    @Override
    public CompletionStage<Result> apply(final Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
                                         final Http.RequestHeader requestHeader) {
        return nextFilter.apply(requestHeader).thenApply(result -> result.withCookies(
                Http.Cookie.builder("stripe.publicKey", stripePublicKey)
                        .withSecure(requestHeader.secure())
                        .build(),
                Http.Cookie.builder("plaid.publicKey", plaidPublicKey)
                        .withSecure(requestHeader.secure())
                        .build(),
                Http.Cookie.builder("plaid.environment", lowerCase(plaidEnvironment))
                        .withSecure(requestHeader.secure())
                        .build()
        ));
    }

}
