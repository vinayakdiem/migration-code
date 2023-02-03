package com.diemlife.plugins;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import play.Application;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Amazon S3 compatible service plugin to store files. This plugin must be
 * compatible with Amazon S3 and Eucalyptus Walrus.
 */
@Singleton
public class S3Plugin {

    private static final String AWS_S3_BUCKET = "aws.s3.bucket";
    private static final String AWS_ENDPOINT = "s3.amazonaws.com";
    private static final String AWS_ACCESS_KEY = "aws.access.key";
    private static final String AWS_SECRET_KEY = "aws.secret.key";

    /**
     * Instance of the S3 API client
     */
    public static AmazonS3 amazonS3;

    /**
     * Name of the S3 bucket to use.
     */
    public static String s3Bucket;

    /**
     * Instance of the current started application.
     */

    private final Application application;

    /**
     * Default constructor.
     *
     * @param application The current instance of the application
     * @see Application
     */
    @Inject
    public S3Plugin(Application application) {
        Logger.info("Injecting s3plugin to application");
        this.application = application;
        onStart();
        Logger.info("Post injection of s3plugin to application");
    }

    /**
     * Initialize the Amazon S3 bucket. This method will create the bucket if
     * it does not exist.
     */
    public void onStart() {
        String accessKey = application.configuration().getString(AWS_ACCESS_KEY);
        String secretKey = application.configuration().getString(AWS_SECRET_KEY);
        s3Bucket = application.configuration().getString(AWS_S3_BUCKET);
        Logger.info("Got all connection details. Attempting to check on bucket status");

        if ((accessKey != null) && (secretKey != null)) {
            Logger.info("accessKey and secretKey != null. Opening connection");
            AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            Logger.info("creating amazonS3 bucket.");
            amazonS3 = new AmazonS3Client(awsCredentials);
            amazonS3.createBucket(s3Bucket);
            Logger.info(amazonS3.getBucketLocation("diemlife-images"));
            Logger.info("Using S3 Bucket: " + s3Bucket);
        }
    }

    /**
     * Check if the Plugin can be enabled.
     *
     * @return true in case of success, otherwise false
     */
    public boolean enabled() {
        Logger.info("application is enabled. Returning.");
        return (this.application.configuration().keys().contains(S3Plugin.AWS_ACCESS_KEY) &&
                this.application.configuration().keys().contains(S3Plugin.AWS_SECRET_KEY) &&
                this.application.configuration().keys().contains(S3Plugin.AWS_S3_BUCKET) &&
                this.application.configuration().keys().contains(S3Plugin.AWS_ENDPOINT));
    }
}
