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

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private List<DocumentSnapshot> users = new ArrayList<>();
    private final OnUserEditClickListener listener;

    public interface OnUserEditClickListener {
        void onEditClick(DocumentSnapshot userDoc);
    }

    public AdminUserAdapter(OnUserEditClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<DocumentSnapshot> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = users.get(position);
        
        String name = doc.getString("name");
        String email = doc.getString("email");
        String role = doc.getString("role");
        Long limitObj = doc.getLong("ai_chat_limit");
        
        holder.tvName.setText(name != null ? name : "Unknown");
        holder.tvEmail.setText(email != null ? email : "No Email");
        holder.tvRole.setText(role != null ? role : "user");
        holder.tvLimit.setText(limitObj != null ? String.valueOf(limitObj) : "?");

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(doc));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole, tvLimit;
        Button btnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminUserName);
            tvEmail = itemView.findViewById(R.id.tvAdminUserEmail);
            tvRole = itemView.findViewById(R.id.tvAdminUserRole);
            tvLimit = itemView.findViewById(R.id.tvAdminUserLimit);
            btnEdit = itemView.findViewById(R.id.btnAdminEditUser);
        }
    }
}
