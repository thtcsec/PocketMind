package com.tuhoang.pocketmind.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.FragmentProfileBinding;
import com.tuhoang.pocketmind.ui.auth.LoginActivity;
import com.tuhoang.pocketmind.ui.settings.SettingsActivity;
import com.tuhoang.pocketmind.utils.AppLogger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.tuhoang.pocketmind.databinding.FragmentProfileBinding;
import com.tuhoang.pocketmind.ui.auth.LoginActivity;
import com.tuhoang.pocketmind.ui.settings.SettingsActivity;
import com.tuhoang.pocketmind.utils.AppLogger;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();

        binding.btnSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), SettingsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                AppLogger.e("ProfileFragment", "Could not open settings", e);
            }
        });

        binding.btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });

        binding.btnWorkerSync.setOnClickListener(v -> showWorkerSyncDialog());

        binding.btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.profile_logout_title))
                .setMessage(getString(R.string.profile_logout_msg))
                .setPositiveButton(getString(R.string.profile_logout_title), (dialog, which) -> {
                    mAuth.signOut();
                    updateUI(null);
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
        });

        return binding.getRoot();
    }

    private void showWorkerSyncDialog() {
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("PocketMindPrefs", Context.MODE_PRIVATE);
        String currentUrl = prefs.getString("PREF_WORKER_URL", "https://pocketmind-admin-worker.pocketmind.workers.dev");

        EditText input = new EditText(context);
        input.setText(currentUrl);
        input.setHint("Enter Worker URL");
        input.setPadding(40, 40, 40, 40);

        new AlertDialog.Builder(context)
                .setTitle("Cloudflare Worker Sync")
                .setMessage("Enter your worker URL to fetch the latest AI Models and Pricing.")
                .setView(input)
                .setPositiveButton("Sync", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        prefs.edit().putString("PREF_WORKER_URL", url).apply();
                        performApiSync(url);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performApiSync(String baseUrl) {
        String endpoint = baseUrl + (baseUrl.endsWith("/") ? "api/models" : "/api/models");
        
        Toast loadingToast = Toast.makeText(requireContext(), "Syncing AI Models...", Toast.LENGTH_SHORT);
        loadingToast.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                
                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    
                    String json = response.toString();
                    
                    handler.post(() -> {
                        SharedPreferences prefs = requireContext().getSharedPreferences("PocketMindPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("PREF_AI_MODELS_CACHE", json).apply();
                        Toast.makeText(requireContext(), "Sync Successful! Models Cached.", Toast.LENGTH_LONG).show();
                    });
                } else {
                    handler.post(() -> Toast.makeText(requireContext(), "Sync Error: HTTP " + code, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                AppLogger.e("ProfileFragment", "Worker Sync Failed", e);
                handler.post(() -> Toast.makeText(requireContext(), "Sync Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            binding.tvName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "User");
            binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            
            binding.btnLogin.setVisibility(View.GONE);
            binding.btnExportCsv.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.VISIBLE);
            binding.divLogout.setVisibility(View.VISIBLE);
        } else {
            binding.tvName.setText("Guest");
            binding.tvEmail.setText("Vui lòng đăng nhập để đồng bộ");
            
            binding.btnLogin.setVisibility(View.VISIBLE);
            binding.btnExportCsv.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.GONE);
            binding.divLogout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
