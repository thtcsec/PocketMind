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

        // Remove previous hardcoded click listeners since cards are dynamic now
        // Wiring Subscription Clicks will be handled when cards are dynamically created


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

        loadDynamicPlans();
    }

    private void loadDynamicPlans() {
        SharedPreferences prefs = getSharedPreferences(PocketMindApp.PREF_SETTINGS, Context.MODE_PRIVATE);
        String currentCurrency = prefs.getString(PocketMindApp.PREF_CURRENCY, "USD");

        binding.llPocketMindPlans.removeAllViews(); // Clear any existing loading or placeholder views

        // Add Loading Text temporarily
        TextView tvLoading = new TextView(this);
        tvLoading.setText(getString(R.string.ai_loading));
        tvLoading.setPadding(16, 16, 16, 16);
        binding.llPocketMindPlans.addView(tvLoading);

        FirebaseFirestore.getInstance().collection("pocketmind_ai_plans_collection").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.llPocketMindPlans.removeAllViews();
                
                CurrencyUtils.fetchExchangeRate(currentCurrency, new CurrencyUtils.ExchangeRateCallback() {
                    @Override
                    public void onSuccess(double rate) {
                        renderPlans(queryDocumentSnapshots, rate, currentCurrency);
                    }

                    @Override
                    public void onError(Exception e) {
                        AppLogger.e("AiSettingsActivity", "Exchange Rate Error", e);
                        Toast.makeText(AiSettingsActivity.this, getString(R.string.ai_err_exchange_rate), Toast.LENGTH_SHORT).show();
                        renderPlans(queryDocumentSnapshots, 1.0, "USD"); // Fallback to USD
                    }
                });
            })
            .addOnFailureListener(e -> {
                AppLogger.e("AiSettingsActivity", "Failed to fetch AI plans", e);
                binding.llPocketMindPlans.removeAllViews();
                TextView errorText = new TextView(this);
                errorText.setText(getString(R.string.ai_err_connection));
                binding.llPocketMindPlans.addView(errorText);
            });
    }

    private void renderPlans(com.google.firebase.firestore.QuerySnapshot snapshots, double exchangeRate, String currencyCode) {
        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
            Boolean isActive = doc.getBoolean("is_active");
            if (isActive == null || !isActive) continue;

            String id = doc.getString("id");
            String name = doc.getString("name");
            
            Double priceUsd = 0.0;
            Object priceObj = doc.get("price");
            if (priceObj instanceof Map) {
                Map<String, Object> priceMap = (Map<String, Object>) priceObj;
                Object amountObj = priceMap.get("amount");
                if (amountObj instanceof Double) {
                    priceUsd = (Double) amountObj;
                } else if (amountObj instanceof Long) {
                    priceUsd = ((Long) amountObj).doubleValue();
                }
            }

            java.util.List<Map<String, Object>> features = (java.util.List<Map<String, Object>>) doc.get("features");

            if (id == null || name == null) continue;

            // Create Card
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 32); // 16dp bottom margin approx
            card.setLayoutParams(cardParams);
            card.setRadius(24f); // 12dp approx
            card.setCardElevation(8f);
            card.setClickable(true);
            card.setFocusable(true);

            // Set specific styling based on Plan ID
            int strokeColor = Color.parseColor("#9E9E9E"); // Default Grey
            int textColor = Color.parseColor("#4CAF50"); // Default Green
            
            if ("PRO_PLAN".equals(id)) {
                strokeColor = Color.parseColor("#3F51B5"); // Indigo
                textColor = Color.parseColor("#3F51B5");
                card.setStrokeWidth(4); // 2dp approx
            } else if ("MAX_PLAN".equals(id)) {
                strokeColor = Color.parseColor("#E91E63"); // Pink
                textColor = Color.parseColor("#E91E63");
                card.setStrokeWidth(4);
            } else {
                card.setStrokeWidth(2); // 1dp approx
            }
            card.setStrokeColor(strokeColor);

            // Create Inner Layout
            android.widget.LinearLayout innerLayout = new android.widget.LinearLayout(this);
            innerLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            innerLayout.setPadding(32, 32, 32, 32); // 16dp padding approx

            // Plan Title
            TextView tvTitle = new TextView(this);
            tvTitle.setText(name.toUpperCase());
            tvTitle.setTextSize(18f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(textColor);
            innerLayout.addView(tvTitle);

            // Features List
            if (features != null && !features.isEmpty()) {
                StringBuilder descBuilder = new StringBuilder();
                for (Map<String, Object> feature : features) {
                    String key = (String) feature.get("key");
                    Object val = feature.get("value");
                    
                    if ("TEXT_CHAT_LIMIT".equals(key)) {
                        descBuilder.append("• ").append(val).append(" Text Chats per cycle\n");
                    } else if ("UNLIM_TEXT".equals(key) && Boolean.TRUE.equals(val)) {
                        descBuilder.append("• Unlimited Text Chats\n");
                    } else if ("PRIORITY_QUEUE".equals(key) && Boolean.TRUE.equals(val)) {
                        descBuilder.append("• Priority Processing Queue\n");
                    } else if ("VOICE_ALLOWED".equals(key) && Boolean.TRUE.equals(val)) {
                        descBuilder.append("• Voice Interaction Included\n");
                    } else if ("IMAGE_PARSING".equals(key) && Boolean.TRUE.equals(val)) {
                        descBuilder.append("• Advanced Image & Receipt Parsing\n");
                    } else if ("PREMIUM_MODELS".equals(key) && val instanceof java.util.List) {
                        java.util.List<String> models = (java.util.List<String>) val;
                        descBuilder.append("• Premium Models: ").append(android.text.TextUtils.join(", ", models)).append("\n");
                    } else {
                        // Fallback generic string
                        descBuilder.append("• ").append(key).append(": ").append(val).append("\n");
                    }
                }
                TextView tvDesc = new TextView(this);
                tvDesc.setText(descBuilder.toString().trim());
                tvDesc.setTextColor(Color.parseColor("#757575")); // Grey
                
                android.widget.LinearLayout.LayoutParams descParams = new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                descParams.setMargins(0, 16, 0, 0); // 8dp mt
                tvDesc.setLayoutParams(descParams);
                innerLayout.addView(tvDesc);
            }

            // Price or Status Indicator
            TextView tvPrice = new TextView(this);
            android.widget.LinearLayout.LayoutParams priceParams = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            priceParams.setMargins(0, 32, 0, 0); // 16dp mt
            tvPrice.setLayoutParams(priceParams);
            
            if (priceUsd == 0.0) {
                tvPrice.setText(getString(R.string.ai_status_active));
                tvPrice.setTextSize(12f);
            } else {
                tvPrice.setText(CurrencyUtils.formatPrice(priceUsd, exchangeRate, currencyCode));
                tvPrice.setTextSize(16f);
                
                // Add click listener to show payment bounds only for non-free plans
                card.setOnClickListener(v -> showPaymentDialog(id));
            }
            tvPrice.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPrice.setTextColor(Color.BLACK);
            innerLayout.addView(tvPrice);

            card.addView(innerLayout);
            binding.llPocketMindPlans.addView(card);
        }
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
