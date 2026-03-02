package com.tuhoang.pocketmind.data.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Transaction {
    private String id;
    private double amount;
    private String type; // "income" or "expense"
    private String category;
    private String note;
    @ServerTimestamp
    private Date timestamp;

    public Transaction() {
        // Required empty constructor for Firestore Serialization
    }

    public Transaction(String id, double amount, String type, String category, String note) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.note = note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
