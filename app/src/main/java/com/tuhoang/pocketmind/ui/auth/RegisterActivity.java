package com.tuhoang.pocketmind.ui.auth;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.Gravity;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.ActivityRegisterBinding;
import com.tuhoang.pocketmind.utils.AppLogger;
import com.tuhoang.pocketmind.utils.LoadingDialog;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        loadingDialog = new LoadingDialog(this);

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.auth_err_empty_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, getString(R.string.auth_err_password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, getString(R.string.auth_err_password_length), Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.show(getString(R.string.action_registering));

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        AppLogger.d("createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            AppLogger.d("User profile updated.");
                                            
                                            // Save to Firestore
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("uid", user.getUid());
                                            userData.put("name", name);
                                            userData.put("email", email);
                                            userData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                            userData.put("ai_chat_limit", 5); // Default free limit
                                            
                                            FirebaseFirestore.getInstance().collection("users")
                                                .document(user.getUid())
                                                .set(userData)
                                                .addOnSuccessListener(aVoid -> {
                                                    loadingDialog.dismiss();
                                                    AppLogger.d("User doc created in Firestore");
                                                    Toast.makeText(RegisterActivity.this, getString(R.string.auth_register_success), Toast.LENGTH_SHORT).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    loadingDialog.dismiss();
                                                    AppLogger.e("RegisterActivity", "Failed to create user doc", e);
                                                    Toast.makeText(RegisterActivity.this, getString(R.string.auth_err_init_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                                                });
                                        } else {
                                            loadingDialog.dismiss();
                                        }
                                    });
                        } else {
                            loadingDialog.dismiss();
                        }
                    } else {
                        loadingDialog.dismiss();
                        AppLogger.e("RegisterActivity", "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, getString(R.string.auth_register_failed, task.getException().getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
        });

        binding.tvLoginLink.setOnClickListener(v -> {
            finish();
        });
    }
}
