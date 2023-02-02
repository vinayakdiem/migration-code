package com.diemlife.dto;

public class PlanCreationDTO extends StripeDTO {

    public enum Interval {
        day, week, month, year
    }

    public String currency;
    public Interval interval = Interval.month;
    public Integer intervalCount;
    public Integer amount;
    public ServiceProduct product;

    public static class ServiceProduct extends StripeDTO {
        public String name;
        public String statementDescriptor;
    }

}
