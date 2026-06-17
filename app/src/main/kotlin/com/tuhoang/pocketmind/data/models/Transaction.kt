package com.tuhoang.pocketmind.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Transaction(
    var id: String? = null,
    var amount: Double = 0.0,
    var type: String? = null,
    var category: String? = null,
    var note: String? = null,
    var receiptUrl: String? = null,
    @ServerTimestamp
    var timestamp: Date? = null
)
