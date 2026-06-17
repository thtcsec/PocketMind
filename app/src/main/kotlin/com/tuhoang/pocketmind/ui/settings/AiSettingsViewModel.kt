package com.tuhoang.pocketmind.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.CurrencyUtils
import com.tuhoang.pocketmind.utils.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiSettingsViewModel : ViewModel() {

    private val _plans = MutableStateFlow<List<AiPlanUiModel>>(emptyList())
    val plans: StateFlow<List<AiPlanUiModel>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPlans(context: Context) {
        _isLoading.value = true
        _error.value = null
        val currency = PrefsManager.getInstance().getCurrency("USD")

        FirebaseFirestore.getInstance().collection("ai_plans").get()
            .addOnSuccessListener { snapshots ->
                CurrencyUtils.fetchExchangeRate(currency, object : CurrencyUtils.ExchangeRateCallback {
                    override fun onSuccess(rate: Double) {
                        viewModelScope.launch {
                            _plans.value = snapshots.documents.mapNotNull { doc ->
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
                                    buildAiFeatureDescription(context, list)
                                } ?: ""
                                AiPlanUiModel(
                                    id = id,
                                    name = name,
                                    priceLabel = if (amount == 0.0) context.getString(R.string.ai_status_active)
                                    else CurrencyUtils.formatPrice(context, amount, rate, currency),
                                    amountVnd = CurrencyUtils.toVndAmount(amount, rate, currency),
                                    description = features,
                                    isPaid = amount > 0.0,
                                    accentColor = when (id) {
                                        "PRO_PLAN" -> androidx.compose.ui.graphics.Color(0xFF3F51B5)
                                        "MAX_PLAN" -> androidx.compose.ui.graphics.Color(0xFFE91E63)
                                        else -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    }
                                )
                            }
                            _isLoading.value = false
                        }
                    }

                    override fun onError(e: Exception) {
                        _error.value = context.getString(R.string.ai_err_exchange_rate)
                        _isLoading.value = false
                    }
                })
            }
            .addOnFailureListener {
                _error.value = context.getString(R.string.ai_err_connection)
                _isLoading.value = false
            }
    }

    fun consumeError() { _error.value = null }
}

fun buildAiFeatureDescription(context: Context, features: List<Map<String, Any>>): String {
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
