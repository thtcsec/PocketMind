package com.tuhoang.pocketmind.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object TransactionRepository {

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    fun saveTransaction(
        amount: Double,
        type: String,
        category: String,
        note: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError(IllegalStateException("Not logged in"))
            return
        }
        val data = hashMapOf(
            "amount" to amount,
            "type" to type,
            "category" to category.ifBlank { "Other" },
            "note" to note,
            "timestamp" to Date()
        )
        db.collection("users").document(uid).collection("expenses")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun saveFromAiData(
        data: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
        val type = (data["type"] as? String)?.ifBlank { "expense" } ?: "expense"
        val category = (data["category"] as? String)?.ifBlank { "Other" } ?: "Other"
        val note = (data["note"] as? String) ?: ""
        saveTransaction(amount, type, category, note, onSuccess, onError)
    }
}
