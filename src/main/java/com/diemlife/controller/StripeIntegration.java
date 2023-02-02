package com.diemlife.controller;

import com.stripe.*;
import com.stripe.model.Recipient;
import com.stripe.model.Transfer;
import play.Logger;
import play.api.data.Form;

import java.util.HashMap;
import java.util.Map;

import static play.mvc.Controller.request;

/**
 * Created by andrewcoleman on 6/27/16.
 */
public class StripeIntegration {



/*    public static Recipient createAndReturnRecipient(String name, String tokenId, String email) {

        // Set your secret key: remember to change this to your live secret key in production
        // See your keys here https://dashboard.stripe.com/account/apikeys
        Stripe.apiKey = "pk_live_xUWzcauaIvhznmbO99HdQRVf";

        // Get the card details submitted by the form
        String tokenID = tokenId;

        Recipient recipient = null;

        // Create a Recipient
        Map<String, Object> recipientParams = new HashMap<String, Object>();
        recipientParams.put("name", "John Doe");
        recipientParams.put("type", "individual");
        recipientParams.put("card", tokenID);
        recipientParams.put("email", "payee@example.com");

        try {
            recipient = Recipient.create(recipientParams);
            return recipient;
        } catch (Exception e) {
            Logger.error("Error creating Stripe recipient: " + e,e);
            return null;
        } finally {
            createAndReturnTransfer(recipient);
        }
    }

    public static Transfer createAndReturnTransfer(Recipient recipient) {
        // Set your secret key: remember to change this to your live secret key in production
        // See your keys here https://dashboard.stripe.com/account/apikeys
        Stripe.apiKey = "pk_live_xUWzcauaIvhznmbO99HdQRVf";

        // Create a transfer to the specified recipient
        Map<String, Object> transferParams = new HashMap<String, Object>();
        transferParams.put("amount", 1000); // amount in cents
        transferParams.put("currency", "usd");
        transferParams.put("recipient", recipient.getId());
        transferParams.put("card", recipient.getDefaultCard());
        transferParams.put("statement_descriptor", "DIEMlife");

        try {
            Transfer transfer = Transfer.create(transferParams);
            return transfer;
        } catch (Exception e) {
            Logger.error("Error creating a transfer: " + e,e);
            return null;
        }
    }*/
}
