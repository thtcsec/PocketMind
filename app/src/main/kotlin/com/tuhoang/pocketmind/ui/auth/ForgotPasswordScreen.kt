package com.tuhoang.pocketmind.ui.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.AuthEmailField
import com.tuhoang.pocketmind.ui.components.AuthPrimaryButton
import com.tuhoang.pocketmind.ui.components.AuthScreenLayout
import com.tuhoang.pocketmind.ui.components.LoadingOverlay
import com.tuhoang.pocketmind.ui.components.rememberShowSnackbar
import com.tuhoang.pocketmind.utils.ValidationUtils

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val showSnackbar = rememberShowSnackbar()

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun submit() {
        if (!ValidationUtils.isValidEmail(email)) {
            showSnackbar(context.getString(R.string.auth_err_invalid_email))
            return
        }
        isLoading = true
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    showSnackbar(context.getString(R.string.auth_reset_email_sent))
                    onBack()
                } else {
                    showSnackbar(
                        context.getString(
                            R.string.auth_reset_failed,
                            task.exception?.message ?: ""
                        )
                    )
                }
            }
    }

    if (isLoading) {
        LoadingOverlay(stringResource(R.string.auth_sending_reset))
        return
    }

    AuthScreenLayout(
        title = stringResource(R.string.auth_forgot_password),
        subtitle = stringResource(R.string.auth_forgot_password_desc),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    ) {
        AuthEmailField(value = email, onValueChange = { email = it })
        AuthPrimaryButton(
            text = stringResource(R.string.auth_send_reset_link),
            onClick = { submit() },
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}
