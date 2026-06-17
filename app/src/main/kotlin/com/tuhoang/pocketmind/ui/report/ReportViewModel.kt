package com.tuhoang.pocketmind.ui.report

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.data.models.Transaction
import com.tuhoang.pocketmind.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date

class ReportViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _startDate = MutableStateFlow<Date?>(null)
    val startDate: StateFlow<Date?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Date?>(null)
    val endDate: StateFlow<Date?> = _endDate.asStateFlow()

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome.asStateFlow()

    private val _totalExpense = MutableStateFlow(0.0)
    val totalExpense: StateFlow<Double> = _totalExpense.asStateFlow()

    private val _categoryExpenses = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryExpenses: StateFlow<Map<String, Double>> = _categoryExpenses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        _endDate.value = cal.time

        cal.add(Calendar.DAY_OF_YEAR, -7)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        _startDate.value = cal.time
    }

    fun setDateRange(start: Date, end: Date) {
        _startDate.value = start
        _endDate.value = end
        fetchReportData()
    }

    fun fetchReportData() {
        val user = auth.currentUser
        val start = _startDate.value
        val end = _endDate.value

        if (user == null || start == null || end == null) {
            clearData()
            return
        }

        _isLoading.value = true

        db.collection("users").document(user.uid).collection("expenses")
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .get()
            .addOnSuccessListener { snapshots ->
                var income = 0.0
                var expense = 0.0
                val categoryMap = mutableMapOf<String, Double>()

                for (doc in snapshots) {
                    val t = doc.toObject(Transaction::class.java) ?: continue
                    if (t.type.equals("income", ignoreCase = true)) {
                        income += t.amount
                    } else {
                        expense += t.amount
                        val cat = t.category ?: "Other"
                        categoryMap[cat] = (categoryMap[cat] ?: 0.0) + t.amount
                    }
                }

                _totalIncome.value = income
                _totalExpense.value = expense
                _categoryExpenses.value = categoryMap
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                AppLogger.e("ReportViewModel", "Failed to fetch report data", e)
                clearData()
                _isLoading.value = false
            }
    }

    private fun clearData() {
        _totalIncome.value = 0.0
        _totalExpense.value = 0.0
        _categoryExpenses.value = emptyMap()
    }
}
