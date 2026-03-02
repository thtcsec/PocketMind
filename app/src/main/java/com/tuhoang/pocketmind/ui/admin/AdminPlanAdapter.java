package com.tuhoang.pocketmind.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.tuhoang.pocketmind.R;
import java.util.ArrayList;
import java.util.List;

public class AdminPlanAdapter extends RecyclerView.Adapter<AdminPlanAdapter.ViewHolder> {

    private List<DocumentSnapshot> plans = new ArrayList<>();
    private final OnPlanEditClickListener listener;

    public interface OnPlanEditClickListener {
        void onEditClick(DocumentSnapshot planDoc);
    }

    public AdminPlanAdapter(OnPlanEditClickListener listener) {
        this.listener = listener;
    }

    public void setPlans(List<DocumentSnapshot> plans) {
        this.plans = plans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = plans.get(position);
        
        String name = doc.getString("name");
        Boolean isActive = doc.getBoolean("is_active");
        
        holder.tvName.setText(name != null ? name : "Unknown Plan");
        if (isActive != null && isActive) {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(0xFF4CAF50); // Green
        } else {
            holder.tvStatus.setText("Inactive");
            holder.tvStatus.setTextColor(0xFFF44336); // Red
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(doc));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        Button btnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminPlanName);
            tvStatus = itemView.findViewById(R.id.tvAdminPlanStatus);
            btnEdit = itemView.findViewById(R.id.btnAdminEditPlan);
        }
    }
}
