package com.tuhoang.pocketmind.ui.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.FragmentReportBinding;
import com.tuhoang.pocketmind.utils.AppLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ReportFragment extends Fragment {
    private FragmentReportBinding binding;
    private ReportViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        setupDateRangePicker();
        setupChartToggles();
        observeViewModel();
        
        viewModel.fetchReportData();
    }

    private void observeViewModel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        viewModel.getStartDate().observe(getViewLifecycleOwner(), startDate -> {
            Date endDate = viewModel.getEndDate().getValue();
            if (startDate != null && endDate != null) {
                binding.tvDateRange.setText(sdf.format(startDate) + " - " + sdf.format(endDate));
            }
        });
        
        viewModel.getEndDate().observe(getViewLifecycleOwner(), endDate -> {
            Date startDate = viewModel.getStartDate().getValue();
            if (startDate != null && endDate != null) {
                binding.tvDateRange.setText(sdf.format(startDate) + " - " + sdf.format(endDate));
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                binding.tvDateRange.append(" (Loading...)");
            } else {
                Date start = viewModel.getStartDate().getValue();
                Date end = viewModel.getEndDate().getValue();
                if (start != null && end != null) {
                    binding.tvDateRange.setText(sdf.format(start) + " - " + sdf.format(end));
                }
            }
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            binding.tvTotalIncome.setText(String.format(Locale.US, "$%.2f", income));
        });

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> {
            binding.tvTotalReportExpense.setText(String.format(Locale.US, "$%.2f", expense));
        });

        viewModel.getCategoryExpenses().observe(getViewLifecycleOwner(), this::renderData);
    }

    private void setupDateRangePicker() {

        binding.cardDateRange.setOnClickListener(v -> {
            MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
            builder.setTitleText("Select Date Range");
            final MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

            picker.addOnPositiveButtonClickListener(selection -> {
                if (selection.first != null && selection.second != null) {
                    // Fix timezone offset for display
                    long offsetFirst = TimeZone.getDefault().getOffset(selection.first);
                    long offsetSecond = TimeZone.getDefault().getOffset(selection.second);
                    
                    Date currentStartDate = new Date(selection.first - offsetFirst);
                    Date currentEndDate = new Date(selection.second - offsetSecond);
                    
                    // Adjust end date to 23:59:59
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(currentEndDate);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    currentEndDate = cal.getTime();
                    
                    viewModel.setDateRange(currentStartDate, currentEndDate);
                }
            });

            picker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
        });
    }

    private void setupChartToggles() {
        binding.toggleChartType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                // Smooth transition
                AutoTransition transition = new AutoTransition();
                transition.setDuration(200);
                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), transition);

                if (checkedId == R.id.btnBarChart) {
                    binding.barChart.setVisibility(View.VISIBLE);
                    binding.pieChart.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnPieChart) {
                    binding.barChart.setVisibility(View.GONE);
                    binding.pieChart.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void renderData(Map<String, Double> categoryExpenses) {

        if (categoryExpenses.isEmpty()) {
            binding.tvTopCategory.setText("No expenses in this period");
            binding.barChart.clear();
            binding.pieChart.clear();
            return;
        }

        // Find top category
        String topCategory = "";
        double maxExpense = -1;
        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            if (entry.getValue() > maxExpense) {
                maxExpense = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        binding.tvTopCategory.setText(String.format(Locale.US, "Top Category: %s ($%.2f)", topCategory, maxExpense));

        // --- Setup BarChart ---
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        
        int xIndex = 1;
        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            barEntries.add(new BarEntry(xIndex++, entry.getValue().floatValue()));
            pieEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Expenses by Category");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData barData = new BarData(barDataSet);
        binding.barChart.setData(barData);
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChart.animateY(800);

        // --- Setup PieChart ---
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        pieDataSet.setValueTextSize(14f);
        pieDataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(pieDataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setCenterText("Expenses");
        binding.pieChart.setCenterTextSize(16f);
        binding.pieChart.animateY(800);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
