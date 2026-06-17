package com.tuhoang.pocketmind.ui.home

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tuhoang.pocketmind.data.models.Transaction
import com.tuhoang.pocketmind.utils.AppLogger
import com.tuhoang.pocketmind.utils.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import java.util.Calendar
import java.util.Date

enum class BudgetAlert { NONE, WARNING, EXCEEDED }

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val prefs = PrefsManager.getInstance()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _totalMonthlyExpense = MutableStateFlow(0.0)
    val totalMonthlyExpense: StateFlow<Double> = _totalMonthlyExpense.asStateFlow()

    private val _categoryExpenses = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryExpenses: StateFlow<Map<String, Double>> = _categoryExpenses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    val monthlyBudget: StateFlow<Double> get() = budget

    private val _budgetValue = MutableStateFlow(prefs.getMonthlyBudget())
    val budget: StateFlow<Double> = _budgetValue.asStateFlow()

    val budgetAlert: StateFlow<BudgetAlert> = combine(_totalMonthlyExpense, _budgetValue) { spent, budget ->
        when {
            budget <= 0 -> BudgetAlert.NONE
            spent >= budget -> BudgetAlert.EXCEEDED
            spent >= budget * 0.8 -> BudgetAlert.WARNING
            else -> BudgetAlert.NONE
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetAlert.NONE)

    val recentTransactions: StateFlow<List<Transaction>> = combine(
        _allTransactions,
        _searchQuery,
        _categoryFilter
    ) { list, query, category ->
        list.filter { tx ->
            val matchesQuery = query.isBlank() ||
                tx.category?.contains(query, ignoreCase = true) == true ||
                tx.note?.contains(query, ignoreCase = true) == true
            val matchesCategory = category.isNullOrBlank() || tx.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCategories: StateFlow<List<String>> = _allTransactions
        .map { list -> list.mapNotNull { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategoryFilter(category: String?) { _categoryFilter.value = category }
    fun refreshBudget() { _budgetValue.value = prefs.getMonthlyBudget() }

    fun fetchHomeData(refreshing: Boolean = false) {
        val user = auth.currentUser ?: run {
            _isLoading.value = false
            _isRefreshing.value = false
            _totalMonthlyExpense.value = 0.0
            _allTransactions.value = emptyList()
            _categoryExpenses.value = emptyMap()
            return
        }

        if (refreshing) _isRefreshing.value = true else _isLoading.value = true
        _budgetValue.value = prefs.getMonthlyBudget()
        val uid = user.uid

        db.collection("users").document(uid).collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshots ->
                _allTransactions.value = snapshots.mapNotNull { it.toObject(Transaction::class.java) }
                finishLoading(refreshing)
            }
            .addOnFailureListener { e ->
                AppLogger.e("HomeViewModel", "Failed to fetch transactions", e)
                finishLoading(refreshing)
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

    private fun finishLoading(refreshing: Boolean) {
        if (refreshing) _isRefreshing.value = false else _isLoading.value = false
    }
}
