package tech.mamxanh.subscription.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.subscription.dto.response.AiChatSettingsResponse;
import tech.mamxanh.subscription.dto.response.AiChatSettingsResponse.ModelOption;
import tech.mamxanh.subscription.dto.response.MyAiUsageResponse;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Model;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;

import java.util.List;
import java.util.Locale;

@Service
public class AiChatSettingsService {
    private final JdbcTemplate jdbc;
    private final AiConfigurationRepository configuration;
    private final AiTokenQuotaService quota;

    public AiChatSettingsService(JdbcTemplate jdbc, AiConfigurationRepository configuration, AiTokenQuotaService quota) {
        this.jdbc = jdbc;
        this.configuration = configuration;
        this.quota = quota;
    }

    @Transactional(readOnly = true)
    public AiChatSettingsResponse get(Long userId, String guestSessionId) {
        MyAiUsageResponse usage = userId == null ? quota.getGuestUsage(guestSessionId) : quota.getMyUsage(userId);
        return response(userId, usage);
    }

    @Transactional
    public AiChatSettingsResponse select(long userId, String modelId) {
        MyAiUsageResponse usage = quota.getMyUsage(userId);
        Model selected = allowedModels(usage.tier()).stream().filter(model -> model.modelId().equals(modelId.strip().toLowerCase(Locale.ROOT)))
                .findFirst().orElseThrow(() -> invalidSelection());
        if (!selected.enabled()) throw invalidSelection();
        int updated = jdbc.update("UPDATE \"USER\" SET preferred_ai_model_id = ?, updated_at = SYSUTCDATETIME() WHERE user_id = ?",
                selected.modelId(), userId);
        if (updated != 1) throw new AppException(ErrorCode.NOT_FOUND);
        return response(userId, usage);
    }

    @Transactional(readOnly = true)
    public String resolve(Long userId, String guestSessionId) {
        MyAiUsageResponse usage = userId == null ? quota.getGuestUsage(guestSessionId) : quota.getMyUsage(userId);
        List<Model> allowed = allowedModels(usage.tier());
        String preferred = userId == null ? null : jdbc.query("SELECT preferred_ai_model_id FROM \"USER\" WHERE user_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, userId);
        if (preferred != null) {
            String normalized = preferred.strip().toLowerCase(Locale.ROOT);
            if (allowed.stream().noneMatch(model -> model.modelId().equals(normalized) && model.enabled())) {
                throw invalidSelection();
            }
            return normalized;
        }
        return allowed.stream().filter(Model::enabled).filter(model -> isDefault(model, usage.tier()))
                .map(Model::modelId).findFirst().orElseThrow(AiChatSettingsService::invalidSelection);
    }

    private AiChatSettingsResponse response(Long userId, MyAiUsageResponse usage) {
        List<Model> allowed = allowedModels(usage.tier()).stream().filter(Model::enabled).toList();
        String preferred = userId == null ? null : jdbc.query("SELECT preferred_ai_model_id FROM \"USER\" WHERE user_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, userId);
        String defaultId = allowed.stream().filter(model -> isDefault(model, usage.tier())).map(Model::modelId)
                .findFirst().orElse(null);
        boolean invalidPreference = preferred != null && allowed.stream().noneMatch(model -> model.modelId().equalsIgnoreCase(preferred));
        String selected = invalidPreference ? null : preferred == null ? defaultId : preferred;
        return new AiChatSettingsResponse(usage.tier(), usage.consumedTokens(), usage.quotaTokens(), usage.remainingTokens(),
                "GUEST".equals(usage.tier()) ? null : usage.resetsAt(), selected, invalidPreference || selected == null,
                allowed.stream().map(model -> new ModelOption(model.modelId(), model.displayName(), model.modelId().equals(defaultId))).toList());
    }

    private List<Model> allowedModels(String tier) {
        List<Model> models = configuration.findConfiguration().models();
        return models.stream().filter(Model::enabled).filter(model -> switch (tier) {
            case "PLUS" -> model.plusAllowed();
            case "PRO" -> model.proAllowed();
            default -> model.freeAllowed();
        }).toList();
    }

    private static boolean isDefault(Model model, String tier) {
        return switch (tier) {
            case "PLUS" -> model.plusDefault();
            case "PRO" -> model.proDefault();
            default -> model.freeDefault();
        };
    }

    private static ApiException invalidSelection() {
        return new ApiException(HttpStatus.CONFLICT, "AI_MODEL_SELECTION_REQUIRED",
                "Model đang chọn không còn được gói của bạn hỗ trợ. Hãy chọn một model khả dụng trong Chatbox.");
    }
}
