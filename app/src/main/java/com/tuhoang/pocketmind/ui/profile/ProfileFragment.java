package com.tuhoang.pocketmind.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseUser;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.FragmentProfileBinding;
import com.tuhoang.pocketmind.ui.auth.LoginActivity;
import com.tuhoang.pocketmind.ui.settings.SettingsActivity;
import com.tuhoang.pocketmind.utils.AppLogger;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        binding.btnSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), SettingsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                AppLogger.e("ProfileFragment", "Could not open settings", e);
            }
        });

        binding.btnProfile.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                AppLogger.e("ProfileFragment", "Could not open profile", e);
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
                    if (viewModel != null) viewModel.logout();
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
        });

        binding.btnAdminDashboard.setOnClickListener(v -> {
            try {
                // Must use fully qualified name or import
                Intent intent = new Intent(requireContext(), com.tuhoang.pocketmind.ui.admin.AdminDashboardActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                AppLogger.e("ProfileFragment", "Could not open admin dashboard", e);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateUI);
        viewModel.getUserRole().observe(getViewLifecycleOwner(), role -> {
            if ("admin".equals(role) && binding != null) {
                binding.btnAdminDashboard.setVisibility(View.VISIBLE);
            } else if (binding != null) {
                binding.btnAdminDashboard.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.fetchUserData();
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            if (binding != null) {
                binding.tvName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "User");
                binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                
                if (user.getPhotoUrl() != null) {
                    Glide.with(this)
                        .load(user.getPhotoUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.ic_profile)
                        .into(binding.ivAvatar);
                } else {
                    binding.ivAvatar.setImageResource(R.drawable.ic_profile);
                    binding.btnLogin.setText(R.string.get_started);
                }
                
                binding.btnLogin.setVisibility(View.GONE);
                binding.btnExportCsv.setVisibility(View.VISIBLE);
                binding.btnLogout.setVisibility(View.VISIBLE);
                binding.divLogout.setVisibility(View.VISIBLE);
                binding.btnProfile.setVisibility(View.VISIBLE);
                binding.divProfile.setVisibility(View.VISIBLE);
            }
            
            // Role is handled by ViewModel observer now
        } else {
            if (binding != null) {
                binding.tvName.setText(getText(R.string.guest));
                binding.tvEmail.setText(getText(R.string.please_login_to_sync));
                binding.ivAvatar.setImageResource(R.drawable.ic_profile);
                
                binding.btnLogin.setVisibility(View.VISIBLE);
                binding.btnExportCsv.setVisibility(View.GONE);
                binding.btnLogout.setVisibility(View.GONE);
                binding.divLogout.setVisibility(View.GONE);
                binding.btnAdminDashboard.setVisibility(View.GONE);
                binding.btnProfile.setVisibility(View.GONE);
                binding.divProfile.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
