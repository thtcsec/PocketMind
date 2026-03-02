package com.tuhoang.pocketmind.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tuhoang.pocketmind.data.models.Transaction;
import com.tuhoang.pocketmind.utils.AppLogger;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReportViewModel extends ViewModel {

    private final MutableLiveData<Date> startDate = new MutableLiveData<>();
    private final MutableLiveData<Date> endDate = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Double>> categoryExpenses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ReportViewModel() {
        // Default to this week
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        endDate.setValue(cal.getTime());
        
        cal.add(Calendar.DAY_OF_YEAR, -7);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        startDate.setValue(cal.getTime());
    }

    public LiveData<Date> getStartDate() { return startDate; }
    public LiveData<Date> getEndDate() { return endDate; }
    public LiveData<Double> getTotalIncome() { return totalIncome; }
    public LiveData<Double> getTotalExpense() { return totalExpense; }
    public LiveData<Map<String, Double>> getCategoryExpenses() { return categoryExpenses; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void setDateRange(Date start, Date end) {
        startDate.setValue(start);
        endDate.setValue(end);
        fetchReportData();
    }

    public void fetchReportData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || startDate.getValue() == null || endDate.getValue() == null) {
            clearData();
            return;
        }

        isLoading.setValue(true);

        db.collection("users").document(user.getUid()).collection("expenses")
                .whereGreaterThanOrEqualTo("timestamp", startDate.getValue())
                .whereLessThanOrEqualTo("timestamp", endDate.getValue())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double income = 0;
                    double expense = 0;
                    Map<String, Double> categoryMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t == null) continue;

                        if ("income".equalsIgnoreCase(t.getType())) {
                            income += t.getAmount();
                        } else {
                            expense += t.getAmount();
                            String cat = t.getCategory() != null ? t.getCategory() : "Other";
                            categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + t.getAmount());
                        }
                    }

                    totalIncome.setValue(income);
                    totalExpense.setValue(expense);
                    categoryExpenses.setValue(categoryMap);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    AppLogger.e("ReportViewModel", "Failed to fetch report data", e);
                    clearData();
                    isLoading.setValue(false);
                });
    }

    private void clearData() {
        totalIncome.setValue(0.0);
        totalExpense.setValue(0.0);
        categoryExpenses.setValue(new HashMap<>());
    }
}
