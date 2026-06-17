import {
  badRequest,
  callOpenAi,
  decrementUserChatLimit,
  getUserChatLimit,
  jsonResponse,
  requireAuth,
  tryParseExpenseJson,
} from "./firebase.js";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const route = url.pathname;

    if (route === "/api/models" && request.method === "GET") {
      const auth = await requireAuth(request, env);
      if (auth.error) return auth.error;
      return fetchAllModels(env);
    }

    if (route === "/api/chat" && request.method === "POST") {
      return processAiChat(request, env);
    }

    return jsonResponse({ success: false, error: "Route not found" }, 404);
  },
};

async function fetchAllModels(env) {
  try {
    const openaiResp = await fetch("https://api.openai.com/v1/models", {
      headers: { Authorization: `Bearer ${env.OPENAI_API_KEY}` },
    });
    const openaiJson = await openaiResp.json();

    const anthropicResp = await fetch("https://api.anthropic.com/v1/models", {
      headers: {
        "x-api-key": env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
      },
    });
    const anthropicJson = await anthropicResp.json();

    const geminiResp = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models",
      { headers: { Authorization: `Bearer ${env.GEMINI_API_KEY}` } }
    );
    const geminiJson = await geminiResp.json();

    const usdToVnd = (usd) => Math.round(usd * 24000);

    return jsonResponse({
      success: true,
      models: {
        openai: openaiJson.data || [],
        anthropic: anthropicJson.data || [],
        gemini: geminiJson.models || [],
      },
      pricing: {
        openai_usd: 0,
        openai_vnd: usdToVnd(0),
        anthropic_usd: 0,
        anthropic_vnd: usdToVnd(0),
        gemini_usd: 0,
        gemini_vnd: usdToVnd(0),
      },
    });
  } catch (err) {
    return jsonResponse({ success: false, error: err.message }, 500);
  }
}

async function processAiChat(request, env) {
  try {
    const body = await request.json();
    const { userId, messages, provider } = body;

    if (!userId || !messages) {
      return badRequest("Missing required fields: userId, messages");
    }

    const auth = await requireAuth(request, env, userId);
    if (auth.error) return auth.error;

    const limit = await getUserChatLimit(env, userId);
    if (limit === null) {
      return jsonResponse(
        { success: false, error: "Server Firestore credentials not configured" },
        503
      );
    }
    if (limit <= 0) {
      return jsonResponse({ success: false, error: "Chat limit reached" }, 403);
    }

    const lastUserMessage = [...messages].reverse().find((m) => m.role === "user");
    const userText = lastUserMessage?.content || "";

    let replyMessage;
    let extractedData = null;

    if (env.OPENAI_API_KEY) {
      const systemPrompt =
        "You are PocketMind finance assistant. Extract expense/income from user text. " +
        "Reply with a short friendly sentence, then on a new line output ONLY JSON: " +
        '{"category":"...","amount":123,"note":"...","type":"expense|income"}';

      const llmText = await callOpenAi(env, [
        { role: "system", content: systemPrompt },
        { role: "user", content: userText },
      ]);

      if (llmText) {
        extractedData = tryParseExpenseJson(llmText);
        replyMessage = llmText.split("\n")[0].trim();
      }
    }

    if (!replyMessage) {
      extractedData = {
        category: "Food",
        amount: 50000,
        note: userText.slice(0, 120),
        type: "expense",
      };
      replyMessage =
        "Mock AI response (configure OPENAI_API_KEY on Worker for real extraction).";
    }

    const decremented = await decrementUserChatLimit(env, userId, limit);
    if (!decremented) {
      return jsonResponse({ success: false, error: "Failed to update chat limit" }, 500);
    }

    return jsonResponse({
      success: true,
      data: extractedData,
      message: replyMessage,
      provider: provider || "openai",
      remaining_chats: Math.max(0, limit - 1),
    });
  } catch (err) {
    return jsonResponse({ success: false, error: err.message }, 500);
  }
}
