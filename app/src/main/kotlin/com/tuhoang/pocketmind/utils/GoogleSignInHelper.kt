package com.tuhoang.pocketmind.utils

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleSignInHelper {

    sealed class Result {
        data class Success(val idToken: String) : Result()
        data object Cancelled : Result()
        data class Error(val message: String?) : Result()
    }

    suspend fun signIn(context: Context, serverClientId: String): Result {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.Success(credential.idToken)
        } catch (e: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: Exception) {
            AppLogger.e("GoogleSignInHelper", "Google sign in failed", e)
            Result.Error(e.message)
        }
    }
}
