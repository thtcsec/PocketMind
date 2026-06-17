package com.tuhoang.pocketmind.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.tuhoang.pocketmind.utils.MoneyFormatter
import com.tuhoang.pocketmind.utils.rememberMoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val recentPreview by viewModel.recentPreview.collectAsState()
    val totalExpense by viewModel.totalMonthlyExpense.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyNet by viewModel.monthlyNet.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val monthlyTxCount by viewModel.monthlyTxCount.collectAsState()
    val topCategory by viewModel.topCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val budgetAlert by viewModel.budgetAlert.collectAsState()
    val showSnackbar = rememberShowSnackbar()
    val budgetExceededMsg = stringResource(R.string.budget_exceeded)
    val budgetWarningMsg = stringResource(R.string.budget_warning)
    val money = rememberMoneyFormatter()

    LaunchedEffect(Unit) { viewModel.fetchHomeData() }

    LaunchedEffect(budgetAlert) {
        when (budgetAlert) {
            BudgetAlert.EXCEEDED -> showSnackbar(budgetExceededMsg)
            BudgetAlert.WARNING -> showSnackbar(budgetWarningMsg)
            BudgetAlert.NONE -> Unit
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.fetchHomeData(refreshing = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && recentPreview.isEmpty()) {
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
                                text = money.format(totalExpense),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.home_income_label, money.format(monthlyIncome)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = stringResource(R.string.home_net_label, money.format(monthlyNet, signed = true)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (monthlyNet >= 0) GreenPrimary else RedExpense
                                )
                            }
                            if (budget > 0) {
                                Text(
                                    text = stringResource(R.string.budget_label, money.format(budget)),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InsightChip(
                            icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                            label = stringResource(R.string.home_tx_count, monthlyTxCount),
                            modifier = Modifier.weight(1f)
                        )
                        InsightChip(
                            icon = { Icon(Icons.Default.Category, contentDescription = null) },
                            label = topCategory?.let { stringResource(R.string.home_top_cat_short, it) }
                                ?: stringResource(R.string.home_no_top_category),
                            modifier = Modifier.weight(1f)
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
                        Text(
                            text = stringResource(R.string.home_see_report_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (recentPreview.isEmpty()) {
                            EmptyState(message = stringResource(R.string.home_no_transactions))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                recentPreview.forEach { transaction ->
                                    TransactionItem(transaction, money)
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
private fun InsightChip(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, money: MoneyFormatter) {
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
                text = money.format(
                    if (isIncome) transaction.amount else -transaction.amount,
                    signed = true
                ),
                style = MaterialTheme.typography.titleSmall,
                color = if (isIncome) GreenPrimary else RedExpense,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
