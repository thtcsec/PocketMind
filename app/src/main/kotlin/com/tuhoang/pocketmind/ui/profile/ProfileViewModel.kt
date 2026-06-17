package com.tuhoang.pocketmind.ui.profile

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileStats(
    val transactionCount: Int = 0,
    val planName: String = "Free Plan"
)

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
        loadRole(firebaseAuth.currentUser)
        loadStats(firebaseAuth.currentUser)
    }

    init {
        auth.addAuthStateListener(authListener)
        fetchUserData()
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    fun fetchUserData() {
        val user = auth.currentUser
        _currentUser.value = user
        user?.reload()?.addOnCompleteListener {
            _currentUser.value = auth.currentUser
        }
        loadRole(user)
        loadStats(user)
    }

    private fun loadRole(user: FirebaseUser?) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    _userRole.value = if (document.exists()) {
                        document.getString("role") ?: "user"
                    } else {
                        "user"
                    }
                    val plan = document.getString("current_plan") ?: "FREE_PLAN"
                    _stats.value = _stats.value.copy(
                        planName = when (plan) {
                            "PRO_PLAN" -> "Pro Plan"
                            else -> "Free Plan"
                        }
                    )
                }
                .addOnFailureListener { e ->
                    AppLogger.e("ProfileViewModel", "Failed to get user role", e)
                    _userRole.value = "user"
                }
        } else {
            _userRole.value = null
            _stats.value = ProfileStats()
        }
    }

    private fun loadStats(user: FirebaseUser?) {
        if (user == null) return
        db.collection("users").document(user.uid).collection("expenses")
            .get()
            .addOnSuccessListener { snapshots ->
                _stats.value = _stats.value.copy(transactionCount = snapshots.size())
            }
            .addOnFailureListener { e ->
                AppLogger.e("ProfileViewModel", "Failed to load stats", e)
            }
    }

    fun logout() {
        auth.signOut()
    }
}
