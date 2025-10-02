package com.moneytrackultra.cashbook;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrefsManager {

    private static final String PREF = "app_prefs";

    // Keys
    private static final String KEY_USER_JSON    = "user_json";
    private static final String KEY_FIRST_LAUNCH_DONE = "first_launch_done";
    private static final String KEY_PROVIDER     = "auth_provider";
    private static final String KEY_SOFT_LOGOUT  = "soft_logout";
    private static final String KEY_PWD_SALT     = "pwd_salt";
    private static final String KEY_PWD_HASH     = "pwd_hash";
    private static final String KEY_TX           = "transactions_json";
    private static final String KEY_WALLETS      = "wallets_json";

    private static final String KEY_CURRENCY_CODE = "currency_code";
    private static final String KEY_PENDING_PROFILE_SYNC = "pending_profile_sync";
    private static final String KEY_WEEKLY       = "weekly_aggregates_json";
    private static final String KEY_BUYING       = "buying_limits_json";
    private static final String KEY_SEED_VERSION = "seed_version";

    private static PrefsManager INSTANCE;
    private final SharedPreferences sp;

    private PrefsManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void init(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new PrefsManager(ctx);
        }
    }

    public static PrefsManager get() {
        if (INSTANCE == null) throw new IllegalStateException("PrefsManager.init(context) not called");
        return INSTANCE;
    }

    /* ---------- User & Auth State ---------- */

    public void saveUser(User u) {
        if (u == null) {
            sp.edit().remove(KEY_USER_JSON).apply();
            return;
        }
        JSONObject o = new JSONObject();
        try {
            o.put("uid", u.uid);
            o.put("email", u.email);
            o.put("displayName", u.displayName);
            o.put("photoUrl", u.photoUrl);
            o.put("createdAtEpoch", u.createdAtEpoch);
        } catch (Exception e) { e.printStackTrace(); }
        sp.edit().putString(KEY_USER_JSON, o.toString()).apply();
    }

    public User getUser() {
        String raw = sp.getString(KEY_USER_JSON, null);
        if (raw == null) return null;
        try {
            JSONObject o = new JSONObject(raw);
            return new User(
                    o.optString("uid", null),
                    o.optString("email", null),
                    o.optString("displayName", null),
                    o.optString("photoUrl", null),
                    o.optLong("createdAtEpoch", 0L)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveProvider(String provider) {
        if (provider == null) sp.edit().remove(KEY_PROVIDER).apply();
        else sp.edit().putString(KEY_PROVIDER, provider).apply();
    }
    public String getProvider() { return sp.getString(KEY_PROVIDER, null); }

    public void setSoftLoggedOut(boolean value) { sp.edit().putBoolean(KEY_SOFT_LOGOUT, value).apply(); }
    public boolean isSoftLoggedOut() { return sp.getBoolean(KEY_SOFT_LOGOUT, false); }

    /* ---------- Local Password (offline) ---------- */

    public void saveLocalPassword(char[] plainPassword) {
        if (plainPassword == null) { clearLocalPassword(); return; }
        String salt = PasswordHashUtil.newSalt();
        String hash = PasswordHashUtil.hash(plainPassword, salt);
        sp.edit().putString(KEY_PWD_SALT, salt).putString(KEY_PWD_HASH, hash).apply();
    }
    public void clearLocalPassword() { sp.edit().remove(KEY_PWD_SALT).remove(KEY_PWD_HASH).apply(); }
    public String getPasswordSalt() { return sp.getString(KEY_PWD_SALT, null); }
    public String getPasswordHash() { return sp.getString(KEY_PWD_HASH, null); }

    /* ---------- Transactions ---------- */

    public List<Transaction> getTransactions() {
        String raw = sp.getString(KEY_TX, "[]");
        List<Transaction> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long id = o.optLong("id");
                TransactionType type = TransactionType.valueOf(o.optString("type", TransactionType.EXPENSE.name()));
                long amountMinor = o.has("amountMinor") ? o.optLong("amountMinor")
                        : (o.has("amount") ? o.optLong("amount") * 100 : 0);
                Transaction t = new Transaction(id, type, amountMinor, o.optLong("epochMillis"), o.optString("title", ""));
                if (o.has("amount")) t.amount = o.optLong("amount"); else t.amount = amountMinor / 100;
                t.source = o.optString("source", null);
                t.date   = o.optString("date", null);
                out.add(t);
            }
        } catch (Exception e) { e.printStackTrace(); }
        Collections.sort(out, (a,b) -> Long.compare(b.epochMillis, a.epochMillis));
        return out;
    }

    public void addTransaction(TransactionType type, double amountMajor, String title, String source, String date) {
        long amountMinor = Math.round(amountMajor * 100);
        Transaction t = new Transaction(System.currentTimeMillis(), type, amountMinor, System.currentTimeMillis(), title);
        t.amount = (long) amountMajor;
        t.source = source;
        t.date   = date;
        List<Transaction> list = getTransactions();
        list.add(0, t);
        saveTransactions(list);
    }

    public void addTransaction(TransactionType type, double amountMajor, String title) {
        addTransaction(type, amountMajor, title, null, null);
    }

    public void deleteTransaction(long id) {
        List<Transaction> list = getTransactions();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        saveTransactions(list);
    }

    // Alias if some code still calls deleteTransactionById
    public void deleteTransactionById(long id) { deleteTransaction(id); }

    public void replaceTransactions(List<Transaction> newList) {
        if (newList == null) newList = new ArrayList<>();
        Collections.sort(newList, (a,b) -> Long.compare(b.epochMillis, a.epochMillis));
        saveTransactions(newList);
    }

    private void saveTransactions(List<Transaction> list) {
        JSONArray arr = new JSONArray();
        for (Transaction t : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", t.id);
                o.put("type", t.type.name());
                o.put("amountMinor", t.getAmountMinor());
                o.put("amount", t.amount);
                o.put("epochMillis", t.epochMillis);
                o.put("title", t.title);
                if (t.source != null) o.put("source", t.source);
                if (t.date != null)   o.put("date", t.date);
            } catch (Exception e) { e.printStackTrace(); }
            arr.put(o);
        }
        sp.edit().putString(KEY_TX, arr.toString()).apply();
    }

    /* ---------- Wallets (optional) ---------- */

    public List<Wallet> getWallets() {
        String raw = sp.getString(KEY_WALLETS, "[]");
        List<Wallet> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i=0;i<arr.length();i++) {
                JSONObject jw = arr.getJSONObject(i);
                Wallet w = new Wallet();
                w.id = jw.optLong("id");
                w.name = jw.optString("name","Wallet");
                w.balance = jw.optLong("balance",0);
                JSONArray txArr = jw.optJSONArray("transactions");
                if (txArr != null) {
                    for (int j=0;j<txArr.length();j++) {
                        JSONObject jtx = txArr.getJSONObject(j);
                        WalletTransaction wt = new WalletTransaction();
                        wt.id = jtx.optLong("id");
                        wt.amount = jtx.optLong("amount");
                        wt.note = jtx.optString("note","");
                        wt.epochMillis = jtx.optLong("epochMillis");
                        wt.title = jtx.optString("title", null);
                        wt.source = jtx.optString("source", null);
                        wt.date = jtx.optString("date", null);
                        w.transactions.add(wt);
                    }
                }
                out.add(w);
            }
        } catch (Exception e){ e.printStackTrace(); }
        return out;
    }
    public void setPendingProfileSync(boolean pending) {
        sp.edit().putBoolean(KEY_PENDING_PROFILE_SYNC, pending).apply();
    }

    public boolean isPendingProfileSync() {
        return sp.getBoolean(KEY_PENDING_PROFILE_SYNC, false);
    }

    public void saveWallets(List<Wallet> wallets) {
        JSONArray arr = new JSONArray();
        if (wallets != null) {
            for (Wallet w : wallets) {
                if (w == null) continue;
                JSONObject jw = new JSONObject();
                try {
                    jw.put("id", w.id);
                    jw.put("name", w.name);
                    jw.put("balance", w.balance);
                    JSONArray txArr = new JSONArray();
                    for (WalletTransaction wt : w.transactions) {
                        JSONObject jtx = new JSONObject();
                        jtx.put("id", wt.id);
                        jtx.put("amount", wt.amount);
                        jtx.put("note", wt.note);
                        jtx.put("epochMillis", wt.epochMillis);
                        if (wt.title != null) jtx.put("title", wt.title);
                        if (wt.source != null) jtx.put("source", wt.source);
                        if (wt.date != null) jtx.put("date", wt.date);
                        txArr.put(jtx);
                    }
                    jw.put("transactions", txArr);
                } catch (Exception ex){ ex.printStackTrace(); }
                arr.put(jw);
            }
        }
        sp.edit().putString(KEY_WALLETS, arr.toString()).apply();
    }

    /* ---------- Weekly Aggregates ---------- */

    public List<WeeklyAggregate> getWeeklyAggregates() {
        String raw = sp.getString(KEY_WEEKLY, "[]");
        List<WeeklyAggregate> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i=0;i<arr.length();i++) {
                JSONObject jw = arr.getJSONObject(i);
                WeeklyAggregate wa = new WeeklyAggregate();
                wa.weekIndex = jw.optInt("weekIndex");
                wa.amountMinor = Math.round(jw.optDouble("amount", 0.0));
                String t = jw.optString("type", TransactionType.EXPENSE.name());
                try { wa.type = TransactionType.valueOf(t); } catch (Exception e) { wa.type = TransactionType.EXPENSE; }
                wa.label = jw.optString("label", null);
                out.add(wa);
            }
        } catch (Exception e){ e.printStackTrace(); }
        return out;
    }

    // Check if first launch
    public boolean isFirstLaunch() {
        return !sp.getBoolean(KEY_FIRST_LAUNCH_DONE, false);
    }

    // Mark handled
    public void markFirstLaunchHandled() {
        sp.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply();
    }

    public void saveCurrency(String code) {
        if (code == null) return;
        sp.edit().putString(KEY_CURRENCY_CODE, code.toUpperCase(java.util.Locale.US)).apply();
        // Optional: also update runtime formatter immediately
        CurrencyUtil.setCode(code);
    }

    public void clearAllDomainDataPreserveUser() {
        sp.edit()
                .remove(KEY_TX)
                .remove(KEY_WALLETS)
                .remove(KEY_WEEKLY)
                .remove(KEY_BUYING)
                .remove(KEY_CURRENCY_CODE)
                .remove(KEY_SEED_VERSION)
                .remove(KEY_PENDING_PROFILE_SYNC)
                .apply();
    }

    public void clearEverythingIncludingUser() {
        sp.edit().clear().apply();
    }
    public void savePasswordHashAndSalt(String hash, String salt) {
        if (hash == null || salt == null) return;
        sp.edit()
                .putString(KEY_PWD_HASH, hash)
                .putString(KEY_PWD_SALT, salt)
                .apply();
    }
    public String getCurrency() {
        String c = sp.getString(KEY_CURRENCY_CODE, null);
        return c;
    }
    public void saveWeeklyAggregates(List<WeeklyAggregate> list) {
        JSONArray arr = new JSONArray();
        if (list != null) {
            for (WeeklyAggregate wa : list) {
                if (wa == null) continue;
                JSONObject o = new JSONObject();
                try {
                    o.put("weekIndex", wa.weekIndex);
                    o.put("amount", wa.amountMinor);
                    o.put("type", wa.type != null ? wa.type.name() : TransactionType.EXPENSE.name());
                    if (wa.label != null) o.put("label", wa.label);
                } catch (Exception e){ e.printStackTrace(); }
                arr.put(o);
            }
        }
        sp.edit().putString(KEY_WEEKLY, arr.toString()).apply();
    }

    /* ---------- Buying Limits ---------- */

    public List<BuyingLimit> getBuyingLimits() {
        String raw = sp.getString(KEY_BUYING, "[]");
        List<BuyingLimit> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i=0;i<arr.length();i++) {
                JSONObject jb = arr.getJSONObject(i);
                BuyingLimit b = new BuyingLimit();
                b.platform = jb.optString("platform","");
                b.limitAmount = jb.optLong("limitAmount",0);
                b.usedInPeriod = jb.optLong("usedInPeriod",0);
                b.createdAtEpoch = jb.optLong("createdAtEpoch", System.currentTimeMillis());
                b.periodStartMillis = jb.optLong("periodStartMillis", b.createdAtEpoch);
                String freq = jb.optString("frequency", BuyingLimitFrequency.MONTHLY.name());
                try { b.frequency = BuyingLimitFrequency.valueOf(freq); } catch (Exception e){ b.frequency = BuyingLimitFrequency.MONTHLY; }
                out.add(b);
            }
        } catch (Exception e){ e.printStackTrace(); }
        return out;
    }

    public BuyingLimit getBuyingLimit(String platform) {
        if (platform == null) return null;
        for (BuyingLimit b : getBuyingLimits()) {
            if (b != null && platform.equalsIgnoreCase(b.platform)) return b;
        }
        return null;
    }

    public void upsertBuyingLimit(BuyingLimit limit) {
        if (limit == null || limit.platform == null) return;
        List<BuyingLimit> list = getBuyingLimits();
        boolean updated = false;
        for (int i=0;i<list.size();i++) {
            BuyingLimit b = list.get(i);
            if (b != null && b.platform.equalsIgnoreCase(limit.platform)) {
                list.set(i, limit);
                updated = true;
                break;
            }
        }
        if (!updated) list.add(limit);
        saveBuyingLimits(list);
    }

    public void saveBuyingLimits(List<BuyingLimit> list) {
        JSONArray arr = new JSONArray();
        if (list != null) {
            for (BuyingLimit b : list) {
                if (b == null) continue;
                JSONObject jb = new JSONObject();
                try {
                    jb.put("platform", b.platform);
                    jb.put("limitAmount", b.limitAmount);
                    jb.put("usedInPeriod", b.usedInPeriod);
                    jb.put("frequency", b.frequency != null ? b.frequency.name() : BuyingLimitFrequency.MONTHLY.name());
                    jb.put("createdAtEpoch", b.createdAtEpoch);
                    jb.put("periodStartMillis", b.periodStartMillis);
                } catch (Exception ex){ ex.printStackTrace(); }
                arr.put(jb);
            }
        }
        sp.edit().putString(KEY_BUYING, arr.toString()).apply();
    }

    /* ---------- Seed Version ---------- */
    public int getSeedVersion() { return sp.getInt(KEY_SEED_VERSION, 0); }
    public void setSeedVersion(int version) { sp.edit().putInt(KEY_SEED_VERSION, version).apply(); }
}