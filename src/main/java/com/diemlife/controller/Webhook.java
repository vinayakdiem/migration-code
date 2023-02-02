package com.diemlife.controller;

import com.typesafe.config.ConfigFactory;
import play.mvc.Controller;
import play.mvc.Result;
import security.JwtSessionLogin;

import java.util.Map;

/**
 * Created by andrewcoleman on 11/30/16.
 */
@JwtSessionLogin
public class Webhook extends Controller {

    @JwtSessionLogin
    public Result stripe() {

        Map<String, String[]> a = request().queryString();
        System.out.println(a);
        if(!a.containsKey("response_type") || !a.containsKey("client_id") || !a.containsKey("scope")) {
            System.out.println("Missing stuff?");
            return noContent();
        }

        String challenge = a.get("response_type")[0];
        String token = a.get("client_id")[0];
        String scope = a.get("scope")[0];

        System.out.println("Does token equal config = " + ConfigFactory.load().getConfig("play-authenticate").getConfig("stripe").getString("verify_token"));
        if(token.equals(ConfigFactory.load().getConfig("play-authenticate").getConfig("stripe").getString("verify_token"))) {
            System.out.println("Not missing anything");
            System.out.println(challenge);
            return ok(challenge);
        }
        System.out.println("Returning no content => " + token.toString());
        return noContent();
    }
}
