export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const route = url.pathname;

    // Simple router
    if (route === "/api/models") {
      return fetchAllModels(env);
    }
    
    if (route === "/api/chat" && request.method === "POST") {
      return processAiChat(request, env);
    }

    return new Response(
      JSON.stringify({
        success: false,
        error: "Route not found"
      }),
      { status: 404, headers: { "Content-Type": "application/json" } }
    );
  },
};

async function fetchAllModels(env) {
  try {
    // OpenAI models
    const openaiResp = await fetch("https://api.openai.com/v1/models", {
      headers: {
        Authorization: `Bearer ${env.OPENAI_API_KEY}`,
      },
    });
    const openaiJson = await openaiResp.json();

    // Anthropic models
    const anthropicResp = await fetch("https://api.anthropic.com/v1/models", {
      headers: {
        "x-api-key": env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
      },
    });
    const anthropicJson = await anthropicResp.json();

    // Google Gemini models
    const geminiResp = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models",
      {
        headers: {
          Authorization: `Bearer ${env.GEMINI_API_KEY}`,
        },
      }
    );
    const geminiJson = await geminiResp.json();

    // Combine and standardize
    const combined = {
      openai: openaiJson.data || [],
      anthropic: anthropicJson.data || [],
      gemini: geminiJson.models || [],
    };

    // Example price conversion:
    const usdToVnd = (usd) => Math.round(usd * 24000);

    return new Response(
      JSON.stringify({
        success: true,
        models: combined,
        pricing: {
          openai_usd: 0, // no pricing returned from model listing
          openai_vnd: usdToVnd(0),
          anthropic_usd: 0,
          anthropic_vnd: usdToVnd(0),
          gemini_usd: 0,
          gemini_vnd: usdToVnd(0),
        },
      }),
      { headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({
        success: false,
        error: err.message,
      }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

async function processAiChat(request, env) {
  try {
    const body = await request.json();
    const { userId, messages, provider } = body;

    if (!userId || !messages) {
      return new Response(JSON.stringify({ error: "Missing required fields" }), { status: 400 });
    }

    // TODO: In a real Cloudflare Worker with REST Firebase integration:
    // 1. Fetch /users/{userId} to check ai_chat_limit.
    // 2. If ai_chat_limit <= 0, return { error: "Chat limit reached" }.
    // 3. Make LLM API call to provider (OpenAI/Anthropic/Gemini).
    // 4. Decrement ai_chat_limit by 1 via Firestore REST API.
    // 5. Return LLM response.
    
    // For now, we mock a safe extraction response that respects the architecture.
    return new Response(JSON.stringify({
      success: true,
      data: {
        category: "Food",
        amount: 50000,
        note: "Extracted from chat mock",
        type: "expense"
      },
      message: "This is a mock response from the secure AI middle-tier. Limits should be deducted here."
    }), { headers: { "Content-Type": "application/json" } });

  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, error: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}