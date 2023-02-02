//package providers.stripe;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.feth.play.module.pa.providers.oauth2.BasicOAuth2AuthUser;
//import com.feth.play.module.pa.user.EmailIdentity;
//import com.feth.play.module.pa.user.NameIdentity;
///**
// * Created by andrewcoleman on 11/29/16.
// */
//public class StripeAuthUser extends BasicOAuth2AuthUser implements EmailIdentity, NameIdentity{
//    /**
//     * From https://developers.google.com/accounts/docs/OAuth2Login#userinfocall
//     */
//    private static class Constants {
//        public static final String ID = "id"; // "00000000000000",
//        public static final String EMAIL = "email"; // "fred.example@gmail.com",
//        public static final String NAME = "name"; // "Fred Example",
//    }
//
//    private String email;
//    private String name;
//
//
//    public StripeAuthUser(final JsonNode n, final StripeAuthInfo info, final String state){
//        super(n.get(Constants.ID).asText(), info, state);
//
//        if(n.has(Constants.EMAIL)) {
//            this.email = n.get(Constants.EMAIL).asText();
//        }
//
//        if(n.has(Constants.NAME)) {
//            this.name = n.get(Constants.NAME).asText();
//        }
//
//
//    }
//
//    @Override
//    public String getEmail() {
//        return email;
//    }
//
//    @Override
//    public String getProvider() {
//        return StripeAuthProvider.PROVIDER_KEY ;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }
//
//}
