package com.moneytrackultra.cashbook;

import java.util.Calendar;

/**
 * Utility for period expiration and formatting.
 */
public final class LimitUtil {
    private LimitUtil(){}

    public static boolean isPeriodExpired(BuyingLimit limit, long nowMillis){
        if (limit == null) return false;
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(limit.periodStartMillis);
        Calendar end = (Calendar) start.clone();
        switch (limit.frequency){
            case DAILY:
                end.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case WEEKLY:
                end.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MONTHLY:
                end.add(Calendar.MONTH, 1);
                break;
            case YEARLY:
                end.add(Calendar.YEAR, 1);
                break;
        }
        return nowMillis >= end.getTimeInMillis();
    }

    public static long newPeriodStart(BuyingLimitFrequency freq){
        // Could normalize to midnight; for simplicity current time
        return System.currentTimeMillis();
    }

    public static String humanFrequency(BuyingLimitFrequency f){
        switch (f){
            case DAILY: return "Daily";
            case WEEKLY: return "Weekly";
            case MONTHLY: return "Monthly";
            case YEARLY: return "Yearly";
            default: return f.name();
        }
    }

    public static String formatLimit(long amount){
        if (amount <= 0) return "No Limit";
        return CurrencyUtil.rupiah(amount);
    }

    public static String formatUsage(long used, long limit){
        if (limit <= 0) {
            return "Used: " + CurrencyUtil.rupiah(used);
        }
        return "Used: " + CurrencyUtil.rupiah(used) + " / " + CurrencyUtil.rupiah(limit);
    }
}