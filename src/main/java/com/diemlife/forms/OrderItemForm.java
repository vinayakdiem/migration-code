package com.diemlife.forms;

import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class OrderItemForm implements Serializable {

    @Required
    private String skuId;
    @Required
    @Min(1)
    private int quantity;

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(final String skuId) {
        this.skuId = skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(final int quantity) {
        this.quantity = quantity;
    }

}
