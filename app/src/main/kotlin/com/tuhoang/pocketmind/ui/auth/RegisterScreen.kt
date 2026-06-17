package com.tuhoang.pocketmind.ui.auth

import android.widget.Toast
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.LoadingOverlay
import com.tuhoang.pocketmind.utils.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    if (isLoading) {
        LoadingOverlay(stringResource(R.string.action_registering))
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_register)) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

            Button(
                onClick = {
                    when {
                        name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                            Toast.makeText(context, R.string.auth_err_empty_fields, Toast.LENGTH_SHORT).show()
                        password != confirmPassword ->
                            Toast.makeText(context, R.string.auth_err_password_mismatch, Toast.LENGTH_SHORT).show()
                        password.length < 6 ->
                            Toast.makeText(context, R.string.auth_err_password_length, Toast.LENGTH_SHORT).show()
                        else -> {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                                        user.updateProfile(profileUpdates).addOnCompleteListener {
                                            val userData = hashMapOf(
                                                "uid" to user.uid,
                                                "name" to name,
                                                "email" to email,
                                                "createdAt" to FieldValue.serverTimestamp(),
                                                "ai_chat_limit" to 5,
                                                "role" to "user"
                                            )
                                            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    Toast.makeText(context, R.string.auth_register_success, Toast.LENGTH_SHORT).show()
                                                    onBack()
                                                }
                                                .addOnFailureListener { e ->
                                                    isLoading = false
                                                    AppLogger.e("RegisterScreen", "Failed to create user doc", e)
                                                    Toast.makeText(context, context.getString(R.string.auth_err_init_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } else {
                                        isLoading = false
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, context.getString(R.string.auth_register_failed, task.exception?.message ?: ""), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(stringResource(R.string.action_register)) }

            TextButton(onClick = onBack) { Text(stringResource(R.string.action_login)) }
        }
    }
}
