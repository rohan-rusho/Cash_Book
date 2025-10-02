package com.moneytrackultra.cashbook;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BalanceAggregateAdapter extends RecyclerView.Adapter<BalanceAggregateAdapter.AggVH> {

    private final List<BalanceAggregate> data = new ArrayList<>();
    private ViewGroup parentRef;

    public interface ExpansionListener {
        void onExpanded(BalanceAggregate agg);
    }

    private ExpansionListener expansionListener;

    public void setExpansionListener(ExpansionListener listener) {
        this.expansionListener = listener;
    }

    public void submit(List<BalanceAggregate> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AggVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        parentRef = parentRef == null ? parent : parentRef;
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_balance_aggregate, parent, false);
        return new AggVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AggVH h, int position) {
        h.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class AggVH extends RecyclerView.ViewHolder {
        TextView tvName, tvAmount;
        ImageView ivChevron;
        View header;
        RecyclerView rvDetails;
        BalanceDetailAdapter detailAdapter;

        AggVH(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
            tvName = itemView.findViewById(R.id.tvAggName);
            tvAmount = itemView.findViewById(R.id.tvAggAmount);
            ivChevron = itemView.findViewById(R.id.ivChevron);
            rvDetails = itemView.findViewById(R.id.rvDetails);
            rvDetails.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(itemView.getContext()));
            detailAdapter = new BalanceDetailAdapter();
            rvDetails.setAdapter(detailAdapter);

            header.setOnClickListener(v -> toggle(getBindingAdapterPosition()));
        }

        void bind(BalanceAggregate agg) {
            tvName.setText(agg.key);
            long net = agg.netMinor();
            tvAmount.setText(CurrencyUtil.formatMinor(net));
            int color = itemView.getContext().getColor(net >= 0 ? R.color.incomeGreen : R.color.expenseRed);
            tvAmount.setTextColor(color);

            ivChevron.setRotation(agg.expanded ? 180f : 0f);
            rvDetails.setVisibility(agg.expanded ? View.VISIBLE : View.GONE);

            if (agg.expanded) {
                // Sort newest first
                List<Transaction> copy = new ArrayList<>(agg.transactions);
                Collections.sort(copy, Comparator.comparingLong(t -> -t.epochMillis));
                detailAdapter.submit(copy);
            }
        }

        private void toggle(int pos) {
            if (pos == RecyclerView.NO_POSITION) return;
            BalanceAggregate target = data.get(pos);

            // Single expand behavior
            for (BalanceAggregate a : data) {
                if (a != target && a.expanded) a.expanded = false;
            }
            target.expanded = !target.expanded;

            // Animated transition
            if (parentRef != null) {
                TransitionManager.beginDelayedTransition(parentRef, new AutoTransition());
            }
            notifyDataSetChanged();

            if (expansionListener != null && target.expanded) {
                expansionListener.onExpanded(target);
            }
        }
    }
}