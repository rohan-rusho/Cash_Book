package com.moneytrackultra.cashbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class BalanceDetailAdapter extends RecyclerView.Adapter<BalanceDetailAdapter.DetailVH> {

    private final List<Transaction> items = new ArrayList<>();
    private final SimpleDateFormat df = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    void submit(List<Transaction> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DetailVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_balance_detail_transaction, parent, false);
        return new DetailVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailVH h, int position) {
        Transaction t = items.get(position);
        String title = (t.title == null || t.title.trim().isEmpty())
                ? (t.type == TransactionType.INCOME ? "Income" : "Expense")
                : t.title.trim();
        h.tvTxTitle.setText(title);

        long amt = t.getAmountMinor();
        boolean income = t.type == TransactionType.INCOME;
        h.tvTxAmount.setText((income ? "+ " : "- ") + CurrencyUtil.formatMinor(amt));
        int c = h.itemView.getContext().getColor(income ? R.color.incomeGreen : R.color.expenseRed);
        h.tvTxAmount.setTextColor(c);

        h.tvTxType.setText(income ? "INCOME" : "EXPENSE");
        h.tvTxType.setTextColor(c);

        String dateStr = (t.date != null && !t.date.isEmpty())
                ? t.date
                : (t.epochMillis > 0 ? df.format(t.epochMillis) : "");
        String source = t.source == null ? "" : t.source;
        if (!dateStr.isEmpty() && !source.isEmpty()) {
            h.tvTxDateSource.setText(dateStr + " • " + source);
        } else if (!dateStr.isEmpty()) {
            h.tvTxDateSource.setText(dateStr);
        } else if (!source.isEmpty()) {
            h.tvTxDateSource.setText(source);
        } else {
            h.tvTxDateSource.setText("");
        }

        View divider = h.itemView.findViewById(R.id.divider);
        if (divider != null) {
            divider.setVisibility(position == items.size() - 1 ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class DetailVH extends RecyclerView.ViewHolder {
        TextView tvTxTitle, tvTxAmount, tvTxDateSource, tvTxType;
        DetailVH(@NonNull View itemView) {
            super(itemView);
            tvTxTitle = itemView.findViewById(R.id.tvTxTitle);
            tvTxAmount = itemView.findViewById(R.id.tvTxAmount);
            tvTxDateSource = itemView.findViewById(R.id.tvTxDateSource);
            tvTxType = itemView.findViewById(R.id.tvTxType);
        }
    }
}