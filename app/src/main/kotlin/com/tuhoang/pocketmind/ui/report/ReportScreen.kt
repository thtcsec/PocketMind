package com.tuhoang.pocketmind.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuhoang.pocketmind.ui.components.BarChart
import com.tuhoang.pocketmind.ui.components.CategoryLegend
import com.tuhoang.pocketmind.ui.components.PieChart
import com.tuhoang.pocketmind.ui.theme.GreenPrimary
import com.tuhoang.pocketmind.ui.theme.RedExpense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = viewModel()) {
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var chartType by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.fetchReportData() }

    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateRangeText = buildString {
        val start = startDate
        val end = endDate
        if (start != null && end != null) {
            append(sdf.format(start))
            append(" - ")
            append(sdf.format(end))
        }
        if (isLoading) append(" (Loading...)")
    }

    val topCategory = categoryExpenses.maxByOrNull { it.value }

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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = dateRangeText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Income", "$${String.format(Locale.US, "%.2f", totalIncome)}", GreenPrimary, Modifier.weight(1f))
                SummaryCard("Expense", "$${String.format(Locale.US, "%.2f", totalExpense)}", RedExpense, Modifier.weight(1f))
            }
        }

        item {
            Text(
                text = topCategory?.let { "Top Category: ${it.key} ($${String.format("%.2f", it.value)})" }
                    ?: "No expenses in this period",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = chartType == 0,
                    onClick = { chartType = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Bar") }
                SegmentedButton(
                    selected = chartType == 1,
                    onClick = { chartType = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Pie") }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (chartType == 0) {
                        BarChart(data = categoryExpenses)
                    } else {
                        PieChart(data = categoryExpenses, centerText = "Expenses")
                    }
                    CategoryLegend(data = categoryExpenses, modifier = Modifier.padding(top = 8.dp))
                }
            }
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
