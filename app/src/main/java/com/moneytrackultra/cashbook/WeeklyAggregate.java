package com.moneytrackultra.cashbook;

/**
 * Weekly aggregate for a single transaction type (INCOME or EXPENSE).
 *
 * Conventions:
 *  - weekIndex is 1-based (Week 1, Week 2, ...)
 *  - amountMinor stores summed value in minor units (e.g. cents / paisa).
 *  - label optional: if null adapter will render "W" + weekIndex.
 *  - fromEpoch/toEpoch (optional) let you debug or drill down (toEpoch exclusive).
 *  - txCount is number of transactions contributing (optional, can be ignored by UI).
 */
public class WeeklyAggregate {
    public int weekIndex;        // 1-based index within the month
    public long amountMinor;     // summed amount in minor units
    public TransactionType type; // INCOME or EXPENSE
    public String label;         // optional custom label
    public long fromEpoch;       // start of week (inclusive) - optional
    public long toEpoch;         // end of week (exclusive) - optional
    public int txCount;          // number of transactions in this week (optional)

    public WeeklyAggregate() {}

    public WeeklyAggregate(int weekIndex,
                           long amountMinor,
                           TransactionType type,
                           String label) {
        this.weekIndex = weekIndex;
        this.amountMinor = amountMinor;
        this.type = type;
        this.label = label;
    }

    public String effectiveLabel() {
        return (label != null && !label.isEmpty()) ? label : "W" + weekIndex;
    }
}