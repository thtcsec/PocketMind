package com.tuhoang.pocketmind.utils

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object HapticUtils {

    fun performClick(context: Context) {
        if (!PrefsManager.getInstance().isHapticEnabled()) return
        val view = View(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
