package com.tuhoang.pocketmind.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.text.InputType;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.ActivityAdminDashboardBinding;
import com.tuhoang.pocketmind.utils.AppLogger;
import java.util.HashMap;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup Toolbar
        setSupportActionBar(binding.toolbarAdmin);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbarAdmin.setNavigationOnClickListener(v -> finish());
        
        binding.toolbarAdmin.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_config_worker) {
                showConfigWorkerDialog();
                return true;
            }
            return false;
        });

        // Setup RecyclerView
        binding.rvAdminContent.setLayoutManager(new LinearLayoutManager(this));

        // Initial Security Verification
        verifyAdminAccess();

        binding.tabLayoutAdmin.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadUsers();
                } else {
                    loadPlans();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });
    }

    private void verifyAdminAccess() {
        if (mAuth.getCurrentUser() == null) {
            kickOut();
            return;
        }

        binding.pbAdminLoading.setVisibility(View.VISIBLE);
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && "admin".equals(documentSnapshot.getString("role"))) {
                    // Passed security check
                    loadUsers();
                } else {
                    kickOut();
                }
            })
            .addOnFailureListener(e -> {
                AppLogger.e("AdminDashboard", "Verification failed", e);
                kickOut();
            });
    }

    private void kickOut() {
        Toast.makeText(this, getString(R.string.admin_err_no_access), Toast.LENGTH_LONG).show();
        finish();
    }

    private void showConfigWorkerDialog() {
        binding.pbAdminLoading.setVisibility(View.VISIBLE);
        db.collection("system_configs").document("global").get()
            .addOnSuccessListener(documentSnapshot -> {
                binding.pbAdminLoading.setVisibility(View.GONE);
                String currentUrl = documentSnapshot.exists() ? documentSnapshot.getString("worker_url") : "";
                
                EditText input = new EditText(this);
                input.setText(currentUrl);
                input.setHint("https://your-worker-url.workers.dev");
                input.setPadding(40, 40, 40, 40);

                new AlertDialog.Builder(this)
                        .setTitle("System Global Config")
                        .setMessage("Update the Cloudflare Worker URL globally for ALL users. This handles AI Models and Payments.")
                        .setView(input)
                        .setPositiveButton("Save Globally", (dialog, which) -> {
                            String newUrl = input.getText().toString().trim();
                            if (!newUrl.isEmpty()) {
                                Map<String, Object> data = new HashMap<>();
                                data.put("worker_url", newUrl);
                                data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                
                                binding.pbAdminLoading.setVisibility(View.VISIBLE);
                                db.collection("system_configs").document("global").set(data, com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        binding.pbAdminLoading.setVisibility(View.GONE);
                                        Toast.makeText(this, "Global Worker URL updated!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        binding.pbAdminLoading.setVisibility(View.GONE);
                                        Toast.makeText(this, "Failed to update URL.", Toast.LENGTH_SHORT).show();
                                    });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            })
            .addOnFailureListener(e -> {
                binding.pbAdminLoading.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to fetch current config", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadUsers() {
        binding.pbAdminLoading.setVisibility(View.VISIBLE);
        
        AdminUserAdapter adapter = new AdminUserAdapter(this::showEditUserDialog);
        binding.rvAdminContent.setAdapter(adapter);

        db.collection("users").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.pbAdminLoading.setVisibility(View.GONE);
                adapter.setUsers(queryDocumentSnapshots.getDocuments());
            })
            .addOnFailureListener(e -> {
                binding.pbAdminLoading.setVisibility(View.GONE);
                AppLogger.e("AdminDashboard", "Failed to load users", e);
                Toast.makeText(this, "Error fetching users", Toast.LENGTH_SHORT).show();
            });
    }

    private void showEditUserDialog(DocumentSnapshot userDoc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit User: " + userDoc.getString("name"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etRole = new EditText(this);
        etRole.setHint("Role (user/admin)");
        etRole.setText(userDoc.getString("role"));
        layout.addView(etRole);

        final EditText etLimit = new EditText(this);
        etLimit.setHint("AI Chat Limit");
        etLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
        Long limit = userDoc.getLong("ai_chat_limit");
        if (limit != null) etLimit.setText(String.valueOf(limit));
        layout.addView(etLimit);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newRole = etRole.getText().toString().trim();
            String limitStr = etLimit.getText().toString().trim();
            
            Map<String, Object> updates = new HashMap<>();
            if (!newRole.isEmpty()) updates.put("role", newRole);
            if (!limitStr.isEmpty()) {
                try {
                    updates.put("ai_chat_limit", Long.parseLong(limitStr));
                } catch (NumberFormatException ignored) {}
            }

            if (!updates.isEmpty()) {
                db.collection("users").document(userDoc.getId()).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show();
                        loadUsers(); // Refresh
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadPlans() {
        binding.pbAdminLoading.setVisibility(View.VISIBLE);

        AdminPlanAdapter adapter = new AdminPlanAdapter(this::showEditPlanDialog);
        binding.rvAdminContent.setAdapter(adapter);

        db.collection("ai_plans").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.pbAdminLoading.setVisibility(View.GONE);
                adapter.setPlans(queryDocumentSnapshots.getDocuments());
            })
            .addOnFailureListener(e -> {
                binding.pbAdminLoading.setVisibility(View.GONE);
                AppLogger.e("AdminDashboard", "Failed to load plans", e);
                Toast.makeText(this, "Error fetching plans", Toast.LENGTH_SHORT).show();
            });
    }

    private void showEditPlanDialog(DocumentSnapshot planDoc) {
        Toast.makeText(this, "Edit Plan clicked for: " + planDoc.getString("name") + "\n(Detailed UI logic omitted for brevity)", Toast.LENGTH_SHORT).show();
    }

}
