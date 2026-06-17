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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.AuthEmailField
import com.tuhoang.pocketmind.ui.components.AuthGoogleButton
import com.tuhoang.pocketmind.ui.components.AuthOrDivider
import com.tuhoang.pocketmind.ui.components.AuthPasswordField
import com.tuhoang.pocketmind.ui.components.AuthPrimaryButton
import com.tuhoang.pocketmind.ui.components.AuthScreenLayout
import com.tuhoang.pocketmind.ui.components.LoadingOverlay
import com.tuhoang.pocketmind.ui.components.rememberShowSnackbar
import com.tuhoang.pocketmind.utils.GoogleSignInHelper
import com.tuhoang.pocketmind.utils.HapticUtils
import com.tuhoang.pocketmind.utils.ValidationUtils
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val showSnackbar = rememberShowSnackbar()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun finishGoogleLogin(idToken: String) {
        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                checkAndCreateFirestoreUser(auth, context, onSuccess = {
                    isLoading = false
                    showSnackbar(context.getString(R.string.auth_google_login_success))
                    onBack()
                }, onError = { msg ->
                    isLoading = false
                    showSnackbar(msg)
                })
            } else {
                isLoading = false
                showSnackbar(context.getString(R.string.auth_google_login_failed))
            }
        }
    }

    fun submitLogin() {
        when {
            email.isBlank() || password.isBlank() ->
                showSnackbar(context.getString(R.string.auth_err_empty_fields))
            !ValidationUtils.isValidEmail(email) ->
                showSnackbar(context.getString(R.string.auth_err_invalid_email))
            else -> {
                HapticUtils.performClick(context)
                isLoading = true
                auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        showSnackbar(context.getString(R.string.auth_login_success))
                        onBack()
                    } else {
                        showSnackbar(
                            context.getString(R.string.auth_login_failed, task.exception?.message ?: "")
                        )
                    }
                }
            }
        }
    }

    if (isLoading) {
        LoadingOverlay(stringResource(R.string.action_logging_in))
        return
    }

    AuthScreenLayout(
        title = stringResource(R.string.action_login),
        subtitle = stringResource(R.string.auth_welcome_back),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    ) {
        AuthEmailField(value = email, onValueChange = { email = it })
        AuthPasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.label_password),
            modifier = Modifier.padding(top = 12.dp),
            onImeAction = { submitLogin() }
        )

        TextButton(
            onClick = onNavigateForgotPassword,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.auth_forgot_password),
                style = MaterialTheme.typography.bodySmall
            )
        }

        AuthPrimaryButton(
            text = stringResource(R.string.action_login),
            onClick = { submitLogin() },
            modifier = Modifier.padding(top = 8.dp)
        )

        AuthOrDivider(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))

        AuthGoogleButton(
            onClick = {
                scope.launch {
                    when (val result = GoogleSignInHelper.signIn(
                        context,
                        context.getString(R.string.default_web_client_id)
                    )) {
                        is GoogleSignInHelper.Result.Success -> finishGoogleLogin(result.idToken)
                        GoogleSignInHelper.Result.Cancelled ->
                            showSnackbar(context.getString(R.string.auth_google_login_cancelled))
                        is GoogleSignInHelper.Result.Error ->
                            showSnackbar(context.getString(R.string.auth_google_login_failed))
                    }
                }
            }
        )

        TextButton(
            onClick = onNavigateRegister,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_no_account),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun checkAndCreateFirestoreUser(
    auth: FirebaseAuth,
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val user = auth.currentUser ?: return onSuccess()
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(user.uid).get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val document = task.result
            if (document?.exists() != true) {
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "name" to (user.displayName ?: context.getString(R.string.auth_default_user_name)),
                    "email" to (user.email ?: ""),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "ai_chat_limit" to 5,
                    "role" to "user"
                )
                db.collection("users").document(user.uid).set(userData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(context.getString(R.string.auth_err_init_failed, e.message ?: ""))
                    }
            } else {
                onSuccess()
            }
        } else {
            onSuccess()
        }
    }
}
