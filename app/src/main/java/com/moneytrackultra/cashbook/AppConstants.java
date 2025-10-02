package com.moneytrackultra.cashbook;

public final class AppConstants {
    private AppConstants(){}

    public static final String PREF_FILE = "cashbook_prefs";

    public static final String PREF_KEY_USER = "pref_user";
    public static final String PREF_KEY_PENDING_PROFILE = "pref_pending_profile";
    public static final String PREF_KEY_WALLETS = "pref_wallets";
    public static final String PREF_KEY_TRANSACTIONS = "pref_transactions";
    public static final String PREF_KEY_BUYING_LIMITS = "pref_buying_limits";
    public static final String PREF_KEY_WEEKLY_AGG = "pref_weekly_agg";
    public static final String PREF_KEY_LAST_SEED_VERSION = "pref_seed_version";

    public static final String PREF_KEY_PASSWORD_HASH = "pref_password_hash";
    public static final String PREF_KEY_PASSWORD_SALT = "pref_password_salt";

    public static final String PREF_KEY_CURRENCY = "pref_currency";
    public static final String PREF_KEY_PROVIDER = "pref_auth_provider"; // EMAIL / GOOGLE / FACEBOOK
    public static final String PREF_KEY_SOFT_LOGGED_OUT = "pref_soft_logged_out";

    public static final int CURRENT_SEED_VERSION = 1;
}