package com.tuhoang.pocketmind.ui.settings.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.settings.components.SettingsNavRow
import com.tuhoang.pocketmind.ui.settings.components.SettingsSectionHeader
import com.tuhoang.pocketmind.ui.settings.components.deleteCacheDir
import com.tuhoang.pocketmind.ui.settings.components.formatCacheSize
import com.tuhoang.pocketmind.ui.settings.components.getDirSize
import com.tuhoang.pocketmind.utils.PrefsManager

@Composable
fun DataStorageSettingsSection(prefs: PrefsManager) {
    val context = LocalContext.current
    var currency by remember { mutableStateOf(prefs.getCurrency()) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(prefs.getMonthlyBudget().toLong().toString()) }
    var cacheSize by remember { mutableStateOf(formatCacheSize(getDirSize(context.cacheDir))) }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(stringResource(R.string.settings_monthly_budget)) },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.settings_budget_amount)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.setMonthlyBudget(budgetInput.toDoubleOrNull() ?: 0.0)
                    showBudgetDialog = false
                    Toast.makeText(context, R.string.settings_budget_saved, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.settings_currency)) },
            text = {
                Column {
                    listOf("USD", "AUD", "JPY", "VND").forEach { code ->
                        TextButton(onClick = {
                            currency = code
                            prefs.setCurrency(code)
                            showCurrencyDialog = false
                        }) { Text(code) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    SettingsSectionHeader(stringResource(R.string.settings_data_storage))
    SettingsNavRow(stringResource(R.string.settings_currency), currency) { showCurrencyDialog = true }
    SettingsNavRow(stringResource(R.string.settings_monthly_budget), budgetInput.ifBlank { "0" }) {
        budgetInput = prefs.getMonthlyBudget().toLong().toString()
        showBudgetDialog = true
    }
    SettingsNavRow(stringResource(R.string.settings_clear_cache), cacheSize) {
        if (deleteCacheDir(context.cacheDir)) {
            cacheSize = formatCacheSize(getDirSize(context.cacheDir))
            Toast.makeText(context, R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show()
        }
    }
}
