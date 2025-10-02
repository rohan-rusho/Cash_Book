package com.moneytrackultra.cashbook;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Weekly chart screen using Title as category; Month derived from epochMillis or date string.
 */
public class CashFlowChartActivity extends AppCompatActivity {

    private static final String TAG = "CashFlowChart";
    private static final String ALL = "All";

    private Spinner spinnerSource, spinnerMonth;
    private TextView tvChartLabel;
    private LineChart lineChart;
    private androidx.recyclerview.widget.RecyclerView rvWeekly;

    private ArrayAdapter<String> sourceAdapter;
    private ArrayAdapter<String> monthAdapter;
    private WeeklyAdapter weeklyAdapter;

    private TransactionType type = TransactionType.INCOME;
    private String selectedSource = ALL;
    private String selectedMonth  = ALL; // e.g. "October 2025"

    private final List<String> sources = new ArrayList<>();
    private final List<String> months  = new ArrayList<>();

    private final Calendar cal = Calendar.getInstance();

    private final SimpleDateFormat[] possibleFormats = new SimpleDateFormat[] {
            new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()),
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault()),
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_flow_chart);

        parseIntent();
        bindViews();
        setupBackHandler();

        collectMonths();
        collectCategories();

        setupSpinners();
        setupRecycler();
        setupChart();
        refreshData();

        // Debug: show if spinner has only one entry
        spinnerSource.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (sources.size() <= 1) {
                            Toast.makeText(CashFlowChartActivity.this,
                                    "Only 'All' category. Add transactions with a Title.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        spinnerSource.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        collectMonths();
        collectCategories();
        refreshAdapters();
        refreshData();
    }

    private void parseIntent() {
        String t = getIntent().getStringExtra("type");
        if (t != null) {
            try { type = TransactionType.valueOf(t); } catch (Exception ignored) {}
        }
    }

    private void bindViews() {

        tvChartLabel  = findViewById(R.id.tvChartLabel);
        lineChart     = findViewById(R.id.lineChart);
        rvWeekly      = findViewById(R.id.rvWeekly);

        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        boolean isIncome = type == TransactionType.INCOME;
        tvChartLabel.setText(isIncome ? getString(R.string.income) : getString(R.string.expense));
        tvChartLabel.setTextColor(getColor(isIncome ? R.color.incomeGreen : R.color.expenseRed));
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    // ---------------- Data Collection ----------------
    private List<Transaction> allTransactions() {
        List<Transaction> list = PrefsManager.get().getTransactions();
        return list == null ? new ArrayList<>() : list;
    }

    private void collectMonths() {
        Set<String> set = new LinkedHashSet<>();
        for (Transaction t : allTransactions()) {
            if (t == null || t.type != type) continue;
            String m = extractMonthYear(t);
            if (m != null) set.add(m);
        }
        months.clear();
        months.add(ALL);
        months.addAll(set);
        if (!months.contains(selectedMonth)) selectedMonth = ALL;
        Log.d(TAG, "Months: " + months);
    }

    private void collectCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (Transaction t : allTransactions()) {
            if (t == null || t.type != type) continue;
            String m = extractMonthYear(t);
            if (!ALL.equals(selectedMonth) && !selectedMonth.equals(m)) continue;
            String title = canonicalTitle(t);
            if (!title.isEmpty()) set.add(title);
        }
        sources.clear();
        sources.add(ALL);
        sources.addAll(set);
        if (!sources.contains(selectedSource)) selectedSource = ALL;
        Log.d(TAG, "Sources(categories): " + sources + " selected=" + selectedSource);
    }

    private String canonicalTitle(Transaction t) {
        if (t.title != null) {
            String tt = t.title.trim();
            if (!tt.isEmpty()) return tt;
        }
        // fallback for blank
        return (t.type == TransactionType.INCOME) ? "Income" : "Expense";
    }

    private String extractMonthYear(Transaction t) {
        long epoch = t.epochMillis;
        if (t.date != null && !t.date.trim().isEmpty()) {
            Long parsed = parseDateEpoch(t.date.trim());
            if (parsed != null) epoch = parsed;
        }
        if (epoch <= 0) return null;
        cal.setTimeInMillis(epoch);
        return cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " + cal.get(Calendar.YEAR);
    }

    private Long parseDateEpoch(String dateStr) {
        for (SimpleDateFormat f : possibleFormats) {
            try { return f.parse(dateStr).getTime(); }
            catch (ParseException ignored) {}
        }
        return null;
    }

    // ---------------- Spinners ----------------
    private void setupSpinners() {
        sourceAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_filter,
                R.id.tvSpinnerItem,
                sources);
        sourceAdapter.setDropDownViewResource(R.layout.spinner_dropdown_filter);
        spinnerSource.setAdapter(sourceAdapter);
        // Optional popup background:
        // spinnerSource.setPopupBackgroundResource(R.drawable.bg_spinner_popup);

        monthAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_filter,
                R.id.tvSpinnerItem,
                months);
        monthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_filter);
        spinnerMonth.setAdapter(monthAdapter);
        // spinnerMonth.setPopupBackgroundResource(R.drawable.bg_spinner_popup);

        spinnerSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedSource = sources.get(pos);
                refreshData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedMonth = months.get(pos);
                collectCategories(); // categories depend on chosen month
                refreshAdapters();
                refreshData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMonth.setSelection(months.indexOf(selectedMonth), false);
        spinnerSource.setSelection(sources.indexOf(selectedSource), false);
    }

    private void refreshAdapters() {
        if (monthAdapter != null) {
            monthAdapter.clear();
            monthAdapter.addAll(months);
            monthAdapter.notifyDataSetChanged();
            int monthIndex = months.indexOf(selectedMonth);
            if (monthIndex >= 0) spinnerMonth.setSelection(monthIndex, false);
        }
        if (sourceAdapter != null) {
            sourceAdapter.clear();
            sourceAdapter.addAll(sources);
            sourceAdapter.notifyDataSetChanged();
            int srcIndex = sources.indexOf(selectedSource);
            if (srcIndex >= 0) spinnerSource.setSelection(srcIndex, false);
        }
    }

    // ---------------- Recycler & Chart ----------------
    private void setupRecycler() {
        rvWeekly.setLayoutManager(new LinearLayoutManager(this));
        weeklyAdapter = new WeeklyAdapter(type);
        weeklyAdapter.setShowSign(true);
        rvWeekly.setAdapter(weeklyAdapter);
    }

    private void setupChart() {
        lineChart.setNoDataText("No chart data");
        lineChart.setNoDataTextColor(getColor(R.color.textSecondary));
        lineChart.getLegend().setEnabled(false);
        Description d = new Description();
        d.setText("");
        lineChart.setDescription(d);

        XAxis x = lineChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#22000000"));
    }

    private void refreshData() {
        List<WeeklyAggregate> weekly = buildWeekly();
        weeklyAdapter.submit(weekly);
        buildChart(weekly);
    }

    private List<WeeklyAggregate> buildWeekly() {
        List<Transaction> all = allTransactions();
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : all) {
            if (t == null || t.type != type) continue;
            String monthYear = extractMonthYear(t);
            if (!ALL.equals(selectedMonth) && !selectedMonth.equals(monthYear)) continue;
            String cat = canonicalTitle(t);
            if (!ALL.equals(selectedSource) && !cat.equals(selectedSource)) continue;
            filtered.add(t);
        }

        if (filtered.isEmpty()) return new ArrayList<>();

        java.util.Map<Integer, WeeklyAggregate> map = new java.util.LinkedHashMap<>();
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);

        for (Transaction t : filtered) {
            long epoch = t.epochMillis;
            if (t.date != null && !t.date.trim().isEmpty()) {
                Long parsed = parseDateEpoch(t.date.trim());
                if (parsed != null) epoch = parsed;
            }
            c.setTimeInMillis(epoch);
            int w = c.get(Calendar.WEEK_OF_MONTH); // 1-based
            WeeklyAggregate agg = map.get(w);
            if (agg == null) {
                agg = new WeeklyAggregate();
                agg.weekIndex = w;
                agg.type = type;
                agg.label = "W" + w;
                map.put(w, agg);
            }
            agg.amountMinor += t.getAmountMinor();
        }

        List<WeeklyAggregate> list = new ArrayList<>(map.values());
        list.sort((a,b)->Integer.compare(a.weekIndex,b.weekIndex));
        return list;
    }

    private void buildChart(List<WeeklyAggregate> list) {
        if (list == null || list.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            WeeklyAggregate w = list.get(i);
            entries.add(new Entry(i, (float) w.amountMinor));
            labels.add(w.label != null ? w.label : ("W" + w.weekIndex));
        }

        LineDataSet ds = new LineDataSet(entries,
                type == TransactionType.INCOME ? "Income" : "Expense");
        int color = getColor(type == TransactionType.INCOME ? R.color.incomeGreen : R.color.expenseRed);
        ds.setColor(color);
        ds.setCircleColor(color);
        ds.setLineWidth(2.2f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setHighLightColor(Color.TRANSPARENT);

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.setData(new LineData(ds));
        lineChart.invalidate();
    }
}