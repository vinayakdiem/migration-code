//package providers.stripe;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.feth.play.module.pa.exceptions.AccessTokenException;
//import com.feth.play.module.pa.exceptions.AuthException;
//import com.feth.play.module.pa.providers.oauth2.OAuth2AuthProvider;
//import com.feth.play.module.pa.user.AuthUserIdentity;
//import com.google.inject.Inject;
//import play.Application;
//import play.Logger;
//import play.libs.ws.WSResponse;
//import plugins.StripeServicePlugin;
//
///**
// * Created by andrewcoleman on 11/29/16.
// * Handles authorization for Stripe
// */
//public class StripeAuthProvider extends OAuth2AuthProvider<StripeAuthUser,StripeAuthInfo>{
//    public static final String CLIENT_ID_KEY = "clientId";
//    public static final String PROVIDER_KEY = "stripe";
//    public static final String URL_KEY = "userInfoUrl";
//    private static final String REDIRECT_URL = "http://localhost:9000/mainFeed";
//
//    // Use this value for REDIRECT_URL in local development
//    // and put same URL in your Stripe App page
//    // private static final String CALLBACK_URL =
//    // "http://localhost:9000/authenticate/stripe";
//
//    private final StripeServicePlugin service;
//
//
//
//
//    @Inject
//    public StripeAuthProvider(Application app, StripeServicePlugin service) {
//        super(app);
//        this.service = service;
//    }
//
//    @Override
//    protected StripeAuthInfo buildInfo(WSResponse r) throws AccessTokenException {
//        if (r.getStatus() >= 400) {
//            throw new AccessTokenException(r.toString());
//        } else {
//            final JsonNode result = r.asJson();
//            Logger.debug(result.asText());
//            return new StripeAuthInfo(result.get(
//                    OAuth2AuthProvider.Constants.ACCESS_TOKEN).asText());
//        }
//    }
//
//    @Override
//    protected AuthUserIdentity transform(StripeAuthInfo info, String state) throws AuthException {
//
//        // Get the user info
//        final JsonNode result = service.getUserInfo(info);
//
//        // Create the webhooks
//        for(JsonNode n : service.getLists(info)) {
//            service.createWebhook(info, n.get("id").asLong());
//        }
//
//        return new StripeAuthUser(result, info, state);
//
//    }
//
//    @Override
//    protected String getRedirectUriKey() {
//        // Attention: This is redirect_urL instead of the normal redirect_urI
//        return REDIRECT_URL;
//    }
//
//
//    @Override
//    public String getKey() {
//        return PROVIDER_KEY ;
//    }
//}
