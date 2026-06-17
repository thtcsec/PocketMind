package com.tuhoang.pocketmind.ui.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.AuthEmailField
import com.tuhoang.pocketmind.ui.components.AuthNameField
import com.tuhoang.pocketmind.ui.components.AuthPasswordField
import com.tuhoang.pocketmind.ui.components.AuthPrimaryButton
import com.tuhoang.pocketmind.ui.components.AuthScreenLayout
import com.tuhoang.pocketmind.ui.components.LoadingOverlay
import com.tuhoang.pocketmind.ui.components.rememberShowSnackbar
import com.tuhoang.pocketmind.utils.AppLogger
import com.tuhoang.pocketmind.utils.HapticUtils
import com.tuhoang.pocketmind.utils.ValidationUtils

@Composable
fun RegisterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val showSnackbar = rememberShowSnackbar()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun submitRegister() {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                showSnackbar(context.getString(R.string.auth_err_empty_fields))
            !ValidationUtils.isValidEmail(email) ->
                showSnackbar(context.getString(R.string.auth_err_invalid_email))
            password != confirmPassword ->
                showSnackbar(context.getString(R.string.auth_err_password_mismatch))
            !ValidationUtils.isValidPassword(password) ->
                showSnackbar(context.getString(R.string.auth_err_password_length))
            else -> {
                HapticUtils.performClick(context)
                isLoading = true
                auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                            user.updateProfile(profileUpdates).addOnCompleteListener {
                                val userData = hashMapOf(
                                    "uid" to user.uid,
                                    "name" to name,
                                    "email" to email.trim(),
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "ai_chat_limit" to 5,
                                    "role" to "user"
                                )
                                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        showSnackbar(context.getString(R.string.auth_register_success))
                                        onBack()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        AppLogger.e("RegisterScreen", "Failed to create user doc", e)
                                        showSnackbar(context.getString(R.string.auth_err_init_failed, e.message ?: ""))
                                    }
                            }
                        } else {
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                        showSnackbar(context.getString(R.string.auth_register_failed, task.exception?.message ?: ""))
                    }
                }
            }
        }
    }

    if (isLoading) {
        LoadingOverlay(stringResource(R.string.action_registering))
        return
    }

    AuthScreenLayout(
        title = stringResource(R.string.auth_create_account),
        subtitle = stringResource(R.string.auth_register_subtitle),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    ) {
        AuthNameField(value = name, onValueChange = { name = it })
        AuthEmailField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.padding(top = 12.dp)
        )
        AuthPasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.label_password),
            modifier = Modifier.padding(top = 12.dp)
        )
        AuthPasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = stringResource(R.string.label_confirm_password),
            modifier = Modifier.padding(top = 12.dp),
            onImeAction = { submitRegister() }
        )

        AuthPrimaryButton(
            text = stringResource(R.string.action_register),
            onClick = { submitRegister() },
            modifier = Modifier.padding(top = 20.dp)
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_have_account),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
