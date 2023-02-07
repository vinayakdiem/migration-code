package com.diemlife.dao;

import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.document.Item;

import com.typesafe.config.Config;

@Repository
public class StripeOrderDAO extends DynamoDAO {
    
    private static final String EMAIL = "email";
    private static final String TIMESTAMP = "timestamp";
    private static final String QUEST_ID = "questId";
    private static final String EVENT_ID = "eventId";
    private static final String STRIPE_PRODUCT_ID = "stripeProductId";
    private static final String STRIPE_MERCHANT_ID = "stripeMerchantId";
    private static final String STRIPE_CUSTOMER_ID = "stripeCustomerId";
    private static final String STRIPE_PAYMENT_ID = "stripePaymentId";
    private static final String ORDER_DETAIL = "orderDetail";
    private static final String TRANSACTION_BREAKDOWN = "transactionBreakdown";
    
    private static final String HASH_KEY = EMAIL;
    private static final String RANGE_KEY = TIMESTAMP;
    
    public StripeOrderDAO(Config conf) {
        super(conf, "stripe_order");
    }
    
    public boolean insert(String email, long timestamp, long questId, long eventId, String stripeProductId, String stripeMerchantId, String stripeCustomerId,
        String stripePaymentId, String orderDetail, String transactionBreakdown)
    {
        Item item = new Item();
        item.withPrimaryKey(HASH_KEY, email, RANGE_KEY, timestamp);
        item.withNumber(QUEST_ID, questId);
        item.withNumber(EVENT_ID, eventId);
        
        if (stripeProductId != null) {
            item.withString(STRIPE_PRODUCT_ID, stripeProductId);
        }
        
        item.withString(STRIPE_MERCHANT_ID, stripeMerchantId);
        
        if (stripeCustomerId != null) {
            item.withString(STRIPE_CUSTOMER_ID, stripeCustomerId);
        }
        
        item.withString(STRIPE_PAYMENT_ID, stripePaymentId);
        item.withString(ORDER_DETAIL, orderDetail);
        
        if (transactionBreakdown != null) {
            item.withString(TRANSACTION_BREAKDOWN, transactionBreakdown);
        }
        
        // FIXME: this needs to check the result to ensure success
        this.table.putItem(item);
        
        return true;
    }
}
