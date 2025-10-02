package com.moneytrackultra.cashbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Expandable wallet adapter (collapsed / expanded view types).
 * Requires Wallet.expanded boolean in the Wallet model.
 *
 * Constructor takes a single WalletToggleListener with (walletId, expanded).
 * Use submit(List<Wallet>) to update data.
 */
public class WalletAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_COLLAPSED = 0;
    private static final int TYPE_EXPANDED  = 1;

    public interface WalletToggleListener {
        void onToggle(long walletId, boolean expanded);
    }

    private final List<Wallet> wallets = new ArrayList<>();
    private final WalletToggleListener listener;

    public WalletAdapter(WalletToggleListener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<Wallet> newList) {
        if (newList == null) newList = new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new WalletDiff(wallets, newList));
        wallets.clear();
        wallets.addAll(newList);
        diff.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        Wallet w = wallets.get(position);
        return (w != null ? w.id : position);
    }

    @Override
    public int getItemViewType(int position) {
        Wallet w = wallets.get(position);
        return (w != null && w.expanded) ? TYPE_EXPANDED : TYPE_COLLAPSED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_EXPANDED) {
            return new ExpandedVH(inf.inflate(R.layout.item_wallet_expanded, parent, false));
        } else {
            return new CollapsedVH(inf.inflate(R.layout.item_wallet_collapsed, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Wallet w = wallets.get(position);
        if (w == null) return;
        if (holder instanceof CollapsedVH) ((CollapsedVH) holder).bind(w);
        else if (holder instanceof ExpandedVH) ((ExpandedVH) holder).bind(w);
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    private void toggleWallet(int pos) {
        if (pos == RecyclerView.NO_POSITION) return;
        Wallet target = wallets.get(pos);
        if (target == null) return;

        // Single expansion; collapse others
        for (Wallet w : wallets) {
            if (w != target && w.expanded) w.expanded = false;
        }
        target.expanded = !target.expanded;
        notifyDataSetChanged();
        if (listener != null) listener.onToggle(target.id, target.expanded);
    }

    /* ------------ Collapsed ------------ */
    class CollapsedVH extends RecyclerView.ViewHolder {
        final TextView tvName, tvAmount;
        final ImageView ivChevron;

        CollapsedVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWalletName);
            tvAmount = itemView.findViewById(R.id.tvWalletAmount);
            ivChevron = itemView.findViewById(R.id.ivChevron);
            itemView.setOnClickListener(v -> toggleWallet(getBindingAdapterPosition()));
        }

        void bind(Wallet w) {
            tvName.setText(nonEmpty(w.name, "Wallet"));
            tvAmount.setText(formatCurrency(w.balance));
            ivChevron.setRotation(0f);
        }
    }

    /* ------------ Expanded ------------ */
    class ExpandedVH extends RecyclerView.ViewHolder {
        final View collapsedHeader;
        final TextView tvName, tvAmount;
        final ImageView ivChevron;
        final LinearLayout transactionsContainer;

        ExpandedVH(@NonNull View itemView) {
            super(itemView);
            collapsedHeader = itemView.findViewById(R.id.collapsedHeader);
            tvName = itemView.findViewById(R.id.tvWalletName);
            tvAmount = itemView.findViewById(R.id.tvWalletAmount);
            ivChevron = itemView.findViewById(R.id.ivChevron);
            transactionsContainer = itemView.findViewById(R.id.transactionsContainer);
            collapsedHeader.setOnClickListener(v -> toggleWallet(getBindingAdapterPosition()));
        }

        void bind(Wallet w) {
            tvName.setText(nonEmpty(w.name, "Wallet"));
            tvAmount.setText(formatCurrency(w.balance));
            ivChevron.setRotation(180f);
            buildTransactions(w);
        }

        private void buildTransactions(Wallet w) {
            transactionsContainer.removeAllViews();
            if (w.transactions == null || w.transactions.isEmpty()) return;

            LayoutInflater inf = LayoutInflater.from(transactionsContainer.getContext());
            int last = w.transactions.size() - 1;

            for (int i = 0; i < w.transactions.size(); i++) {
                WalletTransaction tx = w.transactions.get(i);
                if (tx == null) continue;
                View row = inf.inflate(R.layout.item_wallet_transaction_row, transactionsContainer, false);
                TextView title = row.findViewById(R.id.tvTxTitle);
                TextView sub   = row.findViewById(R.id.tvTxSub);
                TextView amt   = row.findViewById(R.id.tvTxAmount);
                View divider   = row.findViewById(R.id.divider);

                String label = nonEmpty(tx.title, tx.amount < 0 ? "Expense" : "Income");
                title.setText(label);

                String platform = nonEmpty(tx.source, "—");
                String date = nonEmpty(tx.date, "");
                sub.setText(date.isEmpty() ? platform : date + " • " + platform);

                boolean expense = tx.amount < 0;
                long abs = Math.abs(tx.amount);
                amt.setText((expense ? "- " : "+ ") + formatCurrency(abs));
                int c = row.getContext().getColor(expense ? R.color.expenseRed : R.color.incomeGreen);
                amt.setTextColor(c);

                if (i == last && divider != null) divider.setVisibility(View.GONE);
                transactionsContainer.addView(row);
            }
        }
    }

    /* ------------ Helpers ------------ */
    private String nonEmpty(String s, String def) {
        return (s == null || s.trim().isEmpty()) ? def : s;
    }

    private String formatCurrency(long majorUnits) {
        // Adjust to whichever one you standardized on:
        // return CurrencyUtil.rupiah(majorUnits);
        return CurrencyUtil.formatMajor(majorUnits);
    }

    /* ------------ DiffUtil ------------ */
    static class WalletDiff extends DiffUtil.Callback {
        private final List<Wallet> oldList;
        private final List<Wallet> newList;

        WalletDiff(List<Wallet> oldList, List<Wallet> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int o, int n) {
            Wallet a = oldList.get(o);
            Wallet b = newList.get(n);
            return a != null && b != null && a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(int o, int n) {
            Wallet a = oldList.get(o);
            Wallet b = newList.get(n);
            if (a == null || b == null) return false;
            if (a.expanded != b.expanded) return false;
            if (a.balance != b.balance) return false;
            String an = a.name == null ? "" : a.name;
            String bn = b.name == null ? "" : b.name;
            return an.equals(bn);
        }
    }
}