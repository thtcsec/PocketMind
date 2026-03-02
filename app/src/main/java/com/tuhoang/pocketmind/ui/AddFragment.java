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
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;

import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.FragmentAddBinding;
import com.tuhoang.pocketmind.data.AppDatabase;
import com.tuhoang.pocketmind.data.models.ChatMessage;
import com.tuhoang.pocketmind.ui.adapters.ChatAdapter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddFragment extends Fragment {
    private FragmentAddBinding binding;
    
    // Permission Launchers
    private ActivityResultLauncher<String> requestAudioPermissionLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    
    // AI Chat Dependencies
    private ChatAdapter chatAdapter;
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

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

    private void setupAiChat() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvAiChat.setLayoutManager(layoutManager);
        binding.rvAiChat.setAdapter(chatAdapter);

        // Load messages from DB
        diskIO.execute(() -> {
            List<ChatMessage> history = AppDatabase.getDatabase(requireContext()).chatDao().getAllMessages();
            mainThreadHandler.post(() -> {
                chatAdapter.setMessages(history);
                if (!history.isEmpty()) {
                    binding.rvAiChat.scrollToPosition(history.size() - 1);
                }
            });
        });

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

        // Handle Clear Chat (Plan D: Explicit Reset)
        binding.btnAiClear.setOnClickListener(v -> {
            chatAdapter.setMessages(new java.util.ArrayList<>());
            diskIO.execute(() -> {
                AppDatabase.getDatabase(requireContext()).chatDao().deleteAll();
            });
            android.widget.Toast.makeText(requireContext(), "Chat context wiped.", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void handleSendChat() {
        String currText = binding.etAiInput.getText().toString().trim();
        if (currText.isEmpty()) return;

        // Clear input
        binding.etAiInput.setText("");

        // Add user message
        ChatMessage userMsg = new ChatMessage(currText, true, System.currentTimeMillis());
        chatAdapter.addMessage(userMsg);
        binding.rvAiChat.scrollToPosition(chatAdapter.getItemCount() - 1);

        diskIO.execute(() -> {
            AppDatabase.getDatabase(requireContext()).chatDao().insert(userMsg);
        });
        
        // Simulate response for now
        simulateAiResponse();
    }
    
    private void simulateAiResponse() {
        mainThreadHandler.postDelayed(() -> {
            ChatMessage botMsg = new ChatMessage("I will categorize your last transaction automatically.", false, System.currentTimeMillis());
            chatAdapter.addMessage(botMsg);
            binding.rvAiChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            
            diskIO.execute(() -> {
                AppDatabase.getDatabase(requireContext()).chatDao().insert(botMsg);
            });
        }, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
