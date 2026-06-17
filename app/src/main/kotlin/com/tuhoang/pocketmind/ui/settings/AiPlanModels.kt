package com.tuhoang.pocketmind.ui.settings

import androidx.compose.ui.graphics.Color

data class AiPlanUiModel(
    val id: String,
    val name: String,
    val priceLabel: String,
    val amountVnd: Long,
    val description: String,
    val isPaid: Boolean,
    val accentColor: Color
)

data class AiPaymentDialogState(
    val planId: String,
    val code: String,
    val amountVnd: Long
)
