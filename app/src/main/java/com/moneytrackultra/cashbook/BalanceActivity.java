package com.moneytrackultra.cashbook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BalanceActivity extends AppCompatActivity {

    private static final boolean GROUP_BY_TITLE = true;
    private static final String UNTITLED = "(No Title)";

    private TextView tvTotalBalance;
    private TextView tvTotalShoppingValue;
    private TextView tvTotalTopupValue;
    private RecyclerView rvWallets;
    private ImageButton btnBack;
    private TextView tvEmpty;

    private BalanceAggregateAdapter adapter;
    private final List<BalanceAggregate> aggregates = new ArrayList<>();

    private final BroadcastReceiver dataClearedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { reloadAll(); }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(getApplicationContext());
        setContentView(R.layout.activity_balance);

        bindViews();
        setupBackHandler();
        setupList();
        reloadAll();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ProfileActivity.ACTION_DOMAIN_DATA_CLEARED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(dataClearedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dataClearedReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try { unregisterReceiver(dataClearedReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadAll();
    }

    private void bindViews() {
        tvTotalBalance        = findViewById(R.id.tvTotalBalance);
        tvTotalShoppingValue  = findViewById(R.id.tvTotalShoppingValue);
        tvTotalTopupValue     = findViewById(R.id.tvTotalTopupValue);
        rvWallets             = findViewById(R.id.rvWallets);
        btnBack               = findViewById(R.id.btnBack);
        tvEmpty               = findViewById(R.id.tvEmptyAggregates);
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

    private void setupList() {
        rvWallets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BalanceAggregateAdapter();
        rvWallets.setAdapter(adapter);
    }

    private void reloadAll() {
        // Preserve which keys were expanded
        Map<String, Boolean> expandedMap = new LinkedHashMap<>();
        for (BalanceAggregate a : aggregates) {
            expandedMap.put(a.key, a.expanded);
        }

        aggregates.clear();

        List<Transaction> all = PrefsManager.get().getTransactions();
        if (all == null) all = new ArrayList<>();

        long totalIncome = 0;
        long totalExpense = 0;

        Map<String, BalanceAggregate> map = new LinkedHashMap<>();

        for (Transaction t : all) {
            if (t == null) continue;
            long amt = t.getAmountMinor();
            if (t.type == TransactionType.INCOME) totalIncome += amt;
            else totalExpense += amt;

            String key = GROUP_BY_TITLE
                    ? (t.title == null || t.title.trim().isEmpty() ? UNTITLED : t.title.trim())
                    : (t.source == null || t.source.trim().isEmpty() ? UNTITLED : t.source.trim());

            BalanceAggregate agg = map.get(key);
            if (agg == null) {
                agg = new BalanceAggregate();
                agg.key = key;
                map.put(key, agg);
            }
            if (t.type == TransactionType.INCOME) {
                agg.incomeMinor += amt;
            } else {
                agg.expenseMinor += amt;
            }
            agg.transactions.add(t);
        }

        aggregates.addAll(map.values());

        // Restore expansion state
        for (BalanceAggregate a : aggregates) {
            Boolean wasExpanded = expandedMap.get(a.key);
            if (wasExpanded != null) a.expanded = wasExpanded;
        }

        long net = totalIncome - totalExpense;
        tvTotalBalance.setText(CurrencyUtil.formatMinor(net));
        tvTotalShoppingValue.setText(CurrencyUtil.formatMinor(totalExpense));
        tvTotalTopupValue.setText(CurrencyUtil.formatMinor(totalIncome));

        adapter.submit(aggregates);

        if (tvEmpty != null) {
            tvEmpty.setVisibility(aggregates.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}