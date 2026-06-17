# Security Architecture

## Trust boundaries

| Layer | Trust level | Responsibility |
|-------|-------------|----------------|
| Android app | Untrusted client | UI, Firebase Auth session, read/write own data per rules |
| Firestore Rules | Enforcement | Block privilege escalation (`role`, `ai_chat_limit`) |
| Cloudflare Worker | Trusted middle-tier | Verify ID token, hold API keys, decrement limits |
| Firebase Admin / Functions | Trusted backend | Payment verification, admin ops |

## Deploy checklist

1. `firebase deploy --only firestore:rules` — see [FIREBASE_DEPLOY.md](./FIREBASE_DEPLOY.md)
2. Configure Worker secrets — see [pocketmind-admin-worker/README.md](../pocketmind-admin-worker/README.md)
3. Add debug/release SHA-1 to Firebase Console for Google Sign-In
4. Never commit service account JSON or API keys

## What attackers gain from decompiling the APK

- Firebase Web API key (expected; protected by rules + Auth)
- Worker URL (expected; protected by token verification)
- **Cannot** safely: forge admin role, bump chat limits, verify payments, read other users' data (with rules deployed)
