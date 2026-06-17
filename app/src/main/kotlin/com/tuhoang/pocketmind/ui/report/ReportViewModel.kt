package com.tuhoang.pocketmind.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.data.models.Transaction
import com.tuhoang.pocketmind.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _allTransactions,
        _searchQuery,
        _categoryFilter
    ) { list, query, category ->
        list.filter { tx ->
            val matchesQuery = query.isBlank() ||
                tx.category?.contains(query, ignoreCase = true) == true ||
                tx.note?.contains(query, ignoreCase = true) == true ||
                tx.type?.contains(query, ignoreCase = true) == true
            val matchesCategory = category.isNullOrBlank() || tx.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategoryFilter(category: String?) { _categoryFilter.value = category }

    fun setDateRange(start: Date, end: Date) {
        _startDate.value = start
        _endDate.value = end
        fetchReportData()
    }

    fun fetchReportData(refreshing: Boolean = false) {
        val user = auth.currentUser
        val start = _startDate.value
        val end = _endDate.value

        if (user == null || start == null || end == null) {
            clearData()
            return
        }

        if (refreshing) _isRefreshing.value = true else _isLoading.value = true

        db.collection("users").document(user.uid).collection("expenses")
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .get()
            .addOnSuccessListener { snapshots ->
                var income = 0.0
                var expense = 0.0
                val categoryMap = mutableMapOf<String, Double>()
                val transactions = mutableListOf<Transaction>()

                for (doc in snapshots) {
                    val t = doc.toObject(Transaction::class.java) ?: continue
                    transactions.add(t)
                    if (t.type.equals("income", ignoreCase = true)) {
                        income += t.amount
                    } else {
                        expense += t.amount
                        val cat = t.category ?: "Other"
                        categoryMap[cat] = (categoryMap[cat] ?: 0.0) + t.amount
                    }
                }

                _allTransactions.value = transactions.sortedByDescending { it.timestamp?.time ?: 0L }
                _totalIncome.value = income
                _totalExpense.value = expense
                _categoryExpenses.value = categoryMap
                finishLoading(refreshing)
            }
            .addOnFailureListener { e ->
                AppLogger.e("ReportViewModel", "Failed to fetch report data", e)
                clearData()
                finishLoading(refreshing)
            }
    }

    private fun finishLoading(refreshing: Boolean) {
        if (refreshing) _isRefreshing.value = false else _isLoading.value = false
    }

    private fun clearData() {
        _totalIncome.value = 0.0
        _totalExpense.value = 0.0
        _categoryExpenses.value = emptyMap()
        _allTransactions.value = emptyList()
    }
}
