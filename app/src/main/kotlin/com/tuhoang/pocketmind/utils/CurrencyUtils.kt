package com.tuhoang.pocketmind.utils

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.concurrent.Executors
import kotlin.math.roundToLong

object CurrencyUtils {

    private const val API_URL = "https://open.er-api.com/v6/latest/USD"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

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
                val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val rates = JSONObject(response).getJSONObject("rates")
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

    fun formatPrice(priceInUsd: Double, exchangeRate: Double, currencyCode: String): String {
        val convertedPrice = priceInUsd * exchangeRate
        return when (currencyCode.uppercase()) {
            "VND" -> {
                val priceVnd = (convertedPrice / 1000.0).roundToLong() * 1000
                "${DecimalFormat("#,###").format(priceVnd)} đ / Tháng"
            }
            "AUD" -> "$${DecimalFormat("#,##0.00").format(convertedPrice)} AUD / Month"
            "JPY" -> "¥${DecimalFormat("#,###").format(convertedPrice.roundToLong())} / Month"
            else -> "$${DecimalFormat("#,##0.00").format(convertedPrice)} / Month"
        }
    }
}
