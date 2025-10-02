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
 * Generic adapter for listing income / expense transactions.
 * Expects layout: item_cash_flow_transaction.xml
 * Required view IDs inside that layout:
 *   - tvTitle
 *   - tvSub (date + optional source)
 *   - tvAmount
 *   - divider (optional; hide on last item)
 *
 * Data model expected: Transaction with fields:
 *   String id, title, date, source;
 *   long amount; (stored as positive for income, positive for expense?  -> In earlier code: amount positive for income, positive for expense then you decide sign separately.
 *   TransactionType type (INCOME | EXPENSE)
 *
 * The adapter just displays plus/minus styling based on type.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    private final List<Transaction> data = new ArrayList<>();

    public void submit(List<Transaction> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

    public Transaction getItem(int position) {
        return position >= 0 && position < data.size() ? data.get(position) : null;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cash_flow_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(data.get(position), position == data.size() - 1);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, tvAmount;
        View divider;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle  = itemView.findViewById(R.id.tvTitle);
            tvSub    = itemView.findViewById(R.id.tvSub);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            divider  = itemView.findViewById(R.id.divider);
        }

        void bind(Transaction t, boolean last) {
            if (t == null) return;

            // Title
            if (tvTitle != null) tvTitle.setText(t.title);

            // Subtitle (date + source if present)
            if (tvSub != null) {
                StringBuilder sb = new StringBuilder();
                if (t.date != null) sb.append(t.date);
                if (t.source != null && !t.source.isEmpty()) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append(t.source);
                }
                tvSub.setText(sb.toString());
            }

            // Amount (+ for income, - for expense)
            if (tvAmount != null) {
                boolean income = t.type == TransactionType.INCOME;
                long raw = Math.abs(t.amount);
                String prefix = income ? "+ " : "- ";
                tvAmount.setText(prefix + CurrencyUtil.rupiah(raw));
                int color = itemView.getContext().getColor(income ? R.color.incomeGreen : R.color.expenseRed);
                tvAmount.setTextColor(color);
            }

            if (divider != null) {
                divider.setVisibility(last ? View.GONE : View.VISIBLE);
            }
        }
    }
}