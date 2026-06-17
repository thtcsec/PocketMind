package com.tuhoang.pocketmind.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileEditViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val app = getApplication<Application>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun currentUser() = auth.currentUser

    fun uploadAvatar(uri: Uri, onComplete: () -> Unit) {
        val user = auth.currentUser ?: return
        _isLoading.value = true
        val ref = storage.reference.child("avatars").child(user.uid).child("avatar.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUrl)
                        .build()
                    user.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            db.collection("users").document(user.uid)
                                .update("avatarUrl", downloadUrl.toString())
                                .addOnSuccessListener {
                                    user.reload().addOnCompleteListener {
                                        _isLoading.value = false
                                        _message.value = app.getString(R.string.profile_avatar_updated)
                                        onComplete()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    finishAvatarError(e)
                                }
                        }
                        .addOnFailureListener { e -> finishAvatarError(e) }
                }
            }
            .addOnFailureListener { e -> finishAvatarError(e) }
    }

    fun saveProfile(name: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        if (name.isBlank()) {
            _error.value = app.getString(R.string.profile_name_required)
            return
        }
        _isLoading.value = true
        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    db.collection("users").document(user.uid).update("name", name)
                        .addOnSuccessListener {
                            user.reload().addOnCompleteListener {
                                _isLoading.value = false
                                _message.value = app.getString(R.string.profile_save_success)
                                onSuccess()
                            }
                        }
                        .addOnFailureListener { e ->
                            _isLoading.value = false
                            _error.value = app.getString(R.string.profile_save_error_db)
                            AppLogger.e("ProfileEditViewModel", "Firestore update failed", e)
                        }
                } else {
                    _isLoading.value = false
                    _error.value = app.getString(R.string.profile_save_error_auth)
                }
            }
    }

    fun consumeMessage() { _message.value = null }
    fun consumeError() { _error.value = null }

    private fun finishAvatarError(e: Exception) {
        _isLoading.value = false
        _error.value = app.getString(R.string.profile_avatar_failed, e.message ?: "")
        AppLogger.e("ProfileEditViewModel", "Avatar upload failed", e)
    }
}
