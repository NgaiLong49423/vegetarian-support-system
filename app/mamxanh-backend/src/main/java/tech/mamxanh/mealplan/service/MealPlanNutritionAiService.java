package tech.mamxanh.mealplan.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionRecommendationsResponse;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;

@Service
public class MealPlanNutritionAiService {
    private static final Logger LOG = LoggerFactory.getLogger(MealPlanNutritionAiService.class);
    private final CurrentUserService users;
    private final NutritionProfileService profiles;
    private final SubscriptionEntitlementService entitlements;
    private final MealPlanRepository plans;
    private final MealPlanEntryRepository entries;
    private final RecipeService nutrition;
    private final MealPlanNutritionService analysisService;
    private final RecipePostService recipePosts;
    private final AiClient ai;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final AiTokenQuotaService aiQuota;

    public MealPlanNutritionAiService(CurrentUserService users, NutritionProfileService profiles,
            SubscriptionEntitlementService entitlements, MealPlanRepository plans, MealPlanEntryRepository entries,
            RecipeService nutrition, RecipePostService recipePosts, AiClient ai, ObjectMapper mapper,
            ApplicationEventPublisher events, Clock clock, MealPlanNutritionService analysisService,
            AiTokenQuotaService aiQuota) {
        this.users = users; this.profiles = profiles; this.entitlements = entitlements; this.plans = plans;
        this.entries = entries; this.nutrition = nutrition; this.recipePosts = recipePosts; this.ai = ai;
        this.mapper = mapper; this.events = events; this.clock = clock; this.analysisService = analysisService;
        this.aiQuota = aiQuota;
    }

    /** Provider call is deliberately outside a database transaction. */
    public MealPlanNutritionRecommendationsResponse recommend(LocalDate weekStart, long entryId) {
        MealPlanReadService.validateWeekStart(weekStart);
        long owner = users.requireActiveMember().id();
        profiles.requireEligible(owner);
        entitlements.requirePro(owner);
        var plan = plans.findByUserIdAndWeekStartDate(owner, weekStart)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        MealPlanEntryEntity selected = entries.findByIdAndMealPlanId(entryId, plan.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
        List<Long> candidateIds = entries.findPublishedCandidateIds(selected.getRecipeId());
        if (candidateIds.isEmpty()) return new MealPlanNutritionRecommendationsResponse(entryId,
                "Chưa có công thức công khai để đề xuất điều chỉnh.", List.of());
        var currentNutrition = nutrition.calculatePlannedNutrition(selected.getRecipeId(), selected.getPlannedServings());
        var currentAnalysis = analysisService.analyze(weekStart, selected.getMealDate());
        var candidateCards = recipePosts.getPublishedCards(candidateIds);
        Map<Long, RecipeService.PlannedNutrition> candidateNutrition = nutrition.calculatePlannedNutrition(
                candidateCards.stream().collect(java.util.stream.Collectors.toMap(RecipePostResponse::id,
                        ignored -> java.math.BigDecimal.ONE)));
        List<Long> allowed = candidateCards.stream().map(RecipePostResponse::id).toList();
        String prompt = "Analyze the supplied planned nutrition against its supplied reference targets. "
                + "Return a concise Vietnamese qualitative explanation in the analysis field, without any digits or numeric quantities. "
                + "Do not claim a nutrient is low/high unless the supplied values support that comparison. "
                + "Then choose up to 3 existing published recipes that could adjust the plan. "
                + "Use only supplied recipe IDs. Never invent nutrition; incomplete values stay incomplete. "
                + "Do not diagnose, prescribe, judge health, or add numeric claims.\n"
                + mapper.writeValueAsString(Map.of("currentMeal", currentNutrition, "analysis", currentAnalysis.days().get(0).nutrients(),
                        "referenceTargets", currentAnalysis.targets(), "candidates", candidateNutrition));
        AiClient.RecommendationResult result;
        try {
            aiQuota.requireAvailable(owner, null);
            result = ai.analyzeNutritionRecommendations(prompt, allowed, 3);
        }
        catch (AiProviderException exception) {
            LOG.warn("Meal plan nutrition AI unavailable ({})", exception.getClass().getSimpleName());
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }
        if (result == null) throw new AppException(ErrorCode.AI_RESPONSE_INVALID);
        var usageEvent = new AiUsageEvent(owner, "FR-37", clock.instant(), result.usage());
        aiQuota.record(usageEvent);
        events.publishEvent(usageEvent);
        String explanation = analysis(result.json());
        List<Long> ids = selection(result.json(), allowed);
        Map<Long, RecipePostResponse> cards = candidateCards.stream()
                .collect(java.util.stream.Collectors.toMap(RecipePostResponse::id, card -> card));
        List<MealPlanNutritionRecommendationsResponse.Recommendation> recommendations = ids.stream()
                .filter(candidateNutrition::containsKey).map(id -> {
                    var value = candidateNutrition.get(id);
                    return new MealPlanNutritionRecommendationsResponse.Recommendation(id, cards.get(id).title(),
                            value.complete(), value.ingredientsMissingData());
                }).toList();
        return new MealPlanNutritionRecommendationsResponse(entryId, explanation, recommendations);
    }

    private String analysis(String json) {
        try {
            var node = mapper.readTree(json);
            var analysis = node == null ? null : node.get("analysis");
            if (analysis == null || !analysis.isTextual() || analysis.asText().isBlank()
                    || analysis.asText().length() > 1200 || analysis.asText().matches("(?s).*\\d.*")) {
                throw invalid();
            }
            return analysis.asText().trim();
        } catch (JacksonException exception) { throw invalid(); }
    }

    private List<Long> selection(String json, List<Long> allowed) {
        try {
            if (json == null || json.length() > 8192) throw invalid();
            var ids = mapper.readTree(json).get("recipeIds");
            if (ids == null || !ids.isArray() || ids.size() > 3) throw invalid();
            var selected = new LinkedHashSet<Long>();
            for (var id : ids) {
                if (!id.isIntegralNumber() || !id.canConvertToLong() || !allowed.contains(id.asLong())) throw invalid();
                selected.add(id.asLong());
            }
            return new ArrayList<>(selected);
        } catch (JacksonException exception) { throw invalid(); }
    }

    private static AppException invalid() { return new AppException(ErrorCode.AI_RESPONSE_INVALID); }
}
