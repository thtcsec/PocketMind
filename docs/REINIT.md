# PocketMind reinit (2026-06-17)

## Firebase project mới

| Field | Value |
|-------|-------|
| Project ID | `pocketmind-tuhoang` |
| Console | https://console.firebase.google.com/project/pocketmind-tuhoang |
| Android package | `com.tuhoang.pocketmind` |
| App ID | `1:523667388910:android:3917d88bffe4382531b6ff` |
| Firestore region | `asia-southeast1` |
| Debug SHA-1 | `39:3B:F4:50:D5:62:7A:F6:4D:40:1F:17:93:F0:86:3F:74:84:85:66` |

## Cloudflare Worker (tài khoản cá nhân)

| Field | Value |
|-------|-------|
| URL | https://pocketmind.tht-csec2005.workers.dev |
| Account | `Tht.csec2005@gmail.com's Account` |

## Việc còn lại (Console, ~5 phút)

### 0. Blaze / billing workshop ($5 credit)

Project đã lên **Blaze** — **ổn cho dev/MVP**. Blaze chỉ tính phí khi vượt free quota (Auth, Firestore reads…). Credit workshop thường đủ cho vài tháng dev.

**Xóa / gọn billing account thừa** (nhiều workshop):

1. [GCP Billing](https://console.cloud.google.com/billing) → danh sách billing accounts
2. Mỗi account workshop: **Close billing account** (chỉ khi không còn project nào link)
3. Với `pocketmind-tuhoang`: giữ **một** billing account đang link
4. Khuyến nghị: [Budget & alerts](https://console.cloud.google.com/billing/budgets) — alert $1 / $5

### 1. Firestore rules

```bash
firebase deploy --only firestore:rules --project pocketmind-tuhoang
```

### 2. Firebase Storage (avatar)

Console → Storage → **Get started** (chọn region gần VN, ví dụ `asia-southeast1`).

```bash
firebase deploy --only storage --project pocketmind-tuhoang
```

### 3. Authentication

Firebase Console → Authentication → Sign-in method:

- Email/Password: Enable
- Google: Enable

SHA-1 debug đã add — tải lại config nếu đổi máy:

```bash
firebase apps:sdkconfig ANDROID 1:523667388910:android:3917d88bffe4382531b6ff --out app/google-services.json
```

### 4. Seed Firestore + Worker secrets

Copy file service account JSON vào `secrets/` (xem `secrets/README.md`), rồi:

```powershell
.\scripts\setup-worker-secrets.ps1 -ServiceAccountPath ".\secrets\pocketmind-tuhoang-adminsdk.json"
cd scripts; npm install; cd ..
node scripts/seed-firestore.mjs secrets/pocketmind-tuhoang-adminsdk.json
```

Hoặc set secrets thủ công:

```bash
cd pocketmind-admin-worker
npx wrangler secret put FIREBASE_API_KEY
npx wrangler secret put FIREBASE_PROJECT_ID
npx wrangler secret put FIREBASE_CLIENT_EMAIL
npx wrangler secret put FIREBASE_PRIVATE_KEY
npx wrangler secret put OPENAI_API_KEY
```

Giá trị gợi ý:

- `FIREBASE_API_KEY` = key trong `app/google-services.json`
- `FIREBASE_PROJECT_ID` = `pocketmind-tuhoang`
