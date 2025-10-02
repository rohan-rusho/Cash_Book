package com.moneytrackultra.cashbook;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Cash Flow screen (simplified).
 * Removed filtering spinners. Shows all transactions of current type.
 */
public class CashFlowActivity extends AppCompatActivity {

    private View btnBack;
    private TextView tabIncome, tabExpense;
    private View indicatorIncome, indicatorExpense;
    private TextView tvSummaryTitle, tvSummaryAmount, btnViewChart, sectionListTitle;
    private androidx.recyclerview.widget.RecyclerView rvTransactions;

    private final OfflineRepository repo = OfflineRepository.get();
    private TransactionAdapter adapter;

    private TransactionType currentType = TransactionType.INCOME;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_flow);

        bindViews();
        setupBackHandler();
        setupTabs();
        setupRecycler();
        styleTabs();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    /* ---------------- Binding & Navigation ---------------- */
    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        tabIncome        = findViewById(R.id.tabIncome);
        tabExpense       = findViewById(R.id.tabExpense);
        indicatorIncome  = findViewById(R.id.indicatorIncome);
        indicatorExpense = findViewById(R.id.indicatorExpense);
        tvSummaryTitle   = findViewById(R.id.tvSummaryTitle);
        tvSummaryAmount  = findViewById(R.id.tvSummaryAmount);
        btnViewChart     = findViewById(R.id.btnViewChart);
        sectionListTitle = findViewById(R.id.sectionListTitle);
        rvTransactions   = findViewById(R.id.rvTransactions);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnViewChart.setOnClickListener(v -> openChartScreen());
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /* ---------------- Tabs ---------------- */
    private void setupTabs() {
        tabIncome.setOnClickListener(v -> switchType(TransactionType.INCOME));
        tabExpense.setOnClickListener(v -> switchType(TransactionType.EXPENSE));
    }

    private void switchType(TransactionType newType) {
        if (currentType != newType) {
            currentType = newType;
            styleTabs();
            updateUI();
        }
    }

    private void styleTabs() {
        boolean income = currentType == TransactionType.INCOME;
        indicatorIncome.setVisibility(income ? View.VISIBLE : View.INVISIBLE);
        indicatorExpense.setVisibility(income ? View.INVISIBLE : View.VISIBLE);

        tabIncome.setTextColor(getColor(income ? R.color.incomeGreen : R.color.textSecondary));
        tabExpense.setTextColor(getColor(income ? R.color.textSecondary : R.color.expenseRed));

        tvSummaryTitle.setText(income ? R.string.total_income : R.string.total_expense);
        sectionListTitle.setText(income ? R.string.income : R.string.expense);
        tvSummaryAmount.setTextColor(getColor(income ? R.color.incomeGreen : R.color.expenseRed));
    }

    /* ---------------- Recycler ---------------- */
    private void setupRecycler() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();
        rvTransactions.setAdapter(adapter);
    }

    /* ---------------- Update UI ---------------- */
    private void updateUI() {
        if (adapter == null) return;

        List<Transaction> all = PrefsManager.get().getTransactions();
        if (all == null) all = new ArrayList<>();

        List<Transaction> filtered = new ArrayList<>();
        long totalMinor = 0;

        for (Transaction t : all) {
            if (t == null) continue;
            if (t.type == currentType) {
                filtered.add(t);
                totalMinor += t.getAmountMinor(); // amounts stored positive
            }
        }

        adapter.submit(filtered);

        // If expenses are stored as positive numbers (like incomes),
        // we just display total directly. Change if you use negatives.
        tvSummaryAmount.setText(CurrencyUtil.formatMinor(totalMinor));
    }

    /* ---------------- Navigation ---------------- */
    private void openChartScreen() {
        Intent intent = new Intent(this, CashFlowChartActivity.class);
        intent.putExtra("type", currentType.name());
        startActivity(intent);
    }
}