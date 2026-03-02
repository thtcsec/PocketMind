package com.tuhoang.pocketmind.ui.chat;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;

import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.FragmentAddBinding;
import com.tuhoang.pocketmind.data.models.ChatMessage;
import com.tuhoang.pocketmind.utils.PrefsManager;

public class ChatFragment extends Fragment {
    private FragmentAddBinding binding;
    
    // Permission Launchers
    private ActivityResultLauncher<String> requestAudioPermissionLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    
    // AI Chat Dependencies
    private ChatAdapter chatAdapter;
    private ChatViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);

        setupPermissionLaunchers();
        setupToggleGroup();
        setupClickListeners();
        setupAiChat();

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
                pickImageLauncher.launch("image/*");
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_camera_permission), Toast.LENGTH_LONG).show();
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                viewModel.uploadImageAndSend(uri);
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
            boolean micEnabled = PrefsManager.getInstance().isMicEnabled(ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED);
            if (!micEnabled) {
                Toast.makeText(requireContext(), "Microphone is disabled in App Settings", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), getString(R.string.add_voice_recording), Toast.LENGTH_SHORT).show();
            } else {
                requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
            }
        });

        binding.btnUploadImage.setOnClickListener(v -> {
            boolean storageEnabled = PrefsManager.getInstance().isStorageEnabled(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED);
            if (!storageEnabled) {
                Toast.makeText(requireContext(), "Storage access is disabled in App Settings", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pickImageLauncher.launch("image/*");
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
    }

    private void setupAiChat() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvAiChat.setLayoutManager(layoutManager);
        binding.rvAiChat.setAdapter(chatAdapter);

        if (binding.rvAiChat.getAdapter() == null) {
            binding.rvAiChat.setAdapter(chatAdapter);
        }

        // Observe ViewModel
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            chatAdapter.setMessages(messages);
            if (!messages.isEmpty()) {
                binding.rvAiChat.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getErrorEvents().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getInfoEvents().observe(getViewLifecycleOwner(), info -> {
            if (info != null) {
                Toast.makeText(requireContext(), info, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.startListeningForMessages();

        // Setup Input Toggle
        binding.etAiInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    binding.btnAiSend.setVisibility(View.GONE);
                    binding.btnVoiceInput.setVisibility(View.VISIBLE);
                } else {
                    binding.btnAiSend.setVisibility(View.VISIBLE);
                    binding.btnVoiceInput.setVisibility(View.GONE);
                }
            }
        });

        // Handle Send Click
        binding.btnAiSend.setOnClickListener(v -> handleSendChat());

        // Handle Clear Chat
        binding.btnAiClear.setOnClickListener(v -> viewModel.clearChatHistory());
    }

    private void handleSendChat() {
        String currText = binding.etAiInput.getText().toString().trim();
        if (currText.isEmpty()) return;

        // Clear input
        binding.etAiInput.setText("");
        
        viewModel.sendMessage(currText);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
