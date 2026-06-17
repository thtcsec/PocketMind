package com.tuhoang.pocketmind.ui.auth

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.LoadingOverlay
import com.tuhoang.pocketmind.utils.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onBack: () -> Unit, onNavigateRegister: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                isLoading = true
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        checkAndCreateFirestoreUser(auth, context, onSuccess = {
                            isLoading = false
                            Toast.makeText(context, R.string.auth_google_login_success, Toast.LENGTH_SHORT).show()
                            onBack()
                        }, onError = { msg ->
                            isLoading = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        })
                    } else {
                        isLoading = false
                        Toast.makeText(context, R.string.auth_google_login_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                AppLogger.e("LoginScreen", "Google sign in failed", e)
                Toast.makeText(context, R.string.auth_google_login_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.auth_google_login_cancelled, Toast.LENGTH_SHORT).show()
        }
    }

    if (isLoading) {
        LoadingOverlay(stringResource(R.string.action_logging_in))
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_login)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("PocketMind", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, R.string.auth_err_empty_fields, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Toast.makeText(context, R.string.auth_login_success, Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_login_failed, task.exception?.message ?: ""),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(stringResource(R.string.action_login)) }

            OutlinedButton(
                onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text(stringResource(R.string.action_login_google)) }

            TextButton(onClick = onNavigateRegister) {
                Text(stringResource(R.string.action_register))
            }
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
                    "name" to (user.displayName ?: "User"),
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
