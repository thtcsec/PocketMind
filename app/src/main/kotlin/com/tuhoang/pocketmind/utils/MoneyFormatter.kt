package com.tuhoang.pocketmind.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToLong

class MoneyFormatter(private val currencyCode: String) {

    fun format(amount: Double, signed: Boolean = false): String {
        val isIncome = amount >= 0
        val value = abs(amount)
        val body = when (currencyCode.uppercase()) {
            "VND" -> "${vndFormat.format(value.roundToLong())} ₫"
            "JPY" -> "¥${wholeFormat.format(value.roundToLong())}"
            "AUD" -> "A$${decimalFormat.format(value)}"
            else -> "$${decimalFormat.format(value)}"
        }
        return when {
            signed && isIncome -> "+$body"
            signed && !isIncome -> "-$body"
            else -> body
        }
    }

    fun formatUnsigned(amount: Double): String = format(amount, signed = false)

    companion object {
        private val decimalFormat = DecimalFormat("#,##0.00")
        private val vndFormat = DecimalFormat("#,###")
        private val wholeFormat = DecimalFormat("#,###")

        fun symbol(currencyCode: String): String = when (currencyCode.uppercase()) {
            "VND" -> "₫"
            "JPY" -> "¥"
            "AUD" -> "A$"
            else -> "$"
        }
    }
}

@Composable
fun rememberMoneyFormatter(): MoneyFormatter {
    val currency by PrefsManager.getInstance().currencyFlow().collectAsState()
    return remember(currency) { MoneyFormatter(currency) }
}
