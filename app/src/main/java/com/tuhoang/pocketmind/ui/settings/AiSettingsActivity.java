package com.tuhoang.pocketmind.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tuhoang.pocketmind.PocketMindApp;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.ActivityAiSettingsBinding;
import com.tuhoang.pocketmind.utils.AppLogger;
import com.tuhoang.pocketmind.utils.CurrencyUtils;
import com.tuhoang.pocketmind.utils.PaymentUtils;
import java.util.HashMap;
import java.util.Map;

public class AiSettingsActivity extends AppCompatActivity {

    private ActivityAiSettingsBinding binding;
    private final String[] providers = {"OpenAI (ChatGPT)", "Anthropic (Claude)", "Google (Gemini)", "DeepSeek"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup Providers Dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providers);
        binding.acAiProvider.setAdapter(adapter);

        // Define Toggles
        binding.toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                // Animate layout changes
                AutoTransition transition = new AutoTransition();
                transition.setDuration(300);
                TransitionManager.beginDelayedTransition(binding.rootLayout, transition);

                if (checkedId == R.id.btnModePocketMind) {
                    binding.llPocketMindPlans.setVisibility(View.VISIBLE);
                    binding.llApiKeyConfiguration.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnModeApiKey) {
                    binding.llPocketMindPlans.setVisibility(View.GONE);
                    binding.llApiKeyConfiguration.setVisibility(View.VISIBLE);
                }
            }
        });

        // Wiring Subscription Clicks
        binding.cardProPlan.setOnClickListener(v -> {
            showPaymentDialog("PRO_PLAN");
        });

        binding.cardMaxPlan.setOnClickListener(v -> {
            showPaymentDialog("MAX_PLAN");
        });

        // Wiring API Key Save
        binding.btnSaveApiKey.setOnClickListener(v -> {
            String selectedProvider = binding.acAiProvider.getText().toString();
            String key = binding.etApiKey.getText().toString().trim();

            if (selectedProvider.isEmpty()) {
                Toast.makeText(this, getString(R.string.ai_err_select_provider), Toast.LENGTH_SHORT).show();
                return;
            }

            if (key.isEmpty()) {
                Toast.makeText(this, getString(R.string.ai_err_enter_key), Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, String.format(getString(R.string.ai_msg_key_saved), selectedProvider), Toast.LENGTH_SHORT).show();
        });

        loadDynamicPricing();
    }

    private void loadDynamicPricing() {
        SharedPreferences prefs = getSharedPreferences(PocketMindApp.PREF_SETTINGS, Context.MODE_PRIVATE);
        String currentCurrency = prefs.getString(PocketMindApp.PREF_CURRENCY, "USD");

        binding.tvProPlanPrice.setText(getString(R.string.ai_loading));
        binding.tvMaxPlanPrice.setText(getString(R.string.ai_loading));

        FirebaseFirestore.getInstance().collection("pocketmind_ai_plans").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double proPrice = 4.99;
                double maxPrice = 9.99;
                
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    if ("PRO_PLAN".equals(doc.getId()) && doc.contains("price_usd")) {
                        proPrice = doc.getDouble("price_usd");
                    } else if ("MAX_PLAN".equals(doc.getId()) && doc.contains("price_usd")) {
                        maxPrice = doc.getDouble("price_usd");
                    }
                }
                
                fetchAndFormatPrices(proPrice, maxPrice, currentCurrency);
            })
            .addOnFailureListener(e -> {
                AppLogger.e("AiSettingsActivity", "Failed to fetch plans", e);
                // Fallback to defaults
                fetchAndFormatPrices(4.99, 9.99, currentCurrency);
            });
    }

    private void fetchAndFormatPrices(double proPriceUsd, double maxPriceUsd, String currentCurrency) {
        CurrencyUtils.fetchExchangeRate(currentCurrency, new CurrencyUtils.ExchangeRateCallback() {
            @Override
            public void onSuccess(double rate) {
                String proPriceStr = CurrencyUtils.formatPrice(proPriceUsd, rate, currentCurrency);
                String maxPriceStr = CurrencyUtils.formatPrice(maxPriceUsd, rate, currentCurrency);

                binding.tvProPlanPrice.setText(proPriceStr);
                binding.tvMaxPlanPrice.setText(maxPriceStr);
            }

            @Override
            public void onError(Exception e) {
                AppLogger.e("AiSettingsActivity", "Exchange Rate Error", e);
                binding.tvProPlanPrice.setText("$" + proPriceUsd + " / Month");
                binding.tvMaxPlanPrice.setText("$" + maxPriceUsd + " / Month");
                Toast.makeText(AiSettingsActivity.this, getString(R.string.ai_err_exchange_rate), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPaymentDialog(String planType) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.ai_err_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentCode = PaymentUtils.generatePaymentCode();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
        dialog.setContentView(view);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvCode = view.findViewById(R.id.tvPaymentCode);
        TextView tvTitle = view.findViewById(R.id.tvPaymentTitle);
        Button btnConfirm = view.findViewById(R.id.btnConfirmPaid);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        tvCode.setText(paymentCode);
        tvTitle.setText(String.format(getString(R.string.ai_toast_payment_title), planType.replace("_", " ")));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);
            btnConfirm.setText(getString(R.string.ai_msg_payment_processing));
            
            submitTransactionToFirestore(user.getUid(), planType, paymentCode, dialog);
        });

        dialog.show();
    }

    private void submitTransactionToFirestore(String userId, String planType, String paymentCode, Dialog dialog) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", userId);
        transaction.put("planType", planType);
        transaction.put("paymentCode", paymentCode);
        transaction.put("status", "pending");
        transaction.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("transactions").document(paymentCode)
            .set(transaction)
            .addOnSuccessListener(aVoid -> {
                AppLogger.d("Transaction pending: " + paymentCode);
                Toast.makeText(this, getString(R.string.ai_msg_payment_pending), Toast.LENGTH_LONG).show();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            })
            .addOnFailureListener(e -> {
                AppLogger.e("AiSettings Activity", "Error writing transaction", e);
                Toast.makeText(this, getString(R.string.ai_err_connection), Toast.LENGTH_SHORT).show();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
    }
}
