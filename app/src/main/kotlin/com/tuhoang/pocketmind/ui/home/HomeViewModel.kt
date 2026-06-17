package com.tuhoang.pocketmind.ui.home

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tuhoang.pocketmind.data.models.Transaction
import com.tuhoang.pocketmind.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions: StateFlow<List<Transaction>> = _recentTransactions.asStateFlow()

    private val _totalMonthlyExpense = MutableStateFlow(0.0)
    val totalMonthlyExpense: StateFlow<Double> = _totalMonthlyExpense.asStateFlow()

    private val _categoryExpenses = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryExpenses: StateFlow<Map<String, Double>> = _categoryExpenses.asStateFlow()

    fun fetchHomeData() {
        val user = auth.currentUser ?: run {
            _totalMonthlyExpense.value = 0.0
            _recentTransactions.value = emptyList()
            _categoryExpenses.value = emptyMap()
            return
        }

        val uid = user.uid

        db.collection("users").document(uid).collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshots ->
                _recentTransactions.value = snapshots.mapNotNull { it.toObject(Transaction::class.java) }
            }
            .addOnFailureListener { e ->
                AppLogger.e("HomeViewModel", "Failed to fetch recent transactions", e)
            }

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val startOfMonth: Date = cal.time

        db.collection("users").document(uid).collection("expenses")
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .get()
            .addOnSuccessListener { snapshots ->
                var totalExpense = 0.0
                val categoryMap = mutableMapOf<String, Double>()

                for (doc in snapshots) {
                    val t = doc.toObject(Transaction::class.java) ?: continue
                    if (t.type.equals("expense", ignoreCase = true)) {
                        totalExpense += t.amount
                        val cat = t.category ?: "Other"
                        categoryMap[cat] = (categoryMap[cat] ?: 0.0) + t.amount
                    }
                }

                _totalMonthlyExpense.value = totalExpense
                _categoryExpenses.value = categoryMap
            }
            .addOnFailureListener { e ->
                AppLogger.e("HomeViewModel", "Failed to fetch monthly expenses", e)
            }
    }
}
