package com.diemlife.utils;

public class TransactionBreakdown {

    public long netTotal;
    public long netTip;
    public long brutTip;
    public long discount;
    public long platformFee;
    public long tax;
    public long stripeFee;
    public long brutTotal;

    public String toJson() {
        return "{ \"netTotal\": " + netTotal + ", \"netTip\": " + netTip + ", \"brutTip\": " + brutTip
                + ", \"discount\": " + discount + ", \"platformFee\": " + platformFee + ", \"tax\": " + tax + ", \"stripeFee\": " + stripeFee
                + ", \"bruTotal\": " + brutTotal + " }";
    }

    @Override
    public String toString() {
        return "TransactionBreakdown{" +
                "netTotal=" + netTotal +
                ", netTip=" + netTip +
                ", brutTip=" + brutTip +
                ", discount=" + discount +
                ", platformFee=" + platformFee +
                ", tax=" + tax +
                ", stripeFee=" + stripeFee +
                ", brutTotal=" + brutTotal +
                '}';
    }
}
