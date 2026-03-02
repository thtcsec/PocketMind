package com.tuhoang.pocketmind.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.data.models.ChatMessage;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private List<ChatMessage> messages = new ArrayList<>();

    public void setMessages(List<ChatMessage> newMessages) {
        this.messages.clear();
        this.messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        notifyItemInserted(this.messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isUser) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_AI;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.tvUserMessage.setText(message.content);
            if (message.imageUrl != null && !message.imageUrl.isEmpty()) {
                userHolder.ivUserImage.setVisibility(View.VISIBLE);
                Glide.with(userHolder.itemView.getContext())
                        .load(message.imageUrl)
                        .placeholder(R.drawable.ic_save) // Fallback placeholder
                        .into(userHolder.ivUserImage);
            } else {
                userHolder.ivUserImage.setVisibility(View.GONE);
            }
        } else {
            AiMessageViewHolder aiHolder = (AiMessageViewHolder) holder;
            aiHolder.tvAiMessage.setText(message.content);
            if (message.imageUrl != null && !message.imageUrl.isEmpty()) {
                aiHolder.ivAiImage.setVisibility(View.VISIBLE);
                Glide.with(aiHolder.itemView.getContext())
                        .load(message.imageUrl)
                        .placeholder(R.drawable.ic_save)
                        .into(aiHolder.ivAiImage);
            } else {
                aiHolder.ivAiImage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        android.widget.ImageView ivUserImage;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            ivUserImage = itemView.findViewById(R.id.ivUserImage);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvAiMessage;
        android.widget.ImageView ivAiImage;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAiMessage = itemView.findViewById(R.id.tvAiMessage);
            ivAiImage = itemView.findViewById(R.id.ivAiImage);
        }
    }
}
