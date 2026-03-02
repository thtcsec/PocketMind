const express = require("express");
const cors = require("cors");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
const serviceAccount = require("../pocketmind-ceb5e-firebase-adminsdk-fbsvc-52840f33f3.json");
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const app = express();

app.use(cors());
app.use(express.json());

// Basic health check endpoint
app.get("/", (req, res) => {
    res.json({ status: "PocketMind Admin API is running!" });
});

/**
 * 1. AI Chat Limit Decrement Endpoint
 * Protected by secret key or JWT logic (simplified here for brevity)
 */
app.post("/api/decrement-limit", async (req, res) => {
    try {
        const { uid, admin_secret } = req.body;
        
        // Simple security check (replace with real auth in production)
        if (admin_secret !== process.env.ADMIN_SECRET) {
            return res.status(403).json({ error: "Unauthorized" });
        }

        if (!uid) return res.status(400).json({ error: "Missing UID" });

        const userRef = db.collection("users").doc(uid);
        const doc = await userRef.get();
        
        if (!doc.exists) {
            return res.status(404).json({ error: "User not found" });
        }

        const currentLimit = doc.data().ai_chat_limit || 0;
        if (currentLimit <= 0) {
            return res.status(403).json({ error: "No AI chats remaining" });
        }

        await userRef.update({
            ai_chat_limit: admin.firestore.FieldValue.increment(-1)
        });

        res.json({ success: true, message: `Decremented limit for ${uid}` });
    } catch (error) {
        console.error("Error decrementing limit:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

/**
 * 2. Process Manual Payment Command
 * Marks a pending transaction as success and grants the AI Plan limits.
 */
app.post("/api/verify-payment", async (req, res) => {
    try {
        const { transactionId, admin_secret } = req.body;
        if (admin_secret !== process.env.ADMIN_SECRET) {
             return res.status(403).json({ error: "Unauthorized" });
        }

        const txRef = db.collection("transactions").doc(transactionId);
        const txDoc = await txRef.get();
        if (!txDoc.exists) return res.status(404).json({ error: "Transaction not found" });
        
        const txData = txDoc.data();
        if (txData.status === "success") {
            return res.status(400).json({ error: "Already verified" });
        }

        // Fetch Plan Info
        const planDoc = await db.collection("ai_plans").doc(txData.planId).get();
        const planData = planDoc.data();

        // Transaction DB updates
        const batch = db.batch();
        batch.update(txRef, { 
            status: "success", 
            verified_at: admin.firestore.FieldValue.serverTimestamp() 
        });

        // Add 500 limits for demonstration
        const addedLimit = planData.limit || 500; 
        const userRef = db.collection("users").doc(txData.userId);
        batch.update(userRef, {
            ai_chat_limit: admin.firestore.FieldValue.increment(addedLimit),
            current_plan: txData.planId
        });

        await batch.commit();
        res.json({ success: true, message: "Payment verified & user updated" });
    } catch (error) {
        console.error("Payment Verification Error:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log(`PocketMind Admin Worker running on port ${PORT}`);
});
