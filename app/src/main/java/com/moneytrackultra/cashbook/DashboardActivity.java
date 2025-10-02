package com.moneytrackultra.cashbook;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Build;
import android.content.Context;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Offline dashboard with user-added transactions only.
 * Features:
 *  - Day / Week / Month toggle (hourly or daily aggregation)
 *  - Add Income / Expense with date picker (no future dates)
 *  - Drawer navigation
 *  - Real-time chart + balance summary
 *  - Responds instantly to data clear broadcasts (from ProfileActivity)
 */
public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private TextView tvBalanceAmount;
    private TextView tvStatLeftValue;
    private TextView tvStatRightValue;

    private MaterialButton btnAddIncome, btnAddExpense;

    private LineChart lineChart;
    private Chip chipDay, chipWeek, chipMonth;

    private androidx.recyclerview.widget.RecyclerView rvTransactions;
    private RecentTransactionAdapter adapter;

    private PrefsManager prefs;

    private final SimpleDateFormat displayDateFmt =
            new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());

    /* ---- Broadcast receiver for domain data cleared (ProfileActivity) ---- */
    private final BroadcastReceiver dataClearedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            refreshAll();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(getApplicationContext());
        prefs = PrefsManager.get();

        setContentView(R.layout.activity_dashboard);

        initViews();
        setupDrawer();
        setupRecycler();
        setupChart();
        setupChips();
        setupButtons();
        refreshAll();
    }

    /* ---------------- Lifecycle to manage receiver & refresh ---------------- */

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ProfileActivity.ACTION_DOMAIN_DATA_CLEARED);
        if (Build.VERSION.SDK_INT >= 33) { // Build.VERSION_CODES.TIRAMISU
            registerReceiver(dataClearedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dataClearedReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(dataClearedReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure always-current (in case data changed in background)
        refreshAll();
    }

    /* ---------------- Initialization ---------------- */
    private void initViews() {
        drawerLayout      = findViewById(R.id.drawerLayout);
        navigationView    = findViewById(R.id.navigationView);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        tvBalanceAmount   = findViewById(R.id.tvBalanceAmount);
        tvStatLeftValue   = findViewById(R.id.tvStatLeftValue);
        tvStatRightValue  = findViewById(R.id.tvStatRightValue);
        lineChart         = findViewById(R.id.lineChart);
        chipDay           = findViewById(R.id.chipDay);
        chipWeek          = findViewById(R.id.chipWeek);
        chipMonth         = findViewById(R.id.chipMonth);
        rvTransactions    = findViewById(R.id.rvTransactions);
        btnAddIncome      = findViewById(R.id.btnAddIncome);
        btnAddExpense     = findViewById(R.id.btnAddExpense);

        btnMenu.setOnClickListener(v -> toggleDrawer());
    }

    private void setupDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            drawerLayout.openDrawer(GravityCompat.START);
    }

    private void setupRecycler() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecentTransactionAdapter(prefs.getTransactions(), t -> {
            prefs.deleteTransaction(t.id);
            refreshAll();
        });
        rvTransactions.setAdapter(adapter);
    }

    private void setupChart() {
        lineChart.setNoDataText("No data");
        lineChart.setNoDataTextColor(getColor(R.color.textSecondary));
        lineChart.setTouchEnabled(false);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);

        Description desc = new Description();
        desc.setText("");
        lineChart.setDescription(desc);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getColor(R.color.textSecondary));

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#22000000"));
        lineChart.getAxisLeft().setTextColor(getColor(R.color.textSecondary));
        lineChart.getAxisLeft().setAxisLineColor(Color.TRANSPARENT);
    }

    private void setupChips() {
        chipDay.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                chipWeek.setChecked(false);
                chipMonth.setChecked(false);
                renderChart();
            }
        });
        chipWeek.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                chipDay.setChecked(false);
                chipMonth.setChecked(false);
                renderChart();
            }
        });
        chipMonth.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                chipDay.setChecked(false);
                chipWeek.setChecked(false);
                renderChart();
            }
        });
    }

    private void setupButtons() {
        btnAddIncome.setOnClickListener(v -> showAddDialog(TransactionType.INCOME));
        btnAddExpense.setOnClickListener(v -> showAddDialog(TransactionType.EXPENSE));
    }

    /* ---------------- Add Transaction Dialog ---------------- */

    private void showAddDialog(TransactionType type) {
        final android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle(type == TransactionType.INCOME ? "Add Income" : "Add Expense");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 18);
        container.setPadding(pad, pad / 2, pad, 0);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Amount (e.g. 120.50)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(etAmount);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Title (optional)");
        container.addView(etTitle);

        TextView tvDateLabel = new TextView(this);
        tvDateLabel.setText("Date");
        tvDateLabel.setTextSize(14f);
        tvDateLabel.setTextColor(getColor(R.color.textSecondary));

        TextView tvDateValue = new TextView(this);
        tvDateValue.setTextSize(16f);
        tvDateValue.setTextColor(getColor(R.color.textPrimary));
        tvDateValue.setPadding(0, dp(8), 0, 0);

        Calendar chosenCal = Calendar.getInstance();
        tvDateValue.setText(displayDateFmt.format(chosenCal.getTime()));

        LinearLayout dateWrapper = new LinearLayout(this);
        dateWrapper.setOrientation(LinearLayout.VERTICAL);
        dateWrapper.addView(tvDateLabel);
        dateWrapper.addView(tvDateValue);
        dateWrapper.setPadding(0, dp(12), 0, 0);
        dateWrapper.setOnClickListener(v -> openDatePicker(chosenCal, tvDateValue));
        container.addView(dateWrapper);

        TextView hint = new TextView(this);
        hint.setText("Tap date to change (future dates disabled)");
        hint.setTextSize(12f);
        hint.setTextColor(getColor(R.color.textSecondary));
        hint.setPadding(0, dp(4), 0, dp(8));
        container.addView(hint);

        b.setView(container);

        b.setPositiveButton("Save", (dialog, which) -> {
            String raw = etAmount.getText().toString().trim();
            if (raw.isEmpty()) return;
            double amount;
            try {
                amount = Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                return;
            }
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                title = type == TransactionType.INCOME ? "Income" : "Expense";
            }

            String dateString = displayDateFmt.format(chosenCal.getTime());

            Calendar nowCal = Calendar.getInstance();
            Calendar chosenRef = (Calendar) chosenCal.clone();

            if (!isSameDay(chosenRef, nowCal)) {
                // Use midday to keep stable ordering (change to midnight if you prefer)
                chosenRef.set(Calendar.HOUR_OF_DAY, 12);
                chosenRef.set(Calendar.MINUTE, 0);
                chosenRef.set(Calendar.SECOND, 0);
                chosenRef.set(Calendar.MILLISECOND, 0);
            }

            prefs.addTransaction(type, amount, title, null, dateString);

            // Adjust epoch if user picked a past date.
            List<Transaction> current = prefs.getTransactions();
            if (!current.isEmpty()) {
                Transaction newest = current.get(0);
                if (!isSameDay(chosenCal, nowCal)) {
                    newest.epochMillis = chosenRef.getTimeInMillis();
                    prefs.replaceTransactions(current);
                }
            }

            refreshAll();
            rvTransactions.post(() -> rvTransactions.smoothScrollToPosition(0));
        });

        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void openDatePicker(Calendar chosenCal, TextView tvDateValue) {
        Calendar today = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    chosenCal.set(Calendar.YEAR, year);
                    chosenCal.set(Calendar.MONTH, month);
                    chosenCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    tvDateValue.setText(displayDateFmt.format(chosenCal.getTime()));
                },
                chosenCal.get(Calendar.YEAR),
                chosenCal.get(Calendar.MONTH),
                chosenCal.get(Calendar.DAY_OF_MONTH)
        );
        dlg.getDatePicker().setMaxDate(today.getTimeInMillis()); // disallow future
        dlg.show();
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private int dp(int v) {
        return (int) (getResources().getDisplayMetrics().density * v);
    }

    /* ---------------- Summary & List ---------------- */

    private void refreshAll() {
        updateSummary();
        updateList();
        renderChart();
    }

    private void updateSummary() {
        long income = 0;
        long expense = 0;
        for (Transaction t : prefs.getTransactions()) {
            if (t.type == TransactionType.INCOME) income += t.getAmountMinor();
            else expense +=  t.getAmountMinor();;
        }
        long balance = income - expense;
        tvBalanceAmount.setText(CurrencyUtil.formatMinor(balance));
        tvStatLeftValue.setText(CurrencyUtil.formatMinor(expense));
        tvStatRightValue.setText(CurrencyUtil.formatMinor(income));
    }

    private void updateList() {
        adapter.submit(prefs.getTransactions());
    }

    /* ---------------- Chart Rendering ---------------- */

    private void renderChart() {
        List<Transaction> all = prefs.getTransactions();
        if (all.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("Add transactions to see your chart");
            lineChart.invalidate();
            return;
        }

        boolean day = chipDay.isChecked();
        boolean week = chipWeek.isChecked();
        // if neither day nor week -> month

        long now = System.currentTimeMillis();
        long unit;
        int buckets;
        boolean hourly = false;

        if (day) {
            buckets = 24; // 24 hours
            unit = 60L * 60L * 1000L;
            hourly = true;
        } else if (week) {
            buckets = 7;  // 7 days
            unit = 24L * 60L * 60L * 1000L;
        } else {
            buckets = 30; // 30 days
            unit = 24L * 60L * 60L * 1000L;
        }

        long start = now - (buckets - 1L) * unit;

        long[] incBuckets = new long[buckets];
        long[] expBuckets = new long[buckets];

        for (Transaction t : all) {
            if (t.epochMillis < start) continue;
            int idx = (int) ((t.epochMillis - start) / unit);
            if (idx < 0 || idx >= buckets) continue;
            if (t.type == TransactionType.INCOME) incBuckets[idx] += t.getAmountMinor();
            else expBuckets[idx] += t.getAmountMinor();
        }

        List<Entry> incEntries = new ArrayList<>();
        List<Entry> expEntries = new ArrayList<>();
        for (int i = 0; i < buckets; i++) {
            if (incBuckets[i] > 0) incEntries.add(new Entry(i, incBuckets[i] / 100f));
            if (expBuckets[i] > 0) expEntries.add(new Entry(i, expBuckets[i] / 100f));
        }

        if (incEntries.isEmpty() && expEntries.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }

        LineData data = new LineData();
        if (!incEntries.isEmpty()) {
            LineDataSet dsInc = new LineDataSet(incEntries, "Income");
            styleDataSet(dsInc, getColor(R.color.incomeGreen));
            data.addDataSet(dsInc);
        }
        if (!expEntries.isEmpty()) {
            LineDataSet dsExp = new LineDataSet(expEntries, "Expense");
            styleDataSet(dsExp, getColor(R.color.expenseRed));
            data.addDataSet(dsExp);
        }

        if (hourly) {
            lineChart.getXAxis().setGranularity(1f);
        }

        lineChart.setData(data);
        lineChart.invalidate();
    }

    private void styleDataSet(LineDataSet ds, int color) {
        ds.setColor(color);
        ds.setCircleColor(color);
        ds.setLineWidth(2.2f);
        ds.setCircleRadius(4f);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawValues(false);
        ds.setDrawFilled(false);
        ds.setHighLightColor(Color.TRANSPARENT);
    }

    /* ---------------- Drawer Navigation ---------------- */

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        drawerLayout.closeDrawer(GravityCompat.START);

        // Adjust IDs to match nav_menu.xml
        if (id == R.id.nav_home) {
            return true;
        } else if (id == R.id.nav_balance) {
            startActivity(new Intent(this, BalanceActivity.class));
            return true;
        } else if (id == R.id.nav_cash_flow) {
            startActivity(new Intent(this, CashFlowActivity.class)
                    .putExtra("type", TransactionType.INCOME.name()));
            return true;
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.nav_logout) {
            AuthManager.get().hardLogout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return false;
    }
}