package com.tuhoang.pocketmind.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.data.models.Transaction
import com.tuhoang.pocketmind.ui.components.BudgetProgressBar
import com.tuhoang.pocketmind.ui.components.EmptyState
import com.tuhoang.pocketmind.ui.components.HomeSkeleton
import com.tuhoang.pocketmind.ui.components.PieChart
import com.tuhoang.pocketmind.ui.components.SectionCard
import com.tuhoang.pocketmind.ui.components.rememberShowSnackbar
import com.tuhoang.pocketmind.ui.theme.GreenPrimary
import com.tuhoang.pocketmind.ui.theme.RedExpense
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val totalExpense by viewModel.totalMonthlyExpense.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val budgetAlert by viewModel.budgetAlert.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val showSnackbar = rememberShowSnackbar()
    val budgetExceededMsg = stringResource(R.string.budget_exceeded)
    val budgetWarningMsg = stringResource(R.string.budget_warning)

    LaunchedEffect(Unit) { viewModel.fetchHomeData() }

    LaunchedEffect(budgetAlert) {
        when (budgetAlert) {
            BudgetAlert.EXCEEDED -> showSnackbar(budgetExceededMsg)
            BudgetAlert.WARNING -> showSnackbar(budgetWarningMsg)
            BudgetAlert.NONE -> Unit
        }
    }

    val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.fetchHomeData(refreshing = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && recentTransactions.isEmpty()) {
            HomeSkeleton(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = stringResource(R.string.section_monthly_overview),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.home_total_expenses),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = formatter.format(totalExpense),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.home_this_month),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            if (budget > 0) {
                                Text(
                                    text = stringResource(R.string.budget_label, formatter.format(budget)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                                BudgetProgressBar(
                                    spent = totalExpense,
                                    budget = budget,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.search_transactions)) },
                        singleLine = true
                    )
                }

                if (categories.isNotEmpty()) {
                    item {
                        CategoryFilterDropdown(
                            categories = categories,
                            selected = categoryFilter,
                            onSelected = { viewModel.setCategoryFilter(it) }
                        )
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.section_spending_by_category)) {
                        if (categoryExpenses.isEmpty()) {
                            EmptyState(message = stringResource(R.string.chart_no_data))
                        } else {
                            PieChart(
                                data = categoryExpenses,
                                centerText = stringResource(R.string.home_expenses_chart_center)
                            )
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.home_recent_transactions)) {
                        if (recentTransactions.isEmpty()) {
                            EmptyState(message = stringResource(R.string.home_no_transactions))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                recentTransactions.forEach { transaction ->
                                    TransactionItem(transaction, formatter)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterDropdown(
    categories: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected ?: stringResource(R.string.filter_all_categories),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_category)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all_categories)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = {
                        onSelected(cat)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, formatter: NumberFormat) {
    val isIncome = transaction.type.equals("income", ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category ?: stringResource(R.string.category_other),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                transaction.note?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${if (isIncome) "+" else "-"}${formatter.format(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (isIncome) GreenPrimary else RedExpense,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
