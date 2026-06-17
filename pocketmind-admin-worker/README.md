# Cloudflare Worker secrets (set via `wrangler secret put`)

| Secret | Required | Description |
|--------|----------|-------------|
| `FIREBASE_API_KEY` | Yes | Web API key from Firebase project settings |
| `FIREBASE_PROJECT_ID` | Yes | e.g. `pocketmind-tuhoang` |
| `FIREBASE_CLIENT_EMAIL` | Yes | Service account email for Firestore REST |
| `FIREBASE_PRIVATE_KEY` | Yes | Service account private key PEM (`\n` escaped) |
| `OPENAI_API_KEY` | No | Enables real LLM extraction |
| `OPENAI_MODEL` | No | Default `gpt-4o-mini` |
| `ANTHROPIC_API_KEY` | No | For `/api/models` |
| `GEMINI_API_KEY` | No | For `/api/models` |

## Setup

```bash
cd pocketmind-admin-worker
wrangler secret put FIREBASE_API_KEY
wrangler secret put FIREBASE_PROJECT_ID
wrangler secret put FIREBASE_CLIENT_EMAIL
wrangler secret put FIREBASE_PRIVATE_KEY
wrangler secret put OPENAI_API_KEY
wrangler deploy
```

## Auth

All `/api/chat` requests must include:

```
Authorization: Bearer <Firebase ID token>
```

The Worker verifies the token via Identity Toolkit, ensures `userId` matches the token UID, checks `ai_chat_limit`, then decrements server-side.
