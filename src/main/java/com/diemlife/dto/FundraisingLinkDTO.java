package com.diemlife.dto;

import java.io.Serializable;
import java.util.Date;

public class FundraisingLinkDTO implements Serializable {

    public Long id;
    public UserDTO doer;
    public UserDTO creator;
    public BrandConfigDTO brand;
    public BrandConfigDTO secondaryBrand;
    public QuestDTO quest;
    public String campaignName;
    public String coverImageUrl;
    public Integer targetAmount;
    public Long currentAmount;
    public String currency;
    public Integer timesBacked;
    public boolean displayBtn;
    public boolean active;
    public Date endDate;
    public Long ticketTotalAmount;

}
