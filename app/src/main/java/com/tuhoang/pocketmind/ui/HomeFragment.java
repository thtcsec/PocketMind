package com.tuhoang.pocketmind.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tuhoang.pocketmind.data.models.Transaction;
import com.tuhoang.pocketmind.databinding.FragmentHomeBinding;
import com.tuhoang.pocketmind.ui.adapters.TransactionAdapter;
import com.tuhoang.pocketmind.utils.AppLogger;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
        fetchHomeData();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentTransactions.setAdapter(adapter);
    }

    private void fetchHomeData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            binding.tvTotalExpense.setText("$0.00");
            return;
        }

        // --- Fetch Recent Transactions ---
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).collection("expenses")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t != null) list.add(t);
                    }
                    adapter.setTransactions(list);
                })
                .addOnFailureListener(e -> AppLogger.e("HomeFragment", "Failed to fetch recent transactions", e));

        // --- Fetch Monthly Data for Pie Chart & Totals ---
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startOfMonth = cal.getTime();

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).collection("expenses")
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalExpense = 0;
                    Map<String, Double> categoryMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t != null && "expense".equalsIgnoreCase(t.getType())) {
                            totalExpense += t.getAmount();
                            String cat = t.getCategory() != null ? t.getCategory() : "Other";
                            categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + t.getAmount());
                        }
                    }

                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    binding.tvTotalExpense.setText(formatter.format(totalExpense));
                    binding.tvComparison.setText("This Month");

                    renderPieChart(categoryMap);
                })
                .addOnFailureListener(e -> AppLogger.e("HomeFragment", "Failed to fetch monthly expenses", e));
    }

    private void renderPieChart(Map<String, Double> categoryMap) {
        if (categoryMap.isEmpty()) {
            binding.pieChart.clear();
            return;
        }

        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            pieEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pieDataSet.setValueTextSize(12f);
        pieDataSet.setValueTextColor(android.graphics.Color.WHITE);

        PieData pieData = new PieData(pieDataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setCenterText("Expenses\nThis Month");
        binding.pieChart.animateY(800);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
