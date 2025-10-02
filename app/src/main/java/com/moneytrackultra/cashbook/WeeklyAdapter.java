package com.moneytrackultra.cashbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays weekly aggregates for a single TransactionType passed in constructor.
 * Expects weekIndex to be 1-based.
 *
 * Change requested:
 *  - Show label as "Week-1", "Week-2", ... instead of "W1".
 *    If WeeklyAggregate.label is already set (non-empty), that custom label is used.
 */
public class WeeklyAdapter extends RecyclerView.Adapter<WeeklyAdapter.VH> {

    private final List<WeeklyAggregate> data = new ArrayList<>();
    private final TransactionType type;
    private boolean showSign = true; // toggle if you want to hide +/- sign

    public WeeklyAdapter(TransactionType type){
        this.type = type;
    }

    public void submit(List<WeeklyAggregate> list){
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void setShowSign(boolean showSign) {
        this.showSign = showSign;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cash_flow_week_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(data.get(position), type, showSign);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /* ---------------- ViewHolder ---------------- */
    static class VH extends RecyclerView.ViewHolder {
        private final TextView tvWeekLabel;
        private final TextView tvWeekAmount;

        VH(@NonNull View itemView) {
            super(itemView);
            tvWeekLabel  = itemView.findViewById(R.id.tvWeekLabel);
            tvWeekAmount = itemView.findViewById(R.id.tvWeekAmount);
        }

        void bind(WeeklyAggregate w, TransactionType tType, boolean showSign){
            // Custom label logic: use provided label if not empty, otherwise "Week-{index}"
            String label;
            if (w.label != null && !w.label.trim().isEmpty()) {
                label = w.label.trim();
            } else {
                // weekIndex expected 1-based; if your data is 0-based adjust with +1
                label = "Week-" + w.weekIndex;
            }
            tvWeekLabel.setText(label);

            boolean income = (tType == TransactionType.INCOME);
            String sign = showSign ? (income ? "+ " : "- ") : "";
            String formatted = CurrencyUtil.formatMinor(w.amountMinor);
            tvWeekAmount.setText(sign + formatted);

            int color = itemView.getContext()
                    .getColor(income ? R.color.incomeGreen : R.color.expenseRed);
            tvWeekAmount.setTextColor(color);
        }
    }
}