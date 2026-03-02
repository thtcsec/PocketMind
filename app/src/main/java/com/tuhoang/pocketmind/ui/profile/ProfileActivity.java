package com.tuhoang.pocketmind.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.ActivityProfileBinding;
import com.tuhoang.pocketmind.ui.auth.LoginActivity;
import com.tuhoang.pocketmind.utils.AppLogger;
import com.tuhoang.pocketmind.utils.LoadingDialog;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LoadingDialog loadingDialog;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadingDialog = new LoadingDialog(this);
        currentUser = mAuth.getCurrentUser();

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserData();

        binding.btnSave.setOnClickListener(v -> saveProfile());
        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadUserData() {
        binding.etName.setText(currentUser.getDisplayName());
        binding.etEmail.setText(currentUser.getEmail());
        
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                .load(currentUser.getPhotoUrl())
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_profile)
                .into(binding.ivAvatar);
        }
    }

    private void saveProfile() {
        String newName = binding.etName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show("Đang lưu...");

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    db.collection("users").document(currentUser.getUid())
                        .update("name", newName)
                        .addOnSuccessListener(aVoid -> {
                            loadingDialog.dismiss();
                            Toast.makeText(this, "Lưu thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            loadingDialog.dismiss();
                            AppLogger.e("ProfileActivity", "Error updating Firestore", e);
                            Toast.makeText(this, "Lỗi cập nhật CSDL", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    loadingDialog.dismiss();
                    AppLogger.e("ProfileActivity", "Error updating Auth profile", task.getException());
                    Toast.makeText(this, "Lỗi cập nhật Profile", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showDeleteConfirmationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null);
        EditText etKeyword = dialogView.findViewById(R.id.etKeyword);
        CheckBox cbConfirm = dialogView.findViewById(R.id.cbConfirm);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.profile_delete_title))
            .setMessage(getString(R.string.profile_delete_message))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.profile_delete_account), (dialog, which) -> {
                String keyword = etKeyword.getText().toString().trim();
                boolean isChecked = cbConfirm.isChecked();
                String expectedKeyword = getString(R.string.profile_delete_keyword);

                if (!keyword.equals(expectedKeyword) || !isChecked) {
                    Toast.makeText(this, getString(R.string.profile_delete_invalid), Toast.LENGTH_LONG).show();
                    return;
                }

                executeAccountDeletion();
            })
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show();
    }

    private void executeAccountDeletion() {
        loadingDialog.show("Đang xử lý...");
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "pending_delete");
        
        db.collection("users").document(currentUser.getUid()).update(updates)
            .addOnSuccessListener(aVoid -> {
                loadingDialog.dismiss();
                mAuth.signOut();
                Toast.makeText(this, "Tài khoản đã chuyển sang trạng thái chờ xóa. Đã đăng xuất.", Toast.LENGTH_LONG).show();
                
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                loadingDialog.dismiss();
                AppLogger.e("ProfileActivity", "Error setting pending_delete", e);
                Toast.makeText(this, "Lỗi xử lý xóa tài khoản", Toast.LENGTH_SHORT).show();
            });
    }
}
