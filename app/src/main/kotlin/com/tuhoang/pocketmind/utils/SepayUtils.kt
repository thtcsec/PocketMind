package com.tuhoang.pocketmind.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Scaffolding for Sepay / VietQR bank transfer integration.
 * Real webhook verification happens server-side on the Worker.
 */
object SepayUtils {

    data class BankAccount(
        val bankCode: String = "MB",
        val accountNumber: String = "0123456789",
        val accountName: String = "TRINH HOANG TU"
    )

    fun buildTransferContent(paymentCode: String): String = paymentCode

    fun buildVietQrUrl(
        account: BankAccount,
        amount: Long,
        content: String
    ): String {
        val encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString())
        return "https://qr.sepay.vn/img?" +
            "bank=${account.bankCode}" +
            "&acc=${account.accountNumber}" +
            "&template=compact" +
            "&amount=$amount" +
            "&des=$encodedContent"
    }

    fun buildSepayCheckoutUrl(
        baseUrl: String,
        paymentCode: String,
        amount: Long,
        planName: String
    ): String {
        if (baseUrl.isBlank()) return ""
        val encodedPlan = URLEncoder.encode(planName, StandardCharsets.UTF_8.toString())
        return baseUrl.trimEnd('/') +
            "/checkout?code=$paymentCode&amount=$amount&plan=$encodedPlan"
    }
}
