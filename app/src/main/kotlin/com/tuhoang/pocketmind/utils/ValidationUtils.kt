package com.tuhoang.pocketmind.utils

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isValidPassword(password: String): Boolean = password.length >= 6

    fun parseAmount(raw: String): Double? =
        raw.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }
}
