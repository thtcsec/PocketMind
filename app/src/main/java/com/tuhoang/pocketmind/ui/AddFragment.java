package com.tuhoang.pocketmind.ui;

import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.FragmentAddBinding;

public class AddFragment extends Fragment {
    private FragmentAddBinding binding;
    
    // Permission Launchers
    private ActivityResultLauncher<String> requestAudioPermissionLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);

        setupPermissionLaunchers();
        setupToggleGroup();
        setupClickListeners();

        return binding.getRoot();
    }

    private void setupPermissionLaunchers() {
        requestAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(requireContext(), getString(R.string.add_voice_recording), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_mic_permission), Toast.LENGTH_LONG).show();
            }
        });

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(requireContext(), getString(R.string.add_camera_opening), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_camera_permission), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupToggleGroup() {
        binding.toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                // Animate layout changes
                AutoTransition transition = new AutoTransition();
                transition.setDuration(250);
                TransitionManager.beginDelayedTransition(binding.rootLayout, transition);

                if (checkedId == R.id.btnModeManual) {
                    binding.llManualMode.setVisibility(View.VISIBLE);
                    binding.llAiMode.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnModeAi) {
                    binding.llManualMode.setVisibility(View.GONE);
                    binding.llAiMode.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setupClickListeners() {
        // Manual Mode Clicks
        binding.btnSave.setOnClickListener(v -> {
            Toast.makeText(requireContext(), getString(R.string.add_saving_manual), Toast.LENGTH_SHORT).show();
        });

        // AI Mode Clicks
        binding.btnVoiceInput.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), getString(R.string.add_voice_recording), Toast.LENGTH_SHORT).show();
            } else {
                requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
            }
        });

        binding.btnUploadImage.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), getString(R.string.add_camera_opening), Toast.LENGTH_SHORT).show();
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
