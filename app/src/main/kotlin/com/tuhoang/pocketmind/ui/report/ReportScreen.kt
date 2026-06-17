package com.tuhoang.pocketmind.ui.report

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.data.models.Transaction
import com.tuhoang.pocketmind.ui.components.BarChart
import com.tuhoang.pocketmind.ui.components.CategoryLegend
import com.tuhoang.pocketmind.ui.components.EmptyState
import com.tuhoang.pocketmind.ui.components.PieChart
import com.tuhoang.pocketmind.ui.components.ReportSkeleton
import com.tuhoang.pocketmind.ui.components.SectionCard
import com.tuhoang.pocketmind.ui.components.rememberShowSnackbar
import com.tuhoang.pocketmind.ui.theme.GreenPrimary
import com.tuhoang.pocketmind.ui.theme.RedExpense
import com.tuhoang.pocketmind.utils.CsvExporter
import com.tuhoang.pocketmind.utils.HapticUtils
import com.tuhoang.pocketmind.utils.MoneyFormatter
import com.tuhoang.pocketmind.utils.rememberMoneyFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = viewModel()) {
    val context = LocalContext.current
    val showSnackbar = rememberShowSnackbar()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()

    var chartType by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.fetchReportData() }

    val loadingSuffix = stringResource(R.string.report_loading_suffix)
    val money = rememberMoneyFormatter()
    val sdf = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    val dateRangeText = buildString {
        val start = startDate
        val end = endDate
        if (start != null && end != null) {
            append(sdf.format(start))
            append(" - ")
            append(sdf.format(end))
        }
        if (isLoading) append(loadingSuffix)
    }

    val topCategory = categoryExpenses.maxByOrNull { it.value }
    val categories = remember(categoryExpenses) { categoryExpenses.keys.sorted() }

    fun exportCsv() {
        HapticUtils.performClick(context)
        val uri = CsvExporter.export(context, filteredTransactions)
        if (uri == null) {
            showSnackbar(context.getString(R.string.export_no_data))
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_share_title)))
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val startMillis = datePickerState.selectedStartDateMillis
                    val endMillis = datePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        val offsetStart = TimeZone.getDefault().getOffset(startMillis)
                        val offsetEnd = TimeZone.getDefault().getOffset(endMillis)
                        val start = java.util.Date(startMillis - offsetStart)
                        val cal = Calendar.getInstance().apply {
                            time = java.util.Date(endMillis - offsetEnd)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }
                        viewModel.setDateRange(start, cal.time)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DateRangePicker(state = datePickerState)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.fetchReportData(refreshing = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && filteredTransactions.isEmpty() && categoryExpenses.isEmpty()) {
            ReportSkeleton(modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SectionCard(
                        title = stringResource(R.string.report_select_date_range),
                        modifier = Modifier.clickable { showDatePicker = true }
                    ) {
                        Text(
                            text = dateRangeText.ifBlank { stringResource(R.string.report_select_date_range) },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.search_transactions)) },
                            singleLine = true
                        )
                        IconButton(onClick = { exportCsv() }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_csv))
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = categoryFilter ?: stringResource(R.string.filter_all_categories),
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
                                        viewModel.setCategoryFilter(null)
                                        expanded = false
                                    }
                                )
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            viewModel.setCategoryFilter(cat)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.section_summary)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(
                                stringResource(R.string.report_income),
                                money.format(totalIncome),
                                GreenPrimary,
                                Modifier.weight(1f)
                            )
                            SummaryCard(
                                stringResource(R.string.report_expense),
                                money.format(totalExpense),
                                RedExpense,
                                Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = topCategory?.let {
                                stringResource(
                                    R.string.report_top_category,
                                    it.key,
                                    money.format(it.value)
                                )
                            } ?: stringResource(R.string.report_no_expenses),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.section_charts)) {
                        if (categoryExpenses.isEmpty()) {
                            EmptyState(message = stringResource(R.string.chart_no_data))
                        } else {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = chartType == 0,
                                    onClick = { chartType = 0 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) { Text(stringResource(R.string.report_chart_bar)) }
                                SegmentedButton(
                                    selected = chartType == 1,
                                    onClick = { chartType = 1 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) { Text(stringResource(R.string.report_chart_pie)) }
                            }
                            if (chartType == 0) {
                                BarChart(data = categoryExpenses, modifier = Modifier.padding(top = 12.dp))
                            } else {
                                PieChart(
                                    data = categoryExpenses,
                                    centerText = stringResource(R.string.report_expenses_center),
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                            CategoryLegend(data = categoryExpenses, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.report_transaction_list)) {
                        if (filteredTransactions.isEmpty()) {
                            EmptyState(message = stringResource(R.string.home_no_transactions))
                        }
                    }
                }

                items(filteredTransactions) { tx ->
                    TransactionRow(tx, money)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, money: MoneyFormatter) {
    val isIncome = transaction.type.equals("income", ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.category ?: stringResource(R.string.category_other), fontWeight = FontWeight.Medium)
                transaction.note?.takeIf { it.isNotEmpty() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = money.format(
                    if (isIncome) transaction.amount else -transaction.amount,
                    signed = true
                ),
                color = if (isIncome) GreenPrimary else RedExpense,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
