package com.tuhoang.pocketmind.utils

import android.content.Context
import com.tuhoang.pocketmind.R
import java.text.DecimalFormat
import kotlin.math.roundToLong

object CurrencyUtils {

    private const val API_URL = "https://open.er-api.com/v6/latest/USD"
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    interface ExchangeRateCallback {
        fun onSuccess(rate: Double)
        fun onError(e: Exception)
    }

    fun fetchExchangeRate(targetCurrency: String, callback: ExchangeRateCallback) {
        if (targetCurrency.equals("USD", ignoreCase = true)) {
            callback.onSuccess(1.0)
            return
        }

        executor.execute {
            try {
                val conn = (java.net.URL(API_URL).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val rates = org.json.JSONObject(response).getJSONObject("rates")
                    if (rates.has(targetCurrency)) {
                        val rate = rates.getDouble(targetCurrency)
                        mainHandler.post { callback.onSuccess(rate) }
                    } else {
                        throw Exception("Currency not supported")
                    }
                } else {
                    throw Exception("Failed to fetch rates. HTTP Code: ${conn.responseCode}")
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError(e) }
            }
        }
    }

    fun toVndAmount(priceInUsd: Double, exchangeRate: Double, currencyCode: String): Long {
        val vnd = if (currencyCode.equals("VND", ignoreCase = true)) {
            priceInUsd * exchangeRate
        } else {
            priceInUsd * 24000
        }
        return ((vnd / 1000.0).roundToLong() * 1000)
    }

    fun formatPrice(context: Context, priceInUsd: Double, exchangeRate: Double, currencyCode: String): String {
        val convertedPrice = priceInUsd * exchangeRate
        return when (currencyCode.uppercase()) {
            "VND" -> {
                val priceVnd = (convertedPrice / 1000.0).roundToLong() * 1000
                context.getString(R.string.price_per_month_vnd, DecimalFormat("#,###").format(priceVnd))
            }
            "AUD" -> context.getString(
                R.string.price_per_month_aud,
                DecimalFormat("#,##0.00").format(convertedPrice)
            )
            "JPY" -> context.getString(
                R.string.price_per_month_jpy,
                DecimalFormat("#,###").format(convertedPrice.roundToLong())
            )
            else -> context.getString(
                R.string.price_per_month_usd,
                DecimalFormat("#,##0.00").format(convertedPrice)
            )
        }
    }
}
