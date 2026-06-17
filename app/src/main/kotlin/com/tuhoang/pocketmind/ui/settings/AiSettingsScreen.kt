package com.tuhoang.pocketmind.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.PaymentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(onBack: () -> Unit, viewModel: AiSettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val plans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var paymentDialog by remember { mutableStateOf<AiPaymentDialogState?>(null) }

    LaunchedEffect(Unit) { viewModel.loadPlans(context) }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeError()
        }
    }

    paymentDialog?.let { state ->
        AiPaymentDialog(
            state = state,
            onDismiss = { paymentDialog = null },
            onSubmitted = { paymentDialog = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(stringResource(R.string.settings_ai_config)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.ai_config_subtitle), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.ai_note_server_managed),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            AiPlansSection(
                plans = plans,
                isLoading = isLoading,
                loadingLabel = stringResource(R.string.ai_loading),
                onPlanSelected = { plan ->
                    paymentDialog = AiPaymentDialogState(
                        planId = plan.id,
                        code = PaymentUtils.generatePaymentCode(),
                        amountVnd = plan.amountVnd
                    )
                }
            )
        }
    }
}
