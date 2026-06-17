package com.tuhoang.pocketmind.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.CurrencyUtils
import com.tuhoang.pocketmind.utils.PaymentUtils
import com.tuhoang.pocketmind.utils.SepayUtils
import com.tuhoang.pocketmind.utils.PrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val providers = listOf("OpenAI (ChatGPT)", "Anthropic (Claude)", "Google (Gemini)", "DeepSeek")

    var modeIndex by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf(PrefsManager.getInstance().getOpenAiApiKey()) }
    var selectedProvider by remember { mutableStateOf(providers[0]) }
    var plans by remember { mutableStateOf<List<PlanUiModel>>(emptyList()) }
    var isLoadingPlans by remember { mutableStateOf(true) }
    var paymentDialog by remember { mutableStateOf<PaymentDialogState?>(null) }

    LaunchedEffect(Unit) {
        val currency = PrefsManager.getInstance().getCurrency("USD")
        FirebaseFirestore.getInstance().collection("ai_plans").get()
            .addOnSuccessListener { snapshots ->
                CurrencyUtils.fetchExchangeRate(currency, object : CurrencyUtils.ExchangeRateCallback {
                    override fun onSuccess(rate: Double) {
                        plans = snapshots.documents.mapNotNull { doc ->
                            val isActive = doc.getBoolean("is_active") ?: false
                            if (!isActive) return@mapNotNull null
                            val id = doc.id
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val priceMap = doc.get("price") as? Map<*, *>
                            val amount = when (val a = priceMap?.get("amount")) {
                                is Double -> a
                                is Long -> a.toDouble()
                                else -> 0.0
                            }
                            val features = (doc.get("features") as? List<Map<String, Any>>)?.let { list ->
                                buildFeatureDescription(context, list)
                            } ?: ""
                            PlanUiModel(
                                id = id,
                                name = name,
                                priceLabel = if (amount == 0.0) context.getString(R.string.ai_status_active)
                                else CurrencyUtils.formatPrice(context, amount, rate, currency),
                                amountVnd = CurrencyUtils.toVndAmount(amount, rate, currency),
                                description = features,
                                isPaid = amount > 0.0,
                                accentColor = when (id) {
                                    "PRO_PLAN" -> Color(0xFF3F51B5)
                                    "MAX_PLAN" -> Color(0xFFE91E63)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                        }
                        isLoadingPlans = false
                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(context, R.string.ai_err_exchange_rate, Toast.LENGTH_SHORT).show()
                        isLoadingPlans = false
                    }
                })
            }
            .addOnFailureListener {
                isLoadingPlans = false
                Toast.makeText(context, R.string.ai_err_connection, Toast.LENGTH_SHORT).show()
            }
    }

    paymentDialog?.let { state ->
        AlertDialog(
            onDismissRequest = { paymentDialog = null },
            title = { Text(stringResource(R.string.payment_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.payment_desc))
                    Text(stringResource(R.string.payment_bank_name))
                    Text(stringResource(R.string.payment_bank_account))
                    Text(stringResource(R.string.payment_bank_owner))
                    Text(stringResource(R.string.payment_code_label), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(state.code, style = MaterialTheme.typography.titleMedium)
                    val qrUrl = SepayUtils.buildVietQrUrl(
                        account = SepayUtils.BankAccount(),
                        amount = state.amountVnd,
                        content = SepayUtils.buildTransferContent(state.code)
                    )
                    if (qrUrl.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.payment_sepay_qr_hint),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = qrUrl,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        Toast.makeText(context, R.string.ai_err_login_required, Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val transaction = hashMapOf(
                        "userId" to user.uid,
                        "planId" to state.planId,
                        "amount_vnd" to state.amountVnd,
                        "provider" to "manual",
                        "status" to "pending",
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    FirebaseFirestore.getInstance().collection("transactions").document(state.code)
                        .set(transaction)
                        .addOnSuccessListener {
                            Toast.makeText(context, R.string.ai_msg_payment_pending, Toast.LENGTH_LONG).show()
                            paymentDialog = null
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, R.string.ai_err_connection, Toast.LENGTH_SHORT).show()
                        }
                }) { Text(stringResource(R.string.action_transferred)) }
            },
            dismissButton = {
                TextButton(onClick = { paymentDialog = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.ai_config_subtitle), style = MaterialTheme.typography.bodyMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = modeIndex == 0,
                    onClick = { modeIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(stringResource(R.string.ai_mode_plans)) }
                SegmentedButton(
                    selected = modeIndex == 1,
                    onClick = { modeIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(stringResource(R.string.ai_mode_apikey)) }
            }

            if (modeIndex == 0) {
                if (isLoadingPlans) {
                    Text(stringResource(R.string.ai_loading))
                } else {
                    plans.forEach { plan ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(),
                            onClick = {
                                if (plan.isPaid) {
                                    paymentDialog = PaymentDialogState(
                                        planId = plan.id,
                                        code = PaymentUtils.generatePaymentCode(),
                                        amountVnd = plan.amountVnd
                                    )
                                }
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(plan.name.uppercase(), color = plan.accentColor, fontWeight = FontWeight.Bold)
                                if (plan.description.isNotEmpty()) {
                                    Text(plan.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                                }
                                Text(plan.priceLabel, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = selectedProvider,
                    onValueChange = { selectedProvider = it },
                    label = { Text(stringResource(R.string.ai_provider_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.ai_apikey_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (selectedProvider.isBlank()) {
                            Toast.makeText(context, R.string.ai_err_select_provider, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (apiKey.isBlank()) {
                            Toast.makeText(context, R.string.ai_err_enter_key, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        PrefsManager.getInstance().setOpenAiApiKey(apiKey)
                        Toast.makeText(context, context.getString(R.string.ai_msg_key_saved, selectedProvider), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.ai_btn_save_key)) }
                Text(stringResource(R.string.ai_note_key_security), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private data class PlanUiModel(
    val id: String,
    val name: String,
    val priceLabel: String,
    val amountVnd: Long,
    val description: String,
    val isPaid: Boolean,
    val accentColor: Color
)

private data class PaymentDialogState(val planId: String, val code: String, val amountVnd: Long)

private fun buildFeatureDescription(context: android.content.Context, features: List<Map<String, Any>>): String {
    val sb = StringBuilder()
    for (feature in features) {
        val key = feature["key"] as? String ?: continue
        val value = feature["value"]
        when (key) {
            "TEXT_CHAT_LIMIT" -> sb.append(context.getString(R.string.ai_feature_text_limit, value.toString())).append('\n')
            "UNLIM_TEXT" -> if (value == true) sb.append(context.getString(R.string.ai_feature_unlim_text)).append('\n')
            "PRIORITY_QUEUE" -> if (value == true) sb.append(context.getString(R.string.ai_feature_priority)).append('\n')
            "VOICE_ALLOWED" -> if (value == true) sb.append(context.getString(R.string.ai_feature_voice)).append('\n')
            "IMAGE_PARSING" -> if (value == true) sb.append(context.getString(R.string.ai_feature_image)).append('\n')
            "PREMIUM_MODELS" -> if (value is List<*>) {
                sb.append(context.getString(R.string.ai_feature_premium_models, value.joinToString(", "))).append('\n')
            }
            else -> sb.append(context.getString(R.string.ai_feature_generic, key, value.toString())).append('\n')
        }
    }
    return sb.toString().trim()
}
