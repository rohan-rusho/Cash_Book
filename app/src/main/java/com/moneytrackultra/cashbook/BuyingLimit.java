package com.moneytrackultra.cashbook;

public class BuyingLimit {
    public String platform;
    public long limitAmount;              // in major units (whole)
    public BuyingLimitFrequency frequency;
    public long usedInPeriod;             // usage accumulated in same period
    public long createdAtEpoch;
    public long periodStartMillis;

    public BuyingLimit() {}

    public BuyingLimit(String platform,
                       long limitAmount,
                       BuyingLimitFrequency frequency,
                       long usedInPeriod,
                       long createdAtEpoch) {
        this.platform = platform;
        this.limitAmount = limitAmount;
        this.frequency = frequency;
        this.usedInPeriod = usedInPeriod;
        this.createdAtEpoch = createdAtEpoch;
    }
}