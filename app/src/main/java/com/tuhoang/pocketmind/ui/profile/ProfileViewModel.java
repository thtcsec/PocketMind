package com.tuhoang.pocketmind.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tuhoang.pocketmind.utils.AppLogger;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> userRole = new MutableLiveData<>();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<FirebaseUser> getCurrentUser() {
        return currentUser;
    }

    public LiveData<String> getUserRole() {
        return userRole;
    }

    public void fetchUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        currentUser.setValue(user);

        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        userRole.setValue(role);
                    } else {
                        userRole.setValue("user");
                    }
                })
                .addOnFailureListener(e -> {
                    AppLogger.e("ProfileViewModel", "Failed to get user role", e);
                    userRole.setValue("user");
                });
        } else {
            userRole.setValue(null);
        }
    }

    public void logout() {
        mAuth.signOut();
        fetchUserData(); // Updates livedata to null
    }
}
