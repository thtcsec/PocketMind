package com.tuhoang.pocketmind.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.ActivityLoginBinding;
import com.tuhoang.pocketmind.utils.AppLogger;
import com.tuhoang.pocketmind.utils.LoadingDialog;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private LoadingDialog loadingDialog;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        AppLogger.d("firebaseAuthWithGoogle:" + account.getId());
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        // Google Sign In failed
                        AppLogger.e("LoginActivity", "Google sign in failed", e);
                        Toast.makeText(this, getString(R.string.auth_google_login_failed), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.auth_google_login_cancelled), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        loadingDialog = new LoadingDialog(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.auth_err_empty_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            
            loadingDialog.show(getString(R.string.action_logging_in));

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        AppLogger.d("signInWithEmail:success");
                        Toast.makeText(LoginActivity.this, getString(R.string.auth_login_success), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        AppLogger.e("LoginActivity", "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, getString(R.string.auth_login_failed, task.getException().getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
        });

        binding.btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        binding.tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        AppLogger.d("signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkAndCreateFirestoreUser(user);
                        } else {
                            loadingDialog.dismiss();
                            Toast.makeText(LoginActivity.this, getString(R.string.auth_google_login_success), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        loadingDialog.dismiss();
                        AppLogger.e("LoginActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, getString(R.string.auth_google_login_failed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateFirestoreUser(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (!document.exists()) {
                        AppLogger.d("User doc doesn't exist, creating...");
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", user.getUid());
                        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "User");
                        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
                        userData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        userData.put("ai_chat_limit", 5);

                        db.collection("users").document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                loadingDialog.dismiss();
                                AppLogger.d("User doc created via Google SignIn");
                                Toast.makeText(LoginActivity.this, getString(R.string.auth_google_login_success), Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                loadingDialog.dismiss();
                                AppLogger.e("LoginActivity", "Failed to init user", e);
                                Toast.makeText(LoginActivity.this, getString(R.string.auth_err_init_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                                finish();
                            });
                    } else {
                        loadingDialog.dismiss();
                        Toast.makeText(LoginActivity.this, getString(R.string.auth_google_login_success), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    loadingDialog.dismiss();
                    AppLogger.e("LoginActivity", "Firestore get failed", task.getException());
                    Toast.makeText(LoginActivity.this, getString(R.string.auth_google_login_success), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
    }
}
