package com.moneytrackultra.cashbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.VH> {

    public interface OnLongDelete { void onDelete(Transaction t); }

    private List<Transaction> data;
    private final OnLongDelete delete;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US);

    public RecentTransactionAdapter(List<Transaction> data, OnLongDelete delete) {
        this.data = data;
        this.delete = delete;
    }

    public void submit(List<Transaction> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = data.get(position);
        String amount = CurrencyUtil.formatMinor(t.getAmountMinor());
        String signed = (t.type == TransactionType.EXPENSE ? "- " : "+ ") + amount;
        h.title.setText(t.title.isEmpty()
                ? (t.type == TransactionType.INCOME ? "Income" : "Expense")
                : t.title);
        h.subtitle.setText(signed + "  •  " + fmt.format(new Date(t.epochMillis)));

        int color = h.itemView.getResources().getColor(
                t.type == TransactionType.EXPENSE ? R.color.expenseRed : R.color.incomeGreen
        );
        h.subtitle.setTextColor(color);

        h.itemView.setOnLongClickListener(v -> {
            if (delete != null) delete.onDelete(t);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            title   = itemView.findViewById(android.R.id.text1);
            subtitle= itemView.findViewById(android.R.id.text2);
        }
    }
}