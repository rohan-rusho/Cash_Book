package com.moneytrackultra.cashbook;

/**
 * Unified Transaction model.
 *
 * Canonical representation:
 *  - amountMinor: ALWAYS the source of truth (integer minor units: cents/paisa).
 *  - amount: legacy whole (major) unit field kept ONLY for backward compatibility (do not write directly).
 *
 * Conventions assumed elsewhere in the app:
 *  - All amounts are stored as positive numbers; TransactionType distinguishes INCOME vs EXPENSE.
 *    (If you ever change to signed amounts, centralize sign logic in helpers, not all over the codebase.)
 *  - Title doubles as "category" on chart screens. If blank and type == INCOME, category defaults to "Income".
 *
 * Migration notes:
 *  - When loading legacy JSON that had only `amount` (major units), call `updateFromLegacyMajor(amount)`.
 *  - When saving NEW data, persist both fields for forward + backward usage (until legacy code is gone).
 */
public class Transaction {

    // -------- Core Fields --------
    public long id;
    public TransactionType type;

    // Canonical precise amount (minor units)
    private long amountMinor;

    // Legacy major unit field (DO NOT set directly; use sync helpers)
    public long amount;

    public long epochMillis;   // precise timestamp (UTC recommended)
    public String title;       // user-visible name / category
    public String source;      // platform / merchant (optional)
    public String date;        // optional human string ("yyyy-MM-dd" or friendly); NOT the authority for time

    // -------- Constants / Defaults --------
    private static final int MINOR_SCALE = 100;             // factor: 1 major = 100 minor (override if needed)
    private static final String DEFAULT_INCOME_TITLE  = "Income";
    private static final String DEFAULT_EXPENSE_TITLE = "Expense";

    // -------- Constructors --------

    /**
     * Full constructor using minor units directly.
     */
    public Transaction(long id,
                       TransactionType type,
                       long amountMinor,
                       long epochMillis,
                       String title,
                       String source,
                       String date) {
        this.id = id;
        this.type = type;
        setAmountMinor(amountMinor); // ensures sync
        this.epochMillis = epochMillis;
        this.title = title;
        this.source = source;
        this.date = date;
    }

    /**
     * Convenience for when you only have minor units and no extra metadata.
     */
    public Transaction(long id,
                       TransactionType type,
                       long amountMinor,
                       long epochMillis,
                       String title) {
        this(id, type, amountMinor, epochMillis, title, null, null);
    }

    /**
     * Factory for code that still supplies major units (whole currency).
     */
    public static Transaction fromMajor(long id,
                                        TransactionType type,
                                        long amountMajor,
                                        long epochMillis,
                                        String title,
                                        String source,
                                        String date) {
        return new Transaction(id, type, amountMajor * MINOR_SCALE, epochMillis, title, source, date);
    }

    /**
     * Legacy migration helper when only a major-unit 'amount' was loaded.
     * Example: after parsing older JSON: new Transaction(...).updateFromLegacyMajor(parsedAmount);
     */
    public Transaction updateFromLegacyMajor(long legacyMajor) {
        setAmountMajor(legacyMajor);
        return this;
    }

    // -------- Amount Helpers --------

    /**
     * Canonical setter (ALWAYS sets both amountMinor and legacy amount).
     */
    public void setAmountMinor(long newMinor) {
        this.amountMinor = newMinor;
        this.amount = newMinor / MINOR_SCALE; // integer truncation — acceptable for legacy display
    }

    /**
     * Alternate setter from major (whole) units.
     */
    public void setAmountMajor(long major) {
        this.amountMinor = major * MINOR_SCALE;
        this.amount = major;
    }

    /**
     * Get canonical minor units.
     */
    public long getAmountMinor() {
        return amountMinor;
    }

    /**
     * Get a rounded major unit (legacy style). Uses simple floor division.
     * If you need rounding (e.g., 125 -> 1 vs 1.25): adjust logic here.
     */
    public long getAmountMajorRounded() {
        return amount;
    }

    /**
     * Optional: precise major as double (for formatting if you later allow sub-cent scaling).
     */
    public double getAmountMajorExact() {
        return amountMinor / (double) MINOR_SCALE;
    }

    // -------- Category / Title Logic --------

    /**
     * Returns the canonical category for charts:
     *  - If title is non-blank, trimmed title.
     *  - If blank & INCOME -> "Income"
     *  - If blank & EXPENSE -> "Expense" (change if you prefer to still use Income or show "(No Title)")
     */
    public String category() {
        if (title != null) {
            String t = title.trim();
            if (!t.isEmpty()) return t;
        }
        return type == TransactionType.INCOME ? DEFAULT_INCOME_TITLE : DEFAULT_EXPENSE_TITLE;
    }

    /**
     * Returns a safe display label (category + optional source).
     */
    public String displayLabel() {
        if (source != null && !source.trim().isEmpty()) {
            return category() + " • " + source.trim();
        }
        return category();
    }

    // -------- Builders / Copy --------

    public Transaction copy() {
        Transaction t = new Transaction(
                this.id,
                this.type,
                this.amountMinor,
                this.epochMillis,
                this.title,
                this.source,
                this.date
        );
        return t;
    }

    /**
     * Simple incremental builder pattern (chained config).
     */
    public Transaction withTitle(String newTitle) {
        this.title = newTitle;
        return this;
    }

    public Transaction withSource(String newSource) {
        this.source = newSource;
        return this;
    }

    public Transaction withDate(String newDate) {
        this.date = newDate;
        return this;
    }

    public Transaction withEpoch(long newEpochMillis) {
        this.epochMillis = newEpochMillis;
        return this;
    }

    public Transaction withType(TransactionType newType) {
        this.type = newType;
        return this;
    }

    public Transaction withAmountMinor(long newMinor) {
        setAmountMinor(newMinor);
        return this;
    }

    public Transaction withAmountMajor(long newMajor) {
        setAmountMajor(newMajor);
        return this;
    }

    // -------- Debug / Logging --------

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type=" + type +
                ", amountMinor=" + amountMinor +
                ", amount(legacy)=" + amount +
                ", epochMillis=" + epochMillis +
                ", title='" + title + '\'' +
                ", source='" + source + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}