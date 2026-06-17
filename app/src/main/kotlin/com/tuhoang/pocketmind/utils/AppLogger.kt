package com.tuhoang.pocketmind.utils

import android.util.Log

object AppLogger {
    private const val DEFAULT_TAG = "PocketMindLog"

    fun d(tag: String, message: String) = Log.d(tag, message)
    fun d(message: String) = Log.d(DEFAULT_TAG, message)
    fun e(tag: String, message: String, t: Throwable? = null) = Log.e(tag, message, t)
}
