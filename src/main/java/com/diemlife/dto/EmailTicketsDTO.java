package com.diemlife.dto;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.tuple.Pair;

import com.diemlife.models.Quests;
import com.diemlife.models.UserEmailPersonal;

import services.StripeConnectService.ExportedProductVariant;

public class EmailTicketsDTO extends EmailDTO {

    public String orderNumber;
    public Integer orderQuantity;
    public Date orderDate;
    public Long orderTotal;
    public Long orderFees;
    public Long orderDiscount;
    public String questUrl;
    public Quests quest;
    public UserEmailPersonal user;
    public Collection<Pair<ExportedProductVariant, Integer>> tickets;
    public StripeShippingDTO buyer;

}
