const GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function unauthorized(message = "Unauthorized") {
  return jsonResponse({ success: false, error: message }, 401);
}

function badRequest(message) {
  return jsonResponse({ success: false, error: message }, 400);
}

export function extractBearerToken(request) {
  const header = request.headers.get("Authorization") || "";
  if (!header.startsWith("Bearer ")) return null;
  return header.slice(7).trim();
}

export async function verifyIdToken(env, idToken) {
  if (!idToken || !env.FIREBASE_API_KEY) return null;

  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${env.FIREBASE_API_KEY}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken }),
    }
  );

  if (!response.ok) return null;

  const data = await response.json();
  const user = data.users?.[0];
  if (!user?.localId) return null;

  return {
    uid: user.localId,
    email: user.email || "",
  };
}

function pemToArrayBuffer(pem) {
  const cleaned = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const binary = atob(cleaned);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

function base64UrlEncode(data) {
  const str = typeof data === "string" ? data : String.fromCharCode(...new Uint8Array(data));
  return btoa(str).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

async function signJwt(payload, privateKeyPem) {
  const header = { alg: "RS256", typ: "JWT" };
  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const unsigned = `${encodedHeader}.${encodedPayload}`;

  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKeyPem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  );

  return `${unsigned}.${base64UrlEncode(signature)}`;
}

async function getServiceAccountAccessToken(env) {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    return null;
  }

  const now = Math.floor(Date.now() / 1000);
  const assertion = await signJwt(
    {
      iss: env.FIREBASE_CLIENT_EMAIL,
      scope: FIRESTORE_SCOPE,
      aud: GOOGLE_TOKEN_URI,
      iat: now,
      exp: now + 3600,
    },
    env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, "\n")
  );

  const response = await fetch(GOOGLE_TOKEN_URI, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  if (!response.ok) return null;
  const data = await response.json();
  return data.access_token || null;
}

function parseFirestoreInt(fields, key, fallback = 0) {
  const value = fields?.[key];
  if (!value) return fallback;
  if (value.integerValue != null) return parseInt(value.integerValue, 10);
  if (value.doubleValue != null) return Math.floor(value.doubleValue);
  return fallback;
}

export async function getUserChatLimit(env, uid) {
  const accessToken = await getServiceAccountAccessToken(env);
  if (!accessToken || !env.FIREBASE_PROJECT_ID) return null;

  const url =
    `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}` +
    `/databases/(default)/documents/users/${uid}`;

  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  if (response.status === 404) return 0;
  if (!response.ok) return null;

  const doc = await response.json();
  return parseFirestoreInt(doc.fields, "ai_chat_limit", 0);
}

export async function decrementUserChatLimit(env, uid, currentLimit) {
  const accessToken = await getServiceAccountAccessToken(env);
  if (!accessToken || !env.FIREBASE_PROJECT_ID) return false;

  const url =
    `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}` +
    `/databases/(default)/documents/users/${uid}` +
    `?updateMask.fieldPaths=ai_chat_limit`;

  const response = await fetch(url, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      fields: {
        ai_chat_limit: { integerValue: String(Math.max(0, currentLimit - 1)) },
      },
    }),
  });

  return response.ok;
}

export async function requireAuth(request, env, bodyUserId) {
  const idToken = extractBearerToken(request);
  if (!idToken) return { error: unauthorized("Missing Authorization Bearer token") };

  const user = await verifyIdToken(env, idToken);
  if (!user) return { error: unauthorized("Invalid or expired Firebase ID token") };

  if (bodyUserId && bodyUserId !== user.uid) {
    return { error: unauthorized("Token UID does not match request userId") };
  }

  return { user };
}

export async function callOpenAi(env, messages) {
  if (!env.OPENAI_API_KEY) return null;

  const response = await fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.OPENAI_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: env.OPENAI_MODEL || "gpt-4o-mini",
      messages,
      temperature: 0.2,
    }),
  });

  if (!response.ok) return null;
  const data = await response.json();
  return data.choices?.[0]?.message?.content || null;
}

export function tryParseExpenseJson(text) {
  if (!text) return null;
  try {
    const match = text.match(/\{[\s\S]*\}/);
    if (!match) return null;
    const parsed = JSON.parse(match[0]);
    if (!parsed.amount || !parsed.category) return null;
    return {
      category: String(parsed.category),
      amount: Number(parsed.amount),
      note: String(parsed.note || ""),
      type: String(parsed.type || "expense"),
    };
  } catch {
    return null;
  }
}

export { jsonResponse, badRequest };
