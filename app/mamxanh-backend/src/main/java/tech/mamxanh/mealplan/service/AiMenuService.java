package tech.mamxanh.mealplan.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.AiProviderException;
import tech.mamxanh.mealplan.dto.request.ApplyAiMenuRequest;
import tech.mamxanh.mealplan.dto.request.CreateAiMenuPreviewRequest;
import tech.mamxanh.mealplan.dto.response.AiMenuPreviewResponse;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.service.*;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiMenuService {
    private static final int CANDIDATE_LIMIT = 50;
    private static final List<String> MEALS = List.of("BREAKFAST", "LUNCH", "DINNER");
    private static final String DISCLAIMER = "Dinh dưỡng là giá trị dự kiến theo dữ liệu công thức; mục tiêu chỉ dùng để ưu tiên món, không phải cam kết kết quả sức khỏe.";

    private final CurrentUserService users;
    private final SubscriptionEntitlementService entitlements;
    private final NutritionProfileService nutritionProfiles;
    private final DietaryPreferenceService dietaryPreferences;
    private final RecipePostService recipes;
    private final RecipeNutritionService recipeNutrition;
    private final IngredientCatalogLookupService ingredientCatalog;
    private final IngredientConversionLookupService conversions;
    private final AiClient ai;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final MealPlanRepository plans;
    private final MealPlanEntryRepository entries;
    private final MealPlanReadService readService;
    private final AiTokenQuotaService aiQuota;
    private final AiMenuOptimizer optimizer = new AiMenuOptimizer();

    public AiMenuService(CurrentUserService users, SubscriptionEntitlementService entitlements,
            NutritionProfileService nutritionProfiles, DietaryPreferenceService dietaryPreferences,
            RecipePostService recipes, RecipeNutritionService recipeNutrition,
            IngredientCatalogLookupService ingredientCatalog, IngredientConversionLookupService conversions,
            AiClient ai, ApplicationEventPublisher events, Clock clock, ObjectMapper objectMapper,
            MealPlanRepository plans, MealPlanEntryRepository entries,
            MealPlanReadService readService, AiTokenQuotaService aiQuota) {
        this.users = users;
        this.entitlements = entitlements;
        this.nutritionProfiles = nutritionProfiles;
        this.dietaryPreferences = dietaryPreferences;
        this.recipes = recipes;
        this.recipeNutrition = recipeNutrition;
        this.ingredientCatalog = ingredientCatalog;
        this.conversions = conversions;
        this.ai = ai;
        this.events = events;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.plans = plans;
        this.entries = entries;
        this.readService = readService;
        this.aiQuota = aiQuota;
    }

    public AiMenuPreviewResponse createPreview(CreateAiMenuPreviewRequest request) {
        MenuContext context = resolveMenuContext(request);
        if (context.firstSuggestionDate().isAfter(context.lastSuggestionDate())) {
            return preview(request.weekStartDate(), request.mode(), context.goal(), List.of(), List.of(),
                    context.preferred(), context.profile());
        }
        List<Candidate> pool = context.pool();
        if (pool.isEmpty()) throw new AppException(ErrorCode.AI_RESPONSE_INVALID,
                "Chưa có đủ công thức công khai phù hợp và có dữ liệu dinh dưỡng để tạo thực đơn.");
        List<Long> ids = pool.stream().map(c -> c.recipe().id()).toList();
        String prompt = prompt(context.firstSuggestionDate(), context.lastSuggestionDate(), context.goal(),
                context.vegetarianType(), context.preferred(), context.allAvoid(), pool, context.profile());
        try {
            long userId = users.requireActiveMember().id();
            aiQuota.requireAvailable(userId, null);
            var result = ai.recommendRecipes(prompt, ids, ids.size());
            if (result == null) throw new AppException(ErrorCode.AI_RESPONSE_INVALID);
            var usageEvent = new AiUsageEvent(userId, "FR-36", clock.instant(), result.usage());
            aiQuota.record(usageEvent);
            events.publishEvent(usageEvent);
            List<Long> rankedIds = parseRecipeIds(result.json(), ids);
            Map<Long, Candidate> candidateById = pool.stream()
                    .collect(Collectors.toMap(candidate -> candidate.recipe().id(), candidate -> candidate));
            Set<Long> rankedSet = Set.copyOf(rankedIds);
            List<Candidate> ranked = java.util.stream.Stream.concat(rankedIds.stream().map(candidateById::get),
                    pool.stream().filter(candidate -> !rankedSet.contains(candidate.recipe().id()))).toList();
            List<AiMenuOptimizer.Selection> selections = optimizer.optimize(request.weekStartDate(),
                    context.firstSuggestionDate(), context.lastSuggestionDate(), context.profile().energyKcal().doubleValue(), ranked.stream()
                            .map(candidate -> new AiMenuOptimizer.Candidate(candidate.recipe().id(), candidate.nutritionPerServing())).toList(),
                    context.profile().dailyTargets());
            return preview(request.weekStartDate(), request.mode(), context.goal(), selections, pool,
                    context.preferred(), context.profile());
        } catch (AiProviderException exception) {
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "Không thể tạo thực đơn lúc này. Vui lòng thử lại sau.");
        } catch (RuntimeException exception) {
            if (exception instanceof AppException appException) throw appException;
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID,
                    "AI chưa tạo được thực đơn hợp lệ. Vui lòng thử lại.");
        }
    }

    /** Trusted candidates for the Chatbox ADK planner. This method never calls an AI provider. */
    public Map<String, Object> getAdkPreviewCandidates(CreateAiMenuPreviewRequest request) {
        MenuContext context = resolveMenuContext(request);
        if (context.firstSuggestionDate().isAfter(context.lastSuggestionDate())) {
            return Map.of("canGenerate", false, "candidates", List.of(),
                    "firstDate", context.firstSuggestionDate(), "lastDate", context.lastSuggestionDate());
        }
        if (context.pool().isEmpty()) throw new AppException(ErrorCode.AI_RESPONSE_INVALID,
                "Chưa có đủ công thức công khai phù hợp và có dữ liệu dinh dưỡng để tạo thực đơn.");
        List<Map<String, Object>> trustedCandidates = context.pool().stream().map(candidate -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("recipeId", candidate.recipe().id());
            item.put("title", candidate.recipe().title());
            item.put("ingredients", candidate.recipe().ingredients().stream()
                    .map(RecipePostResponse.Ingredient::name).toList());
            // The ADK only ranks eligible candidates; the Backend optimizer uses the full trusted
            // nutrition map below when it constructs the preview. Avoid duplicating every nutrient
            // value for all candidates in the model conversation history.
            return item;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("canGenerate", true);
        result.put("firstDate", context.firstSuggestionDate());
        result.put("lastDate", context.lastSuggestionDate());
        result.put("goal", context.goal());
        result.put("vegetarianType", context.vegetarianType());
        result.put("preferredIngredientIds", context.preferred());
        result.put("avoidedIngredientIds", context.allAvoid());
        result.put("energyKcal", context.profile().energyKcal());
        result.put("candidates", trustedCandidates);
        return result;
    }

    /** Builds the existing business preview from ADK-ranked IDs after re-resolving trusted data. */
    public AiMenuPreviewResponse createPreviewFromAdkSelection(CreateAiMenuPreviewRequest request, List<Long> rankedRecipeIds) {
        MenuContext context = resolveMenuContext(request);
        if (context.firstSuggestionDate().isAfter(context.lastSuggestionDate())) {
            return preview(request.weekStartDate(), request.mode(), context.goal(), List.of(), List.of(),
                    context.preferred(), context.profile());
        }
        List<Long> allowedIds = context.pool().stream().map(candidate -> candidate.recipe().id()).toList();
        List<Long> rankedIds = normalizeAdkRankedIds(rankedRecipeIds, allowedIds);
        Map<Long, Candidate> candidateById = context.pool().stream()
                .collect(Collectors.toMap(candidate -> candidate.recipe().id(), candidate -> candidate));
        Set<Long> rankedSet = Set.copyOf(rankedIds);
        List<Candidate> ranked = java.util.stream.Stream.concat(rankedIds.stream().map(candidateById::get),
                context.pool().stream().filter(candidate -> !rankedSet.contains(candidate.recipe().id()))).toList();
        List<AiMenuOptimizer.Selection> selections = optimizer.optimize(request.weekStartDate(),
                context.firstSuggestionDate(), context.lastSuggestionDate(), context.profile().energyKcal().doubleValue(),
                ranked.stream().map(candidate -> new AiMenuOptimizer.Candidate(candidate.recipe().id(), candidate.nutritionPerServing())).toList(),
                context.profile().dailyTargets());
        return preview(request.weekStartDate(), request.mode(), context.goal(), selections, context.pool(),
                context.preferred(), context.profile());
    }

    private MenuContext resolveMenuContext(CreateAiMenuPreviewRequest request) {
        validateWeek(request.weekStartDate());
        long userId = users.requireActiveMember().id();
        var profile = nutritionProfiles.requireMenuNutritionContext(userId);
        LocalDate firstSuggestionDate = firstActionableDate(request.weekStartDate());
        LocalDate lastSuggestionDate = lastActionableDate(request.weekStartDate());
        var dietary = dietaryPreferences.menuConstraints(userId);
        String goal = "PROFILE".equals(request.mode()) ? profile.nutritionGoal() : request.goal();
        Set<Long> preferred = Set.copyOf(normalizedIds(request.preferredIngredientIds()));
        List<Long> weeklyAvoid = normalizedIds(request.avoidedIngredientIds());
        List<Long> allAvoid = java.util.stream.Stream.concat(dietary.avoidedIngredientIds().stream(), weeklyAvoid.stream())
                .distinct().toList();
        List<Candidate> pool = firstSuggestionDate.isAfter(lastSuggestionDate) ? List.of()
                : candidates(dietary.vegetarianType(), allAvoid, dietary.avoidedIngredientNames());
        return new MenuContext(profile, firstSuggestionDate, lastSuggestionDate, goal, preferred, allAvoid,
                dietary.vegetarianType(), pool);
    }

    private static List<Long> normalizeAdkRankedIds(List<Long> rankedIds, List<Long> allowedIds) {
        if (rankedIds == null || rankedIds.isEmpty() || rankedIds.size() > CANDIDATE_LIMIT
                || rankedIds.stream().anyMatch(id -> id == null || id < 1)
                || rankedIds.stream().distinct().count() != rankedIds.size()
                || rankedIds.stream().anyMatch(id -> !allowedIds.contains(id))) {
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID, "ADK trả về công thức ngoài danh sách được phép.");
        }
        return List.copyOf(rankedIds);
    }

    @Transactional
    public MealPlanWeekResponse apply(ApplyAiMenuRequest request) {
        validateWeek(request.weekStartDate());
        long userId = users.requireActiveMember().id();
        nutritionProfiles.requireMenuNutritionContext(userId);
        LocalDate firstReplaceDate = firstActionableDate(request.weekStartDate());
        LocalDate lastReplaceDate = lastActionableDate(request.weekStartDate());
        if (firstReplaceDate.isAfter(lastReplaceDate)) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND, "AI chỉ đề xuất lịch từ hôm nay đến hết tuần hiện tại.");
        }
        var dietary = dietaryPreferences.menuConstraints(userId);
        List<Long> weeklyAvoid = normalizedIds(request.avoidedIngredientIds());
        List<Long> avoid = java.util.stream.Stream.concat(dietary.avoidedIngredientIds().stream(), weeklyAvoid.stream())
                .distinct().toList();
        List<Candidate> pool = candidates(dietary.vegetarianType(), avoid, dietary.avoidedIngredientNames());
        Map<Long, Candidate> allowed = pool.stream().collect(Collectors.toMap(c -> c.recipe().id(), c -> c));
        validateSelectedMeals(request, allowed, firstReplaceDate, lastReplaceDate);

        // Lock every selected recipe before removing old entries. Any hidden/deleted recipe aborts and rolls back.
        request.meals().forEach(meal -> recipes.requirePublishedMealPlanRecipe(meal.recipeId()));
        plans.createWeekIfAbsent(userId, request.weekStartDate());
        var week = plans.lockWeek(userId, request.weekStartDate())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        entries.deleteAllByMealPlanIdAndMealDateGreaterThanEqual(week.getId(), firstReplaceDate);
        entries.flush();
        for (var meal : request.meals()) {
            var entry = new MealPlanEntryEntity();
            entry.setMealPlanId(week.getId());
            entry.setMealDate(meal.mealDate());
            entry.setMealType(meal.mealType());
            entry.setRecipeId(meal.recipeId());
            entry.setPlannedServings(java.math.BigDecimal.ONE);
            entries.save(entry);
        }
        entries.flush();
        plans.touchWeek(week.getId());
        return readService.getWeek(request.weekStartDate());
    }

    private List<Candidate> candidates(String vegetarianType, List<Long> avoidedIds, List<String> avoidedNames) {
        Map<Integer, UnitResponse> units = ingredientCatalog.findActiveUnits().stream()
                .collect(Collectors.toMap(UnitResponse::id, unit -> unit));
        Set<Long> avoided = Set.copyOf(avoidedIds);
        List<RecipePostResponse> source = recipes.searchPublished("", 0, CANDIDATE_LIMIT).items();
        List<Candidate> result = new ArrayList<>();
        Set<String> avoidNameKeys = avoidedNames.stream().map(AiMenuService::ingredientKey).collect(Collectors.toSet());
        for (RecipePostResponse recipe : source) {
            if (!compatible(vegetarianType, recipe.vegetarianType()) || violatesAvoid(recipe, avoided, avoidNameKeys)) continue;
            List<RecipeNutritionService.RecipeIngredient> lines = new ArrayList<>();
            boolean resolvable = true;
            for (var ingredient : recipe.ingredients()) {
                UnitResponse unit = units.get(ingredient.unitId());
                if (ingredient.ingredientId() == null || unit == null) { resolvable = false; break; }
                var base = "MASS".equals(unit.dimension().name()) ? unit.baseFactor()
                        : conversions.findActiveGramsPerUnit(ingredient.ingredientId(), ingredient.unitId()).orElse(null);
                lines.add(new RecipeNutritionService.RecipeIngredient(ingredient.ingredientId(), ingredient.unitId(),
                        unit.dimension().name(), base, ingredient.quantity(), ingredient.name()));
            }
            if (!resolvable || lines.isEmpty()) continue;
            var nutrition = recipeNutrition.calculate(lines, recipe.servings());
            if (nutrition.complete()) result.add(new Candidate(recipe, nutrition.perServing()));
        }
        return result;
    }

    private AiMenuPreviewResponse preview(LocalDate start, String mode, String goal,
            List<AiMenuOptimizer.Selection> selections, List<Candidate> pool, Set<Long> preferred,
            NutritionProfileService.MenuNutritionContext profile) {
        Map<Long, Candidate> byId = pool.stream().collect(Collectors.toMap(c -> c.recipe().id(), c -> c));
        List<AiMenuPreviewResponse.Meal> meals = new ArrayList<>();
        for (AiMenuOptimizer.Selection selection : selections) {
            Candidate candidate = byId.get(selection.recipeId());
            var recipe = candidate.recipe();
            boolean matches = recipe.ingredients().stream().anyMatch(ingredient -> preferred.contains(ingredient.ingredientId()));
            meals.add(new AiMenuPreviewResponse.Meal(selection.date(), selection.mealType(), recipe.id(), recipe.title(),
                    recipe.media().stream().filter(RecipePostResponse.Media::cover).map(RecipePostResponse.Media::url).findFirst().orElse(null),
                    java.math.BigDecimal.ONE, candidate.nutritionPerServing(),
                    matches ? "Có nguyên liệu bạn muốn ưu tiên." : "Được chọn từ công thức công khai phù hợp."));
        }
        List<AiMenuPreviewResponse.NutritionTarget> targets = profile.dailyTargets().stream()
                .map(target -> new AiMenuPreviewResponse.NutritionTarget(normalizedNutritionKey(target.key()),
                        target.label(), target.minimum(), target.maximum(), target.unit(), target.referenceType())).toList();
        return new AiMenuPreviewResponse(start, mode, goal, profile.energyKcal(), profile.bmi(), profile.bmiCategory(),
                targets, meals, DISCLAIMER);
    }

    private void validateSelectedMeals(ApplyAiMenuRequest request, Map<Long, Candidate> allowed,
            LocalDate firstReplaceDate, LocalDate lastReplaceDate) {
        Set<String> positions = new HashSet<>();
        Map<Integer, Set<Long>> sequence = new HashMap<>();
        for (var meal : request.meals()) {
            long offset = java.time.temporal.ChronoUnit.DAYS.between(request.weekStartDate(), meal.mealDate());
            if (offset < 0 || offset > 6 || meal.mealDate().isBefore(firstReplaceDate)
                    || meal.mealDate().isAfter(lastReplaceDate)
                    || !MEALS.contains(meal.mealType()) || !allowed.containsKey(meal.recipeId())) {
                throw new AppException(ErrorCode.RECIPE_NOT_FOUND,
                        "Một công thức không còn công khai, đủ dữ liệu dinh dưỡng hoặc không phù hợp ràng buộc.");
            }
            int slot = Math.toIntExact(offset * 3 + MEALS.indexOf(meal.mealType()));
            if (!positions.add(meal.mealDate() + ":" + meal.mealType() + ":" + meal.recipeId())) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Không thể thêm trùng một công thức trong cùng bữa.");
            }
            sequence.computeIfAbsent(slot, ignored -> new HashSet<>()).add(meal.recipeId());
        }
        for (int i = 1; i < 21; i++) {
            if (sequence.containsKey(i) && sequence.containsKey(i - 1)
                    && allowed.size() > 1 && sequence.get(i).stream().anyMatch(sequence.get(i - 1)::contains)) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Không thể lặp cùng món ở hai bữa liền kề.");
            }
        }
    }

    private List<Long> parseRecipeIds(String json, List<Long> allowed) {
        try {
            var node = objectMapper.readTree(json).get("recipeIds");
            if (node == null || !node.isArray() || node.isEmpty() || node.size() > CANDIDATE_LIMIT) throw new IllegalArgumentException();
            List<Long> ids = new ArrayList<>();
            for (var value : node) {
                if (!value.canConvertToLong() || value.asLong() < 1) throw new IllegalArgumentException();
                ids.add(value.asLong());
            }
            if (ids.stream().distinct().count() != ids.size() || ids.stream().anyMatch(id -> !allowed.contains(id))) {
                throw new IllegalArgumentException();
            }
            return ids;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private static boolean compatible(String requested, String recipe) {
        if (requested == null) return true;
        return switch (requested) {
            case "VEGAN" -> "VEGAN".equals(recipe);
            case "LACTO" -> List.of("VEGAN", "LACTO").contains(recipe);
            case "OVO" -> List.of("VEGAN", "OVO").contains(recipe);
            case "LACTO_OVO" -> List.of("VEGAN", "LACTO", "OVO", "LACTO_OVO").contains(recipe);
            default -> false;
        };
    }

    private static boolean violatesAvoid(RecipePostResponse recipe, Set<Long> avoided, Set<String> avoidedNames) {
        return recipe.ingredients().stream().anyMatch(ingredient -> avoided.contains(ingredient.ingredientId())
                || avoidedNames.contains(ingredientKey(ingredient.name())));
    }

    private static String ingredientKey(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFC)
                .replaceAll("[\\s\\u00A0]+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static List<Long> normalizedIds(List<Long> ids) {
        if (ids == null) return List.of();
        if (ids.stream().anyMatch(id -> id == null || id < 1)) throw new AppException(ErrorCode.VALIDATION_FAILED);
        return ids.stream().distinct().toList();
    }

    private static String prompt(LocalDate firstSuggestionDate, LocalDate lastSuggestionDate, String goal, String vegetarianType, Set<Long> preferred,
            List<Long> avoided, List<Candidate> candidates,
            NutritionProfileService.MenuNutritionContext profile) {
        String recipeData = candidates.stream().map(candidate -> "ID " + candidate.recipe().id() + ": "
                + candidate.recipe().title() + "; ingredients=" + candidate.recipe().ingredients().stream()
                        .map(RecipePostResponse.Ingredient::name).collect(Collectors.joining(", "))
                + "; per-serving nutrition=" + candidate.nutritionPerServing()).collect(Collectors.joining("\n"));
        String targets = profile.dailyTargets().stream().map(target -> target.key() + "=" + target.minimum()
                + (target.maximum() == null ? "" : ".." + target.maximum()) + " " + target.unit())
                .collect(Collectors.joining(", "));
        return "Rank the listed trusted vegetarian recipes for menu dates from " + firstSuggestionDate
                + " through " + lastSuggestionDate
                + ". Return JSON with recipeIds ordered from most suitable to least suitable, using each listed ID at most once. "
                + "The server will calculate exact nutrients and compose multiple one-serving recipes per meal; do not calculate or invent nutrition values. "
                + "Goal preference=" + (goal == null ? "null; choose a balanced varied menu" : goal)
                + "; BMI=" + profile.bmi() + " (" + profile.bmiCategory() + "); daily energy target=" + profile.energyKcal()
                + " kcal; daily nutrient references=" + targets + ". Use these only to personalize recipe suitability; do not infer calorie deficits/surpluses or treatment."
                + "; declared vegetarian pattern=" + vegetarianType + "; preferred ingredient IDs=" + preferred
                + "; forbidden ingredient IDs=" + avoided + ". Do not change the supplied target values, promise health outcomes, or invent recipes.\nCandidates:\n" + recipeData;
    }

    private static String normalizedNutritionKey(String key) {
        return switch (key) {
            case "protein" -> "PROTEIN_G";
            case "carbohydrate" -> "CARBOHYDRATE_G";
            case "fat" -> "TOTAL_FAT_G";
            case "fiber" -> "FIBER_G";
            case "calcium" -> "CALCIUM_MG";
            case "iron" -> "IRON_MG";
            case "vitaminB12" -> "VITAMIN_B12_MCG";
            case "zinc" -> "ZINC_MG";
            default -> key;
        };
    }

    private static void validateWeek(LocalDate start) {
        MealPlanReadService.validateWeekStart(start);
    }

    private LocalDate firstActionableDate(LocalDate weekStart) {
        LocalDate today = todayInVietnam();
        return today.isAfter(weekStart) ? today : weekStart;
    }

    private LocalDate lastActionableDate(LocalDate weekStart) {
        LocalDate currentWeekEnd = todayInVietnam().with(java.time.temporal.TemporalAdjusters.previousOrSame(
                java.time.DayOfWeek.MONDAY)).plusDays(6);
        LocalDate requestedWeekEnd = weekStart.plusDays(6);
        return requestedWeekEnd.isBefore(currentWeekEnd) ? requestedWeekEnd : currentWeekEnd;
    }

    private LocalDate todayInVietnam() {
        return LocalDate.now(clock.withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    private record Candidate(RecipePostResponse recipe, Map<String, java.math.BigDecimal> nutritionPerServing) { }
    private record MenuContext(NutritionProfileService.MenuNutritionContext profile, LocalDate firstSuggestionDate,
            LocalDate lastSuggestionDate, String goal, Set<Long> preferred, List<Long> allAvoid,
            String vegetarianType, List<Candidate> pool) { }
}
