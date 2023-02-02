package com.diemlife.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

// TODO: delete this class if it is not needed ==> it is only used by unit tests

@Getter
@Setter
public class LeaderboardAttributeConfig implements Serializable {

    private String attributeId;
    private int eventId;
    private String stripeSkuId;

    public LeaderboardAttributeConfig(String attributeId, int eventId, String stripeSkuId) {
        this.attributeId = attributeId;
        this.eventId = eventId;
        this.stripeSkuId = stripeSkuId;
    }
}
