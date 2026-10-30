package tech.mamxanh.subscription.dto.response;

import java.time.Instant;
import java.util.List;

public record AiChatSettingsResponse(String tier, long consumedTokens, long quotaTokens, long remainingTokens,
        Instant resetsAt, String selectedModelId, boolean selectionRequired, List<ModelOption> models) {
    public record ModelOption(String modelId, String displayName, boolean isDefault) { }
}
