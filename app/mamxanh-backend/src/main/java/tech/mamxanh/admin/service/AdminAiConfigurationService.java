package tech.mamxanh.admin.service;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.mamxanh.admin.dto.AdminAiConfigurationResponse;
import tech.mamxanh.admin.dto.AdminAiConfigurationResponse.CreditSetting;
import tech.mamxanh.admin.dto.AdminAiConfigurationResponse.ModelSetting;
import tech.mamxanh.admin.dto.AdminAiConfigurationResponse.PlanSetting;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest.ModelUpdate;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest.PlanUpdate;
import tech.mamxanh.admin.event.AiConfigurationEmailEvent;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.integration.ai.GeminiAiClient;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.notification.service.NotificationService;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Credit;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Model;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Plan;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;

/** Admin operations over the subscription-owned AI settings. Provider validation runs outside SQL transactions. */
@Service
public class AdminAiConfigurationService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(BUSINESS_ZONE);

    private final AiConfigurationRepository configurations;
    private final GeminiAiClient gemini;
    private final NotificationService notifications;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public AdminAiConfigurationService(AiConfigurationRepository configurations, GeminiAiClient gemini,
            NotificationService notifications, ApplicationEventPublisher events,
            PlatformTransactionManager transactionManager, Clock clock) {
        this.configurations = configurations;
        this.gemini = gemini;
        this.notifications = notifications;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public AdminAiConfigurationResponse getConfiguration() {
        AiConfigurationSnapshot current = transactions.execute(status -> loadCurrent());
        return response(current);
    }

    public AdminAiConfigurationResponse update(AdminAiConfigurationUpdateRequest request) {
        AiConfigurationSnapshot beforeValidation = transactions.execute(status -> loadCurrent());
        List<Model> requestedModels = normalizeModels(request.models());
        validateConfiguration(request, requestedModels);
        Set<String> existingIds = beforeValidation.models().stream().map(Model::modelId).collect(Collectors.toSet());

        for (Model model : requestedModels) {
            if (existingIds.contains(model.modelId())) continue;
            verifyNewModel(model.modelId());
        }

        return transactions.execute(status -> {
            AiConfigurationSnapshot current = loadCurrent();
            AiConfigurationSnapshot updated = apply(request, requestedModels, current);
            List<String> changes = describeChanges(current, updated, clock.instant());
            if (changes.isEmpty()) return response(current);

            updated.plans().forEach(configurations::updatePlan);
            configurations.replaceModels(updated.models());
            configurations.updateCredit(updated.credit());
            notifications.aiConfigurationChanged(changes);
            if (request.sendEmailNotification()) events.publishEvent(new AiConfigurationEmailEvent(changes));
            return response(updated);
        });
    }

    private AiConfigurationSnapshot loadCurrent() {
        configurations.promoteFreeQuotaIfDue(clock.instant());
        return configurations.findConfiguration();
    }

    private void verifyNewModel(String modelId) {
        try {
            if (!gemini.verifyModel(modelId)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_MODEL_NOT_AVAILABLE",
                        "Gemini model này không hỗ trợ tạo nội dung cho AI Assistant.");
            }
        } catch (AiProviderException failure) {
            if (failure.httpStatus() != null && (failure.httpStatus() == 400 || failure.httpStatus() == 404)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_MODEL_NOT_AVAILABLE",
                        "Gemini không tìm thấy model này hoặc model không khả dụng cho API project.");
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_MODEL_VALIDATION_UNAVAILABLE",
                    "Không thể xác minh model với Gemini. Cấu hình hiện tại chưa được thay đổi.");
        }
    }

    private static List<Model> normalizeModels(List<ModelUpdate> values) {
        List<Model> models = new ArrayList<>(values.size());
        Set<String> ids = new HashSet<>();
        for (ModelUpdate value : values) {
            String modelId = value.modelId().strip().toLowerCase(Locale.ROOT);
            if (!ids.add(modelId)) {
                throw validation("Danh sách model có model ID bị trùng.");
            }
            models.add(new Model(modelId, value.displayName().strip(), value.enabled(), value.freeAllowed(),
                    value.freeDefault(), value.plusAllowed(), value.plusDefault(), value.proAllowed(), value.proDefault()));
        }
        return List.copyOf(models);
    }

    private static void validateConfiguration(AdminAiConfigurationUpdateRequest request, List<Model> models) {
        Map<String, PlanUpdate> plans = new HashMap<>();
        for (PlanUpdate plan : request.plans()) {
            if (plans.put(plan.tier(), plan) != null) throw validation("Mỗi gói chỉ được cấu hình một lần.");
        }
        if (!plans.keySet().equals(Set.of("FREE", "PLUS", "PRO"))) {
            throw validation("Cấu hình phải có đủ ba gói Free, Plus và Pro.");
        }
        if (plans.get("FREE").priceVnd() != 0) throw validation("Gói Free luôn có giá 0đ.");
        if (plans.get("PLUS").priceVnd() <= 0 || plans.get("PRO").priceVnd() <= 0) {
            throw validation("Gói Plus và Pro phải có giá lớn hơn 0đ.");
        }
        if (request.credit().maximumTopUpVnd() < request.credit().minimumTopUpVnd()) {
            throw validation("Mức nạp tối đa phải lớn hơn hoặc bằng mức nạp tối thiểu.");
        }

        for (Model model : models) {
            if (!model.enabled() && (model.freeAllowed() || model.plusAllowed() || model.proAllowed()
                    || model.freeDefault() || model.plusDefault() || model.proDefault())) {
                throw validation("Model đang tắt không thể được cấp cho tier hoặc làm model mặc định.");
            }
            if ((model.freeDefault() && !model.freeAllowed()) || (model.plusDefault() && !model.plusAllowed())
                    || (model.proDefault() && !model.proAllowed())) {
                throw validation("Model mặc định phải được cấp quyền cho tier tương ứng.");
            }
        }
        requireTierDefault(models, "Free", Model::freeAllowed, Model::freeDefault);
        requireTierDefault(models, "Plus", Model::plusAllowed, Model::plusDefault);
        requireTierDefault(models, "Pro", Model::proAllowed, Model::proDefault);
    }

    private static void requireTierDefault(List<Model> models, String tier,
            Function<Model, Boolean> access, Function<Model, Boolean> defaultModel) {
        long available = models.stream().filter(Model::enabled).filter(model -> access.apply(model)).count();
        long defaults = models.stream().filter(Model::enabled).filter(model -> access.apply(model))
                .filter(model -> defaultModel.apply(model)).count();
        if (available == 0 || defaults != 1) {
            throw validation("Mỗi tier phải có ít nhất một model Gemini đang bật và đúng một model mặc định: " + tier + ".");
        }
    }

    private AiConfigurationSnapshot apply(AdminAiConfigurationUpdateRequest request, List<Model> models,
            AiConfigurationSnapshot current) {
        Map<String, Plan> existing = current.plans().stream().collect(Collectors.toMap(Plan::tier, Function.identity()));
        List<Plan> plans = request.plans().stream().map(value -> {
            Plan previous = existing.get(value.tier());
            if ("FREE".equals(value.tier())) {
                if (value.tokenQuota() == previous.tokenQuota()) {
                    return new Plan("FREE", 0, previous.tokenQuota(), null, null);
                }
                Instant effectiveAt = nextFreeCycle(clock.instant());
                return new Plan("FREE", 0, previous.tokenQuota(), value.tokenQuota(), effectiveAt);
            }
            return new Plan(value.tier(), value.priceVnd(), value.tokenQuota(), null, null);
        }).toList();
        Credit credit = new Credit(request.credit().creditsPer1000Vnd(),
                request.credit().creditsPer1000OverageTokens(), request.credit().minimumTopUpVnd(),
                request.credit().maximumTopUpVnd());
        return new AiConfigurationSnapshot(plans, models, credit);
    }

    private List<String> describeChanges(AiConfigurationSnapshot before, AiConfigurationSnapshot after, Instant now) {
        List<String> changes = new ArrayList<>();
        Map<String, Plan> oldPlans = before.plans().stream().collect(Collectors.toMap(Plan::tier, Function.identity()));
        for (Plan updated : after.plans()) {
            Plan old = oldPlans.get(updated.tier());
            if ("FREE".equals(updated.tier())) {
                if (!java.util.Objects.equals(old.pendingTokenQuota(), updated.pendingTokenQuota())) {
                    String oldValue = old.pendingTokenQuota() == null ? "Không có thay đổi chờ" : quota(old.pendingTokenQuota());
                    String newValue = updated.pendingTokenQuota() == null ? "Không có thay đổi chờ" : quota(updated.pendingTokenQuota());
                    changes.add("Quota Free: " + oldValue + " → " + newValue + "; có hiệu lực "
                            + (updated.pendingEffectiveAt() == null ? "từ chu kỳ tháng kế tiếp" : LOCAL_DATE_TIME.format(updated.pendingEffectiveAt()) + " (Asia/Saigon)"));
                }
                continue;
            }
            String label = "PLUS".equals(updated.tier()) ? "Plus" : "Pro";
            if (old.priceVnd() != updated.priceVnd()) {
                changes.add("Giá gói " + label + ": " + money(old.priceVnd()) + " → " + money(updated.priceVnd())
                        + "; áp dụng cho đơn mua/kích hoạt mới từ " + LOCAL_DATE_TIME.format(now) + " (Asia/Saigon).");
            }
            if (old.tokenQuota() != updated.tokenQuota()) {
                changes.add("Quota gói " + label + ": " + quota(old.tokenQuota()) + " → " + quota(updated.tokenQuota())
                        + "; áp dụng cho chu kỳ mua/kích hoạt mới từ " + LOCAL_DATE_TIME.format(now) + " (Asia/Saigon).");
            }
        }

        Map<String, Model> oldModels = before.models().stream().collect(Collectors.toMap(Model::modelId, Function.identity()));
        Map<String, Model> newModels = after.models().stream().collect(Collectors.toMap(Model::modelId, Function.identity()));
        Set<String> modelIds = new HashSet<>(oldModels.keySet());
        modelIds.addAll(newModels.keySet());
        for (String id : modelIds) {
            Model old = oldModels.get(id);
            Model updated = newModels.get(id);
            if (!java.util.Objects.equals(old, updated)) {
                changes.add("Gemini model " + id + ": " + modelSummary(old) + " → " + modelSummary(updated)
                        + "; có hiệu lực ngay từ " + LOCAL_DATE_TIME.format(now) + " (Asia/Saigon).");
            }
        }

        Credit oldCredit = before.credit();
        Credit newCredit = after.credit();
        if (!oldCredit.creditsPer1000Vnd().equals(newCredit.creditsPer1000Vnd())) {
            changes.add("Tỷ lệ nạp: " + oldCredit.creditsPer1000Vnd().stripTrailingZeros().toPlainString()
                    + " → " + newCredit.creditsPer1000Vnd().stripTrailingZeros().toPlainString()
                    + " Credit/1.000đ; áp dụng cho đơn mới từ " + LOCAL_DATE_TIME.format(now) + " (Asia/Saigon).");
        }
        if (oldCredit.creditsPer1000OverageTokens() != newCredit.creditsPer1000OverageTokens()) {
            changes.add("Tỷ lệ overage: " + number(oldCredit.creditsPer1000OverageTokens()) + " → "
                    + number(newCredit.creditsPer1000OverageTokens())
                    + " Credit/1.000 token; có hiệu lực ngay từ " + LOCAL_DATE_TIME.format(now) + " (Asia/Saigon).");
        }
        if (oldCredit.minimumTopUpVnd() != newCredit.minimumTopUpVnd()
                || oldCredit.maximumTopUpVnd() != newCredit.maximumTopUpVnd()) {
            changes.add("Giới hạn nạp: " + money(oldCredit.minimumTopUpVnd()) + "–" + money(oldCredit.maximumTopUpVnd())
                    + " → " + money(newCredit.minimumTopUpVnd()) + "–" + money(newCredit.maximumTopUpVnd())
                    + "; áp dụng cho đơn mới từ " + LOCAL_DATE_TIME.format(now) + " (Asia/Saigon).");
        }
        return List.copyOf(changes);
    }

    private static String modelSummary(Model model) {
        if (model == null) return "chưa có trong catalog";
        return (model.enabled() ? "đang bật" : "đang tắt") + ", tên " + model.displayName()
                + ", Free=" + access(model.freeAllowed(), model.freeDefault())
                + ", Plus=" + access(model.plusAllowed(), model.plusDefault())
                + ", Pro=" + access(model.proAllowed(), model.proDefault());
    }

    private static String access(boolean allowed, boolean isDefault) {
        return !allowed ? "không cấp" : isDefault ? "được cấp, mặc định" : "được cấp";
    }

    private static AdminAiConfigurationResponse response(AiConfigurationSnapshot snapshot) {
        return new AdminAiConfigurationResponse(snapshot.plans().stream()
                .map(value -> new PlanSetting(value.tier(), value.priceVnd(), value.tokenQuota(),
                        value.pendingTokenQuota(), value.pendingEffectiveAt())).toList(),
                snapshot.models().stream().map(value -> new ModelSetting(value.modelId(), value.displayName(),
                        value.enabled(), value.freeAllowed(), value.freeDefault(), value.plusAllowed(),
                        value.plusDefault(), value.proAllowed(), value.proDefault())).toList(),
                new CreditSetting(snapshot.credit().creditsPer1000Vnd(),
                        snapshot.credit().creditsPer1000OverageTokens(), snapshot.credit().minimumTopUpVnd(),
                        snapshot.credit().maximumTopUpVnd()));
    }

    private static Instant nextFreeCycle(Instant now) {
        LocalDate localDate = now.atZone(BUSINESS_ZONE).toLocalDate();
        return localDate.withDayOfMonth(1).plusMonths(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private static String money(int value) { return number(value) + "đ"; }
    private static String quota(long value) { return number(value) + " token/tháng"; }
    private static String number(long value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }

    private static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }
}
