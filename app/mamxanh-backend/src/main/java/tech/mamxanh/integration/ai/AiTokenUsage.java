package tech.mamxanh.integration.ai;

/** Provider metadata only. Never carries prompt text or an SDK response object. */
public record AiTokenUsage(String model, String serviceTier, Integer promptTokens,
        Integer responseTokens, Integer totalTokens, Integer thinkingTokens,
        Integer cachedTokens, Integer toolUsePromptTokens) {
    public static final String MODEL = "gemini-3.8-flash";
    public static AiTokenUsage legacy(Integer prompt, Integer response, Integer total) {
        return new AiTokenUsage(MODEL, "STANDARD", prompt, response, total, null, null, null);
    }
}
