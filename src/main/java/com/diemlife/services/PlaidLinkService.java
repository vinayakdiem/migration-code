package com.diemlife.services;

import com.plaid.client.PlaidClient;
import com.plaid.client.request.ItemPublicTokenExchangeRequest;
import com.plaid.client.request.ItemStripeTokenCreateRequest;
import com.plaid.client.response.ItemPublicTokenExchangeResponse;
import com.plaid.client.response.ItemStripeTokenCreateResponse;
import com.typesafe.config.Config;
import com.diemlife.constants.PlaidEnvironment;
import retrofit2.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PlaidLinkService {

	@Autowired
	private Config configuration;
	
	@Autowired
    private String plaidSecretKey = configuration.getString("plaid.secretKey");
	
	@Autowired
    private String plaidPublicKey = configuration.getString("plaid.publicKey");
	
	@Autowired
    private String plaidClientId = configuration.getString("plaid.clientId");
	
	@Autowired
    private PlaidEnvironment plaidEnvironment = PlaidEnvironment.valueOf(configuration.getString("plaid.environment"));

    public String getBankAccountToken(final String publicToken, final String accountId) throws PlaidServiceException {
        final PlaidClient plaidClient = buildPlaidClient(plaidEnvironment);
        try {
            final Response<ItemPublicTokenExchangeResponse> exchangeResponse = plaidClient
                    .service()
                    .itemPublicTokenExchange(new ItemPublicTokenExchangeRequest(publicToken))
                    .execute();

            if (exchangeResponse.isSuccessful()) {
                final String accessToken = exchangeResponse.body().getAccessToken();
                final Response<ItemStripeTokenCreateResponse> stripeResponse = plaidClient
                        .service()
                        .itemStripeTokenCreate(new ItemStripeTokenCreateRequest(accessToken, accountId))
                        .execute();

                if (stripeResponse.isSuccessful()) {
                    return stripeResponse.body().getStripeBankAccountToken();
                } else {
                    throw new PlaidServiceException(stripeResponse.errorBody().string());
                }
            } else {
                throw new PlaidServiceException(exchangeResponse.errorBody().string());
            }
        } catch (final IOException e) {
            throw new PlaidServiceException(e);
        }
    }

    private PlaidClient buildPlaidClient(final PlaidEnvironment environment) throws PlaidServiceException {
        final PlaidClient.Builder builder = PlaidClient.newBuilder()
                .clientIdAndSecret(plaidClientId, plaidSecretKey)
                .publicKey(plaidPublicKey);
        switch (environment) {
            case SANDBOX:
                return builder.sandboxBaseUrl().build();
            case DEVELOPMENT:
                return builder.developmentBaseUrl().build();
            case PRODUCTION:
                return builder.productionBaseUrl().build();
            default:
                throw new PlaidServiceException(new IllegalArgumentException());
        }
    }

    private static class PlaidServiceException extends RuntimeException {
        private PlaidServiceException(final String message) {
            super(message);
        }

        private PlaidServiceException(final Throwable cause) {
            super(cause);
        }
    }

}
