package com.tuhoang.pocketmind.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuhoang.pocketmind.data.models.Transaction;
import com.tuhoang.pocketmind.databinding.ItemTransactionBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<Transaction> transactions = new ArrayList<>();

    public void setTransactions(List<Transaction> newTransactions) {
        transactions.clear();
        transactions.addAll(newTransactions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction t = transactions.get(position);
        holder.bind(t);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;
        private final NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

        public TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Transaction t) {
            String category = t.getCategory();
            if (category == null || category.isEmpty()) category = "Other";
            binding.tvCategory.setText(category);

            String note = t.getNote();
            if (note == null || note.isEmpty()) {
                binding.tvNote.setText("No description");
            } else {
                binding.tvNote.setText(note);
            }

            // Format Amount
            String formattedAmount = formatter.format(t.getAmount());
            if ("income".equalsIgnoreCase(t.getType())) {
                binding.tvAmount.setText("+" + formattedAmount);
                binding.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else {
                binding.tvAmount.setText("-" + formattedAmount);
                binding.tvAmount.setTextColor(Color.parseColor("#F44336")); // Red
            }
        }
    }
}
