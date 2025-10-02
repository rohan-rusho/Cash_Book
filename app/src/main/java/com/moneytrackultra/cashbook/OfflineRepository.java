package com.moneytrackultra.cashbook;

import java.util.ArrayList;
import java.util.List;

public class OfflineRepository {

    private static OfflineRepository INSTANCE;

    public static OfflineRepository get() {
        if (INSTANCE == null) INSTANCE = new OfflineRepository();
        return INSTANCE;
    }

    private OfflineRepository() {}

    // Wallets
    public List<Wallet> getWallets() {
        return PrefsManager.get().getWallets();
    }

    // Filter transactions by type + optional source + optional date
    public List<Transaction> getTransactionsByType(String source, String date, TransactionType type) {
        List<Transaction> all = PrefsManager.get().getTransactions();
        List<Transaction> out = new ArrayList<>();
        if (all == null) return out;
        for (Transaction t : all) {
            if (t == null) continue;
            if (t.type == type &&
                    (source == null || (t.source != null && t.source.equalsIgnoreCase(source))) &&
                    (date == null || (t.date != null && t.date.equalsIgnoreCase(date)))) {
                out.add(t);
            }
        }
        return out;
    }

    public long sumAmount(List<Transaction> entries) {
        long total = 0;
        if (entries == null) return 0;
        for (Transaction t : entries) {
            if (t != null) total += t.amount;
        }
        return total;
    }

    public List<WeeklyAggregate> getWeekly(TransactionType type) {
        List<WeeklyAggregate> all = PrefsManager.get().getWeeklyAggregates();
        List<WeeklyAggregate> out = new ArrayList<>();
        if (all == null) return out;
        for (WeeklyAggregate w : all) {
            if (w != null && w.type == type) out.add(w);
        }
        return out;
    }

    // Buying limits
    public List<BuyingLimit> getBuyingLimits() {
        return PrefsManager.get().getBuyingLimits();
    }

    /**
     * Update the limitAmount for a given platform.
     * Recompute usedInPeriod percentage on the fly (not stored).
     */
    public void updateBuyingLimit(String platform, long newLimitAmount) {
        List<BuyingLimit> list = PrefsManager.get().getBuyingLimits();
        boolean changed = false;
        for (BuyingLimit b : list) {
            if (b != null && b.platform.equalsIgnoreCase(platform)) {
                b.limitAmount = newLimitAmount;
                // (Optional) clamp usedInPeriod if it now exceeds the new limit
                if (b.limitAmount > 0 && b.usedInPeriod > b.limitAmount) {
                    b.usedInPeriod = b.limitAmount;
                }
                changed = true;
                break;
            }
        }
        if (changed) {
            PrefsManager.get().saveBuyingLimits(list);
        }
    }

    /**
     * Convenience: compute percentage without storing an extra field.
     */
    public int getLimitUsagePercent(BuyingLimit b) {
        if (b == null || b.limitAmount <= 0) return 0;
        long used = b.usedInPeriod;
        if (used < 0) used = Math.abs(used);
        double pct = (double) used / (double) b.limitAmount * 100.0;
        if (pct > 100) pct = 100;
        return (int) pct;
    }

    /**
     * Increment usage (usedInPeriod) for a platform (e.g., when new expense recorded).
     * This does NOT handle period rollover (add logic if needed).
     */
    public void addUsage(String platform, long amount) {
        if (amount <= 0) return;
        List<BuyingLimit> list = PrefsManager.get().getBuyingLimits();
        boolean changed = false;
        for (BuyingLimit b : list) {
            if (b != null && b.platform.equalsIgnoreCase(platform)) {
                b.usedInPeriod += amount;
                if (b.limitAmount > 0 && b.usedInPeriod > b.limitAmount) {
                    b.usedInPeriod = b.limitAmount;
                }
                changed = true;
                break;
            }
        }
        if (changed) {
            PrefsManager.get().saveBuyingLimits(list);
        }
    }

    // User
    public User getUser() {
        return PrefsManager.get().getUser();
    }

    public void updateUserName(String newName) {
        if (newName == null || newName.trim().isEmpty()) return;
        User u = getUser();
        if (u != null) {
            u.displayName = newName.trim();
            PrefsManager.get().saveUser(u);
        }
    }
}