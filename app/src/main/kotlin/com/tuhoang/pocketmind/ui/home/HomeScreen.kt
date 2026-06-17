package com.tuhoang.pocketmind.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.tuhoang.pocketmind.ui.components.PieChart
import com.tuhoang.pocketmind.ui.theme.GreenPrimary
import com.tuhoang.pocketmind.ui.theme.RedExpense
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val totalExpense by viewModel.totalMonthlyExpense.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchHomeData() }

    val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.home_total_expenses),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = formatter.format(totalExpense),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This Month",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PieChart(
                        data = categoryExpenses,
                        centerText = "Expenses\nThis Month"
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_recent_transactions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(recentTransactions) { transaction ->
            TransactionItem(transaction, formatter)
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, formatter: NumberFormat) {
    val isIncome = transaction.type.equals("income", ignoreCase = true)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category ?: "Other",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                transaction.note?.takeIf { it.isNotEmpty() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
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
