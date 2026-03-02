package com.tuhoang.pocketmind.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingDialog {
    private AlertDialog dialog;

    public LoadingDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        
        ProgressBar pb = new ProgressBar(context);
        layout.addView(pb);
        
        TextView tv = new TextView(context);
        tv.setId(android.R.id.message);
        tv.setText("Loading...");
        tv.setTextSize(16);
        tv.setPadding(30, 0, 0, 0);
        layout.addView(tv);
        
        builder.setView(layout);
        dialog = builder.create();
    }

    public void show(String message) {
        if (dialog != null) {
            TextView tv = dialog.findViewById(android.R.id.message);
            if (tv != null) {
                tv.setText(message);
            }
            if (!dialog.isShowing()) {
                dialog.show();
            }
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
