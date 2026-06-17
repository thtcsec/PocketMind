# Firebase Security Rules

## Deploy rules

```bash
npm install -g firebase-tools
firebase login
firebase use pocketmind-ceb5e
firebase deploy --only firestore:rules
```

## What the rules enforce

| Collection | Client can | Server/admin only |
|------------|-----------|-------------------|
| `users/{uid}` | Read/write own profile (not `role`, `ai_chat_limit`, `current_plan`) | Admin edits limits/roles |
| `users/{uid}/expenses` | Full CRUD on own data | — |
| `users/{uid}/chats` | Full CRUD on own data | — |
| `ai_plans` | Read when signed in | Admin write |
| `system_configs` | Read when signed in | Admin write |
| `transactions/{code}` | Create own `pending` payment | Verify/update status (admin or Worker) |

After deploying, test login, manual expense entry, and AI chat. If writes fail with `PERMISSION_DENIED`, check Firebase Console → Firestore → Rules tab for syntax errors.
