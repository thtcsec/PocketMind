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
    private val _categoryFilter = MutableStateFlow<String?>(null)

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

    private val _monthlyTxCount = MutableStateFlow(0)
    val monthlyTxCount: StateFlow<Int> = _monthlyTxCount.asStateFlow()

    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _topCategory = MutableStateFlow<String?>(null)
    val topCategory: StateFlow<String?> = _topCategory.asStateFlow()

    val monthlyNet: StateFlow<Double> = combine(_monthlyIncome, _totalMonthlyExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentPreview: StateFlow<List<Transaction>> = _allTransactions
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
                var totalIncome = 0.0
                var txCount = 0
                val categoryMap = mutableMapOf<String, Double>()

                for (doc in snapshots) {
                    val t = doc.toObject(Transaction::class.java) ?: continue
                    txCount++
                    if (t.type.equals("expense", ignoreCase = true)) {
                        totalExpense += t.amount
                        val cat = t.category ?: "Other"
                        categoryMap[cat] = (categoryMap[cat] ?: 0.0) + t.amount
                    } else if (t.type.equals("income", ignoreCase = true)) {
                        totalIncome += t.amount
                    }
                }

                _totalMonthlyExpense.value = totalExpense
                _monthlyIncome.value = totalIncome
                _monthlyTxCount.value = txCount
                _categoryExpenses.value = categoryMap
                _topCategory.value = categoryMap.maxByOrNull { it.value }?.key
            }
            .addOnFailureListener { e ->
                AppLogger.e("HomeViewModel", "Failed to fetch monthly expenses", e)
            }
    }

    private fun finishLoading(refreshing: Boolean) {
        if (refreshing) _isRefreshing.value = false else _isLoading.value = false
    }
}
