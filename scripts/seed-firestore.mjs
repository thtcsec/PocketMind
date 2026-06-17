/**
 * Usage: node scripts/seed-firestore.mjs path/to/firebase-adminsdk.json
 */
import { readFileSync } from "node:fs";
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const saPath = process.argv[2];
if (!saPath) {
  console.error("Usage: node scripts/seed-firestore.mjs <service-account.json>");
  process.exit(1);
}

const serviceAccount = JSON.parse(readFileSync(saPath, "utf8"));
if (!serviceAccount.project_id || !serviceAccount.client_email || !serviceAccount.private_key) {
  console.error(
    "Invalid service account file.\n" +
      "Expected Firebase Admin SDK JSON (project_id, client_email, private_key).\n" +
      "You may have used google-services.json by mistake.\n" +
      "Download from: Firebase Console → Project settings → Service accounts → Generate new private key"
  );
  process.exit(1);
}

initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

const workerUrl = "https://pocketmind.tht-csec2005.workers.dev";

await db.collection("system_configs").doc("global").set(
  { worker_url: workerUrl, updatedAt: new Date() },
  { merge: true }
);

const plans = [
  {
    id: "FREE_PLAN",
    data: {
      name: "Free Plan",
      is_active: true,
      price: { amount: 0, currency: "USD" },
      features: [{ key: "TEXT_CHAT_LIMIT", value: 5 }],
    },
  },
  {
    id: "PRO_PLAN",
    data: {
      name: "Pro Plan",
      is_active: true,
      price: { amount: 4.99, currency: "USD" },
      features: [
        { key: "UNLIM_TEXT", value: true },
        { key: "PRIORITY_QUEUE", value: true },
      ],
    },
  },
];

for (const plan of plans) {
  await db.collection("ai_plans").doc(plan.id).set(plan.data, { merge: true });
}

console.log("Seeded system_configs/global and ai_plans");
