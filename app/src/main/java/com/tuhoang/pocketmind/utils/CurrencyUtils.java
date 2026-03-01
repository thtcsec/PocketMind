package com.tuhoang.pocketmind.utils;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class CurrencyUtils {

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface ExchangeRateCallback {
        void onSuccess(double rate);
        void onError(Exception e);
    }

    public static void fetchExchangeRate(String targetCurrency, ExchangeRateCallback callback) {
        if ("USD".equalsIgnoreCase(targetCurrency)) {
            callback.onSuccess(1.0);
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject rates = jsonResponse.getJSONObject("rates");

                    if (rates.has(targetCurrency)) {
                        double rate = rates.getDouble(targetCurrency);
                        mainHandler.post(() -> callback.onSuccess(rate));
                    } else {
                        throw new Exception("Currency not supported");
                    }
                } else {
                    throw new Exception("Failed to fetch rates. HTTP Code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static String formatPrice(double priceInUsd, double exchangeRate, String currencyCode) {
        double convertedPrice = priceInUsd * exchangeRate;
        
        switch (currencyCode.toUpperCase()) {
            case "VND":
                // Round up to nearest thousand for VND (e.g. 119,760 -> 120,000)
                long priceVnd = Math.round(convertedPrice / 1000.0) * 1000;
                return new java.text.DecimalFormat("#,###").format(priceVnd) + " đ / Tháng";
            case "AUD":
                return "$" + new java.text.DecimalFormat("#,##0.00").format(convertedPrice) + " AUD / Month";
            case "JPY":
                return "¥" + new java.text.DecimalFormat("#,###").format(Math.round(convertedPrice)) + " / Month";
            case "USD":
            default:
                return "$" + new java.text.DecimalFormat("#,##0.00").format(convertedPrice) + " / Month";
        }
    }
}
