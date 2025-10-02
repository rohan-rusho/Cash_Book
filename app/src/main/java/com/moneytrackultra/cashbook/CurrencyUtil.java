package com.moneytrackultra.cashbook;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Currency formatting helper.
 *
 * Supports both "major" long units (e.g. amount = 720000 meaning 720,000 whole units)
 * and "minor" units (amountMinor = amountInMajor * 100) if you need cents/paisa later.
 *
 * Current default currency code (change with setCode or extend with preference storage).
 */
public class CurrencyUtil {

    private static String code = "BDT"; // default; change or persist if needed

    public static void setCode(String c) {
        if (c != null && !c.trim().isEmpty()) code = c.toUpperCase(Locale.US);
    }

    public static String symbol() {
        switch (code) {
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            case "INR": return "₹";
            case "BDT": return "৳";
            case "JPY": return "¥";
            default: return code + " ";
        }
    }

    /* ========= MAJOR UNIT FORMATTERS (no stored fractional minor units) ========= */

    /**
     * Format a long representing whole currency units with grouping (no forced decimals).
     * Example: 720000 -> ৳ 720,000
     */
    public static String formatMajor(long amountMajor) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setGroupingUsed(true);
        nf.setMaximumFractionDigits(0);
        return symbol() + " " + nf.format(amountMajor);
    }

    /**
     * Same as formatMajor but always prints two decimals (720000 -> ৳ 720,000.00).
     */
    public static String formatMajorWithDecimals(long amountMajor) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return symbol() + " " + nf.format(amountMajor);
    }

    /* ========= MINOR UNIT FORMATTER (if you store cents/paisa) ========= */

    /**
     * If you store amounts in minor units (e.g. cents) pass them here:
     * 123456 (cents) -> 1234.56 major units
     */
    public static String formatMinor(long amountMinor) {
        double major = amountMinor / 100.0;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return symbol() + " " + nf.format(major);
    }

    /* ========= BACKWARD COMPATIBILITY ========= */

    /**
     * Legacy alias for old code that called CurrencyUtil.rupiah().
     * Maps to formatMajor (no decimals). Change to formatMajorWithDecimals if you prefer.
     */
    public static String rupiah(long amountMajor) {
        // If you REALLY want to keep the old "Rp." prefix always, you could special-case here.
        return formatMajor(amountMajor);
    }
}