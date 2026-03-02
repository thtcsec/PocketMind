package com.tuhoang.pocketmind.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tuhoang.pocketmind.data.models.Transaction;
import com.tuhoang.pocketmind.utils.AppLogger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<Transaction>> recentTransactions = new MutableLiveData<>();
    private final MutableLiveData<Double> totalMonthlyExpense = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Double>> categoryExpenses = new MutableLiveData<>();

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }

    public LiveData<Double> getTotalMonthlyExpense() {
        return totalMonthlyExpense;
    }

    public LiveData<Map<String, Double>> getCategoryExpenses() {
        return categoryExpenses;
    }

    public void fetchHomeData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            totalMonthlyExpense.setValue(0.0);
            return;
        }

        String uid = user.getUid();

        // 1. Fetch Recent Transactions
        db.collection("users").document(uid).collection("expenses")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t != null) list.add(t);
                    }
                    recentTransactions.setValue(list);
                })
                .addOnFailureListener(e -> AppLogger.e("HomeViewModel", "Failed to fetch recent transactions", e));

        // 2. Fetch Monthly Data
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startOfMonth = cal.getTime();

        db.collection("users").document(uid).collection("expenses")
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

                    totalMonthlyExpense.setValue(totalExpense);
                    categoryExpenses.setValue(categoryMap);
                })
                .addOnFailureListener(e -> AppLogger.e("HomeViewModel", "Failed to fetch monthly expenses", e));
    }
}
