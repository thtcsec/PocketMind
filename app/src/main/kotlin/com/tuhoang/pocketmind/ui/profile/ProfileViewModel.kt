package com.tuhoang.pocketmind.ui.profile

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    fun fetchUserData() {
        val user = auth.currentUser
        _currentUser.value = user

        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    _userRole.value = if (document.exists()) {
                        document.getString("role") ?: "user"
                    } else {
                        "user"
                    }
                }
                .addOnFailureListener { e ->
                    AppLogger.e("ProfileViewModel", "Failed to get user role", e)
                    _userRole.value = "user"
                }
        } else {
            _userRole.value = null
        }
    }

    fun logout() {
        auth.signOut()
        fetchUserData()
    }
}
