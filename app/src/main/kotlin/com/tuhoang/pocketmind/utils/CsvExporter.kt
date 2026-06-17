package com.tuhoang.pocketmind.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.tuhoang.pocketmind.data.models.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun export(context: Context, transactions: List<Transaction>): Uri? {
        if (transactions.isEmpty()) return null
        val file = File(context.cacheDir, "pocketmind_export_${System.currentTimeMillis()}.csv")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        file.bufferedWriter().use { writer ->
            writer.appendLine("Date,Type,Category,Amount,Note")
            for (t in transactions) {
                val date = t.timestamp?.let { sdf.format(it) } ?: ""
                val note = (t.note ?: "").replace("\"", "\"\"")
                writer.appendLine("$date,${t.type},${t.category},${t.amount},\"$note\"")
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
