package com.diemlife.services;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;
import play.api.Play;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Created by andrewcoleman on 4/21/17.
 */
public class AmazonSESService {

    private static final String FROM = "DIEMlife <admin@diemlife.com>";   //This address must be verified via AWS
    private static final String BCC = "admin@diemlife.com";

    @Inject
    private static final MessagesApi messagesApi = Play.current().injector().instanceOf(MessagesApi.class);

    public static void sendEmail(String SMTP_USERNAME, String SMTP_PASSWORD, String toEmail, String ccEmail,
                                 String bccEmail, String htmlBody, String textBody, String subject) {

        try {
            BasicAWSCredentials credentials = new BasicAWSCredentials(SMTP_USERNAME, SMTP_PASSWORD);
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard()
                            .withCredentials(new AWSStaticCredentialsProvider(credentials))
                            .withRegion(Regions.US_EAST_1).build();

            SendEmailRequest request = builder()
                    .withToEmail(toEmail)
                    .withBCCEmail(bccEmail)
                    .withCCEmail(ccEmail)
                    .withHtmlBody(htmlBody)
                    .withTextBody(textBody)
                    .withSubject(subject)
                    .build();

            client.sendEmail(request);
            Logger.info("AmazonSESService :: sendEmail : Email sent to - " + toEmail);
            Logger.info("AmazonSESService :: sendEmail : BCC sent to - " + bccEmail);
            Logger.info("AmazonSESService :: sendEmail : CC sent to - " + ccEmail);
        } catch (RuntimeException ex) {
            Logger.error("AmazonSESService :: sendEmail : failed to send email => " + ex, ex);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String toEmail;
        private String ccEmail;
        private String bccEmail;
        private String htmlBody;
        private String textBody;
        private String subject;

        /**
         * Use the static method on SendEmailRequest to get a builder instance.
         */
        private Builder() {

        }

        public Builder withToEmail(String toEmail) {
            this.toEmail = toEmail;
            return this;
        }

        public Builder withCCEmail(String ccEmail) {
            this.ccEmail = ccEmail;
            return this;
        }

        public Builder withBCCEmail(String bccEmail) {
            this.bccEmail = bccEmail;
            return this;
        }

        public Builder withHtmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }

        public Builder withTextBody(String textBody) {
            this.textBody = textBody;
            return this;
        }

        public Builder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public SendEmailRequest build() {
            Destination destination;
            if (this.ccEmail != null) {
                destination = new Destination()
                        .withToAddresses(toEmail)
                        .withBccAddresses(bccEmail)
                        .withCcAddresses(ccEmail);
            } else {
                destination = new Destination()
                        .withToAddresses(toEmail)
                        .withBccAddresses(bccEmail);
            }
            return new SendEmailRequest()
                    .withDestination(destination)
                    .withMessage(new Message().withBody(new Body()
                            .withHtml(new Content().withCharset("UTF-8").withData(htmlBody))
                            .withText(new Content().withCharset("UTF-8").withData(textBody)))
                            .withSubject(new Content().withCharset("UTF-8").withData(subject)))
                    .withSource(FROM);
        }
    }

    public static void sendFriendRequestEmail(final Http.RequestHeader request, String username, String password, String user, String email, String toFriend) {

        final Messages message = messagesApi.preferred(request);

        String subject = message.at("email.friend.request.subject", user);

        String htmlBody = message.at("email.friend.request.body.html",
                user,
                toFriend);

        String textBody = message.at("email.friend.request.body.text",
                user,
                toFriend);

        String toEmail = email;
        String bccEmail = BCC;

        try {
            sendEmail(username, password, toEmail, null, bccEmail, htmlBody, textBody, subject);
        } catch (Exception ex) {
            Logger.error("AmazonSESService :: sendFriendRequestEmail : failed to send friend request notification => " + ex, ex);
        }
    }

    public static void sendAddCreditCardConfirmation(final Http.RequestHeader request, String username, String password, User user) {

        final Messages message = messagesApi.preferred(request);

        String subject = message.at("email.stripe.add.credit.card.subject");

        String htmlBody = message.at("email.stripe.add.credit.card.body.html");

        String textBody = message.at("email.stripe.add.credit.card.body.text");

        String toEmail = user.getEmail();
        String bccEmail = BCC;

        try {
            sendEmail(username, password, toEmail, null, bccEmail, htmlBody, textBody, subject);
        } catch (Exception ex) {
            Logger.info("AmazonSESService :: sendAddCreditCardConfirmation : failed to send add credit card email => " + ex, ex);
        }

    }

    public static void sendDeleteCreditCardConfirmation(final Http.RequestHeader request, String username, String password, User user) {

        final Messages message = messagesApi.preferred(request);

        String subject = message.at("email.stripe.delete.credit.card.subject");

        String htmlBody = message.at("email.stripe.delete.credit.card.body.html");

        String textBody = message.at("email.stripe.delete.credit.card.body.text");

        String toEmail = user.getEmail();
        String bccEmail = BCC;

        try {
            sendEmail(username, password, toEmail, null, bccEmail, htmlBody, textBody, subject);
        } catch (Exception ex) {
            Logger.info("AmazonSESService :: sendDeleteCreditCardConfirmation : failed to send remove credit card email => " + ex, ex);
        }
    }

    public static void sendOrderTicketConfirmationWithEmail(final Http.RequestHeader request, Quests quest, String eventEmail, String purchaserEmail,
                                                            String awsUser, String awsPass) {

        final Messages message = messagesApi.preferred(request);

        String subject = message.at("email.ticket.default.subject", quest);

        String htmlBody = eventEmail;

        String textBody = eventEmail;

        String toEmail = purchaserEmail;

        String ccEmail = quest.getUser().getEmail();

        String bccEmail = BCC;

        try {
            sendEmail(awsUser, awsPass, toEmail, ccEmail, bccEmail, htmlBody, textBody, subject);
        } catch (Exception ex) {
            Logger.info("AmazonSESService :: sendOrderTicketConfirmation : failed to send ticket default email => " + ex, ex);
        }
    }


    public static void sendPasswordResetConfirmation(final Http.RequestHeader request, String awsUser, String awsPass, User user) {

        final Messages message = messagesApi.preferred(request);

        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        String formattedDate = dateFormat.format(date);

        String subject = message.at("playauthenticate.reset_password.message.success.auto_login");

        String htmlBody = message.at("email.password.reset.body.html", user.getFirstName(), formattedDate);

        String textBody = message.at("email.password.reset.body.text");

        String toEmail = user.getEmail();
        String bccEmail = BCC;

        try {
            sendEmail(awsUser, awsPass, toEmail, null, bccEmail, htmlBody, textBody, subject);
        } catch (Exception ex) {
            Logger.info("AmazonSESService :: sendPasswordResetConfirmation : failed to send password reset confirmation email => " + ex, ex);
        }
    }

    public static void sendAdminEmail(String awsUser, String awsPass, String subject, String body, String userName) {

        String newSubject =  "New user signup: " + userName;

        try {
            sendEmail(awsUser, awsPass, BCC, null, null, body, body, newSubject);
        } catch (Exception ex) {
            Logger.info("AmazonSESService :: sendAdminEmail : failed to send admin email for new user => " + ex, ex);
        }
    }
}