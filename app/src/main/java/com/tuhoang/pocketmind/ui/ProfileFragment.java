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
