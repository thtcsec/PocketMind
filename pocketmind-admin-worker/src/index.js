export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const route = url.pathname;

    // Simple router
    if (route === "/api/models") {
      return fetchAllModels(env);
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