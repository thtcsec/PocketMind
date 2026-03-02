package com.tuhoang.pocketmind.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.tuhoang.pocketmind.data.models.Transaction;
import com.tuhoang.pocketmind.databinding.FragmentHomeBinding;
import com.tuhoang.pocketmind.databinding.FragmentHomeBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private TransactionAdapter adapter;
    private HomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerView();
        observeViewModel();
        
        viewModel.fetchHomeData();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentTransactions.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), list -> {
            if (adapter != null) {
                adapter.setTransactions(list);
            }
        });

        viewModel.getTotalMonthlyExpense().observe(getViewLifecycleOwner(), total -> {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            binding.tvTotalExpense.setText(formatter.format(total));
            binding.tvComparison.setText("This Month");
        });

        viewModel.getCategoryExpenses().observe(getViewLifecycleOwner(), this::renderPieChart);
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
