package com.moneytrackultra.cashbook;

/**
 * Transaction inside a specific Wallet.
 * amount: positive = income/top-up, negative = expense/shopping.
 */
public class WalletTransaction {
    public long id;
    public long amount;          // major units; negative for expenses if you follow that pattern
    public String title;         // short title/label
    public String source;        // platform / merchant (optional)
    public String date;          // simple date string shown in UI (e.g. "2025-10-02")
    public long epochMillis;     // precise timestamp
    public String note;          // optional note

    public WalletTransaction() {}

    public WalletTransaction(long id,
                             long amount,
                             String title,
                             String source,
                             String date,
                             long epochMillis,
                             String note) {
        this.id = id;
        this.amount = amount;
        this.title = title;
        this.source = source;
        this.date = date;
        this.epochMillis = epochMillis;
        this.note = note;
    }
}