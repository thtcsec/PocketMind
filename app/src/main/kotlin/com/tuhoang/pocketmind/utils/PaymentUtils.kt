package com.tuhoang.pocketmind.utils

import java.security.SecureRandom

object PaymentUtils {

    private const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val CODE_LENGTH = 6
    private val random = SecureRandom()

    fun generatePaymentCode(): String {
        val sb = StringBuilder("PM-")
        repeat(CODE_LENGTH) {
            sb.append(CHARACTERS[random.nextInt(CHARACTERS.length)])
        }
        return sb.toString()
    }
}
