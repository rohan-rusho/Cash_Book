package com.moneytrackultra.cashbook;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

/**
 * Minimal Buying Limit screen (no advanced logic).
 */
public class BuyingLimitActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etLimitAmount;
    private Spinner spinnerFrequency;
    private Button btnSaveLimit;
    private TextView tvStatus;

    private View[] cardViews;

    private static final List<String> PLATFORMS = Arrays.asList(
            "Shopee","Tokopedia","Lazada","Amazon","eBay","AliExpress",
            "Flipkart","Etsy","Walmart","BestBuy","Rakuten","Daraz"
    );

    private String selectedPlatform = PLATFORMS.get(0);
    private BuyingLimitFrequency selectedFrequency = BuyingLimitFrequency.MONTHLY;

    private boolean suppressAmountWatcher = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buying_limit);

        bindViews();
        setupBackHandler();
        initCards();
        attachCardClicks();
        setupFrequencySpinner();
        setupAmountFormatter();

        loadPlatform(selectedPlatform);
        btnSaveLimit.setOnClickListener(v -> saveCurrent());
    }

    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        etLimitAmount    = findViewById(R.id.etLimitAmount);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        btnSaveLimit     = findViewById(R.id.btnSaveLimit);
        tvStatus         = findViewById(R.id.tvStatus);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void initCards() {
        cardViews = new View[]{
                findViewById(R.id.cardShopee),
                findViewById(R.id.cardTokopedia),
                findViewById(R.id.cardLazada),
                findViewById(R.id.cardAmazon),
                findViewById(R.id.cardEbay),
                findViewById(R.id.cardAliExpress),
                findViewById(R.id.cardFlipkart),
                findViewById(R.id.cardEtsy),
                findViewById(R.id.cardWalmart),
                findViewById(R.id.cardBestBuy),
                findViewById(R.id.cardRakuten),
                findViewById(R.id.cardDaraz)
        };
    }

    private void attachCardClicks() {
        if (cardViews == null) return;
        for (View v : cardViews) {
            if (v == null) continue;
            v.setOnClickListener(view -> {
                String platform = getPlatformFromView(view);
                if (platform == null) return;
                if (!platform.equals(selectedPlatform)) {
                    selectedPlatform = platform;
                    loadPlatform(platform);
                }
                updateSelectionHighlight();
            });
            // Initialize brand text inside each card if needed
            TextView brand = v.findViewById(R.id.tvBrand);
            String p = getPlatformFromView(v);
            if (brand != null && p != null) brand.setText(p);
        }
        updateSelectionHighlight();
    }

    private String getPlatformFromView(View view) {
        if (view == null) return null;
        Object tag = view.getTag();
        if (tag instanceof String && !((String) tag).isEmpty()) return (String) tag;
        // fallback by index
        if (cardViews != null) {
            for (int i = 0; i < cardViews.length; i++) {
                if (cardViews[i] == view && i < PLATFORMS.size()) {
                    return PLATFORMS.get(i);
                }
            }
        }
        return null;
    }

    private void updateSelectionHighlight() {
        if (cardViews == null) return;
        for (View v : cardViews) {
            if (v == null) continue;
            boolean selected = selectedPlatform.equals(getPlatformFromView(v));
            // simple highlight: change alpha
            v.setAlpha(selected ? 1f : 0.55f);
        }
    }

    private void setupFrequencySpinner() {
        String[] freqs = {"Daily","Weekly","Monthly","Yearly"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, freqs);
        spinnerFrequency.setAdapter(adapter);
        spinnerFrequency.setSelection(2);

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: selectedFrequency = BuyingLimitFrequency.DAILY; break;
                    case 1: selectedFrequency = BuyingLimitFrequency.WEEKLY; break;
                    case 2: selectedFrequency = BuyingLimitFrequency.MONTHLY; break;
                    case 3: selectedFrequency = BuyingLimitFrequency.YEARLY; break;
                    default: selectedFrequency = BuyingLimitFrequency.MONTHLY; break;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAmountFormatter() {
        etLimitAmount.addTextChangedListener(new TextWatcher() {
            String lastDigits = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (suppressAmountWatcher) return;
                String digits = s.toString().replaceAll("[^0-9]","");
                if (digits.isEmpty()) {
                    lastDigits = "";
                    return;
                }
                try {
                    long val = Long.parseLong(digits);
                    String formatted = CurrencyUtil.rupiah(val);
                    suppressAmountWatcher = true;
                    etLimitAmount.setText(formatted);
                    etLimitAmount.setSelection(formatted.length());
                    suppressAmountWatcher = false;
                    lastDigits = digits;
                } catch (NumberFormatException e) {
                    suppressAmountWatcher = true;
                    etLimitAmount.setText(lastDigits);
                    etLimitAmount.setSelection(lastDigits.length());
                    suppressAmountWatcher = false;
                }
            }
        });
    }

    private void loadPlatform(String platform) {
        BuyingLimit limit = PrefsManager.get().getBuyingLimit(platform);
        if (limit == null) {
            etLimitAmount.setText("");
            spinnerFrequency.setSelection(2);
            selectedFrequency = BuyingLimitFrequency.MONTHLY;
            tvStatus.setText("No limit set for " + platform);
            updateCardUsage(platform, 0, 0);
        } else {
            etLimitAmount.setText(limit.limitAmount > 0 ? CurrencyUtil.rupiah(limit.limitAmount) : "");
            int pos;
            switch (limit.frequency) {
                case DAILY: pos = 0; break;
                case WEEKLY: pos = 1; break;
                case MONTHLY: pos = 2; break;
                case YEARLY: pos = 3; break;
                default: pos = 2; break;
            }
            spinnerFrequency.setSelection(pos);
            selectedFrequency = limit.frequency;

            // compute usage
            long used = computeUsageForPlatform(platform);
            updateCardUsage(platform, used, limit.limitAmount);
            tvStatus.setText("Usage: " + formatUsage(used, limit.limitAmount));
        }
        updateSelectionHighlight();
    }

    private long computeUsageForPlatform(String platform) {
        List<Transaction> tx = PrefsManager.get().getTransactions();
        if (tx == null) return 0;
        long sum = 0;
        for (Transaction t : tx) {
            if (t == null) continue;
            if (t.type == TransactionType.EXPENSE &&
                    t.source != null &&
                    t.source.equalsIgnoreCase(platform)) {
                long amt = t.amount < 0 ? Math.abs(t.amount) : t.amount;
                sum += amt;
            }
        }
        return sum;
    }

    private void updateCardUsage(String platform, long used, long limit) {
        // find the card and update tvLimitValue + tvPercent if present
        if (cardViews == null) return;
        for (View v : cardViews) {
            if (v == null) continue;
            String p = getPlatformFromView(v);
            if (platform.equals(p)) {
                TextView tvValue = v.findViewById(R.id.tvLimitValue);
                TextView tvPercent = v.findViewById(R.id.tvPercent);
                if (tvValue != null) {
                    tvValue.setText(limit > 0
                            ? CurrencyUtil.rupiah(used) + " / " + CurrencyUtil.rupiah(limit)
                            : "No Limit");
                }
                if (tvPercent != null) {
                    if (limit > 0) {
                        int pct = (int) Math.min(100, (used * 100f / limit));
                        tvPercent.setText(pct + "%");
                    } else {
                        tvPercent.setText("0%");
                    }
                }
                break;
            }
        }
    }

    private String formatUsage(long used, long limit) {
        if (limit <= 0) return CurrencyUtil.rupiah(used);
        return CurrencyUtil.rupiah(used) + " / " + CurrencyUtil.rupiah(limit);
    }

    private void saveCurrent() {
        String digits = etLimitAmount.getText().toString().replaceAll("[^0-9]","");
        long amount = 0;
        if (!digits.isEmpty()) {
            try { amount = Long.parseLong(digits); } catch (NumberFormatException ignored) {}
        }

        BuyingLimit existing = PrefsManager.get().getBuyingLimit(selectedPlatform);
        if (existing == null) {
            existing = new BuyingLimit(selectedPlatform, amount, selectedFrequency, 0, System.currentTimeMillis());
        } else {
            existing.limitAmount = amount;
            existing.frequency = selectedFrequency;
        }
        PrefsManager.get().upsertBuyingLimit(existing);

        long used = computeUsageForPlatform(selectedPlatform);
        updateCardUsage(selectedPlatform, used, amount);
        tvStatus.setText("Saved: " + formatUsage(used, amount));
    }
}