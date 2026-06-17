package com.tuhoang.pocketmind.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.SepayUtils

@Composable
fun AiPaymentDialog(
    state: AiPaymentDialogState,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.payment_title)) },
        text = {
            Column {
                Text(stringResource(R.string.payment_desc))
                Text(stringResource(R.string.payment_bank_name))
                Text(stringResource(R.string.payment_bank_account))
                Text(stringResource(R.string.payment_bank_owner))
                Text(
                    stringResource(R.string.payment_code_label),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(state.code, style = MaterialTheme.typography.titleMedium)
                val qrUrl = SepayUtils.buildVietQrUrl(
                    account = SepayUtils.BankAccount(),
                    amount = state.amountVnd,
                    content = SepayUtils.buildTransferContent(state.code)
                )
                if (qrUrl.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.payment_sepay_qr_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(text = qrUrl, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    Toast.makeText(context, R.string.ai_err_login_required, Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                val transaction = hashMapOf(
                    "userId" to user.uid,
                    "planId" to state.planId,
                    "amount_vnd" to state.amountVnd,
                    "provider" to "manual",
                    "status" to "pending",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                FirebaseFirestore.getInstance().collection("transactions").document(state.code)
                    .set(transaction)
                    .addOnSuccessListener {
                        Toast.makeText(context, R.string.ai_msg_payment_pending, Toast.LENGTH_LONG).show()
                        onSubmitted()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, R.string.ai_err_connection, Toast.LENGTH_SHORT).show()
                    }
            }) { Text(stringResource(R.string.action_transferred)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun AiPlansSection(
    plans: List<AiPlanUiModel>,
    isLoading: Boolean,
    loadingLabel: String,
    onPlanSelected: (AiPlanUiModel) -> Unit
) {
    if (isLoading) {
        Text(loadingLabel)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        plans.forEach { plan ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                onClick = {
                    if (plan.isPaid) onPlanSelected(plan)
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(plan.name.uppercase(), color = plan.accentColor, fontWeight = FontWeight.Bold)
                    if (plan.description.isNotEmpty()) {
                        Text(plan.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text(plan.priceLabel, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
