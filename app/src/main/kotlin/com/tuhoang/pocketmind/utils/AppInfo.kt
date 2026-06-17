package com.tuhoang.pocketmind.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tuhoang.pocketmind.R

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Long
)

object AppInfo {

    const val GITHUB_URL = "https://github.com/thtcsec/PocketMind"
    const val LINKEDIN_URL = "https://www.linkedin.com/in/trinhhoangtu"

    fun versionInfo(context: Context): AppVersionInfo {
        return try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }
            AppVersionInfo(
                versionName = pkg.versionName ?: context.getString(R.string.settings_unknown_version),
                versionCode = code
            )
        } catch (_: PackageManager.NameNotFoundException) {
            AppVersionInfo(
                versionName = context.getString(R.string.settings_unknown_version),
                versionCode = 0L
            )
        }
    }
}
