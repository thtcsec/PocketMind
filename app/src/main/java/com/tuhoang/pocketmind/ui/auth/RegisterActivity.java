package com.tuhoang.pocketmind.ui.auth;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.tuhoang.pocketmind.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRegister.setOnClickListener(v -> {
            // Mock register logic, then finish Activity to go back to Login
            finish();
        });

        binding.tvLoginLink.setOnClickListener(v -> {
            finish();
        });
    }
}
