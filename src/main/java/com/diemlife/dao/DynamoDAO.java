package dao;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;

import com.typesafe.config.Config;

import play.Logger;

public abstract class DynamoDAO {
    
    // Note: our config files typically have these in all caps
    private static final String ENV_PROD = "PROD";
    private static final String ENV_DEV = "DEV";
    private static final String ENV_LOCAL = "LOCAL";
    
    private static final String APP_MODE = "application.mode";
    private static final String AWS_ENDPOINT = "aws.dynamodb.endpoint";
    private static final String AWS_REGION = "aws.dynamodb.region";
    private static final String AWS_ACCESS_KEY = "aws.dynamodb.access.key";
    private static final String AWS_SECRET_KEY = "aws.dynamodb.secret.key";

    protected String tableName;
    protected AmazonDynamoDB client;
    protected AmazonDynamoDBClient _client;
    protected DynamoDB docClient;
    protected Table table;
    
    // TODO: consider switching to implicit credentials, which can take advantage of environment variables locally and IAM roles
    // when run in the cloud.
    public DynamoDAO(Config conf, String baseTableName) {
        this(createClient(conf), constructTableName(conf, baseTableName));
    }
    
    public DynamoDAO(AmazonDynamoDB client, String fullTableName) {
        this.tableName = fullTableName;
        this.client = client;
        this._client = (AmazonDynamoDBClient) client;
        this.docClient = new DynamoDB(client);
        this.table = this.docClient.getTable(fullTableName);
    }
    
    public static String constructTableName(Config conf, String baseTableName) {
        return constructTableName(conf.getString(APP_MODE), baseTableName);
    }
    
    public static String constructTableName(String env, String baseTableName) {
        StringBuilder sb = new StringBuilder();
        switch (env) {
            case ENV_PROD:
                // do nothing
                break;
            case ENV_DEV:
                sb.append("dev_");
                break;
            case ENV_LOCAL:
            default:
                // TODO: use local mode later on
                //sb.append("local_");
                sb.append("dev_");
                break;
        }
        sb.append(baseTableName);
        
        return sb.toString();
    }
    
    public static AmazonDynamoDB createClient(Config conf) {
        return createClient(conf.getString(AWS_REGION), conf.getString(AWS_ACCESS_KEY), conf.getString(AWS_SECRET_KEY));
    }
    
    // TODO: add endpoint support so that we can use DynamoDB local at some point for local dev work
    public static AmazonDynamoDB createClient(String region, String accessKey, String secretKey) {
        Logger.debug("createClient - " + region + ", " + accessKey);
        
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        builder.setRegion(region);
        builder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        
        return builder.build();
    }
}
