package tech.mamxanh.mealplan.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Day;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Entry;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.MissingEntry;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Target;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse;
import tech.mamxanh.nutrition.repository.NutritionDailyValueRepository;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.recipe.service.RecipeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MealPlanNutritionService {
    public static final String DISCLAIMER = "Thông tin dinh dưỡng trong báo cáo chỉ mang tính tham khảo, được ước tính từ thực đơn dự kiến; không phải kết quả khám, chẩn đoán hay điều trị y tế, và không thay thế tư vấn của bác sĩ hoặc chuyên gia dinh dưỡng. Kết quả không có giá trị pháp lý hoặc y tế.";
    private static final List<String> KEYS = List.of("ENERGY_KCAL", "PROTEIN_G", "CARBOHYDRATE_G", "TOTAL_FAT_G",
            "FIBER_G", "CALCIUM_MG", "IRON_MG", "VITAMIN_B12_MCG", "ZINC_MG");
    private final CurrentUserService users;
    private final NutritionProfileService profiles;
    private final MealPlanRepository plans;
    private final MealPlanEntryRepository entries;
    private final NutritionDailyValueRepository dailyValues;
    private final RecipeService recipes;

    public MealPlanNutritionService(CurrentUserService users, NutritionProfileService profiles,
            MealPlanRepository plans, MealPlanEntryRepository entries, NutritionDailyValueRepository dailyValues,
            RecipeService recipes) {
        this.users = users;
        this.profiles = profiles;
        this.plans = plans;
        this.entries = entries;
        this.dailyValues = dailyValues;
        this.recipes = recipes;
    }

    @Transactional(readOnly = true)
    public MealPlanNutritionResponse analyze(LocalDate weekStart, LocalDate date) {
        MealPlanReadService.validateWeekStart(weekStart);
        if (date != null && (date.isBefore(weekStart) || date.isAfter(weekStart.plusDays(6)))) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ngày phải nằm trong tuần được chọn.");
        }
        long owner = users.requireActiveMember().id();
        profiles.requireEligible(owner);
        var plan = plans.findByUserIdAndWeekStartDate(owner, weekStart);
        List<MealPlanEntryEntity> stored = plan.map(p -> entries.findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(p.getId()))
                .orElse(List.of());
        List<MealPlanEntryEntity> selected = stored.stream().filter(e -> date == null || date.equals(e.getMealDate())).toList();
        Map<Long, BigDecimal> servingByEntry = selected.stream().collect(Collectors.toMap(
                MealPlanEntryEntity::getId, MealPlanEntryEntity::getPlannedServings));
        Map<Long, RecipeService.PlannedNutrition> nutrition = recipes.calculatePlannedNutritionByEntry(
                selected.stream().collect(Collectors.toMap(MealPlanEntryEntity::getId, MealPlanEntryEntity::getRecipeId)),
                servingByEntry);
        var personalized = profiles.targetsIfProfileExists(owner);
        var targets = targets(personalized);
        List<Day> days = new ArrayList<>();
        int count = date == null ? 7 : 1;
        for (int i = 0; i < count; i++) {
            LocalDate current = (date == null ? weekStart : date).plusDays(i);
            List<MealPlanEntryEntity> dayEntries = selected.stream().filter(e -> e.getMealDate().equals(current)).toList();
            List<Entry> detailed = dayEntries.stream().map(entry -> {
                var value = nutrition.get(entry.getId());
                return value == null ? new Entry(entry.getId(), current, entry.getMealType(), entry.getRecipeId(),
                        "Công thức không còn khả dụng", entry.getPlannedServings(), false, List.of("Công thức không còn khả dụng"), Map.of())
                        : new Entry(entry.getId(), current, entry.getMealType(), entry.getRecipeId(), value.title(),
                                entry.getPlannedServings(), value.complete(), value.ingredientsMissingData(), value.nutrients());
            }).toList();
            Map<String, BigDecimal> totals = sum(detailed);
            days.add(new Day(current, totals, detailed));
        }
        List<MissingEntry> missing = days.stream().flatMap(day -> day.entries().stream())
                .filter(entry -> !entry.complete()).map(entry -> new MissingEntry(entry.entryId(), entry.date(), entry.mealType(),
                        entry.recipeTitle(), entry.ingredientsMissingData())).toList();
        String basis = personalized.isPresent()
                ? "Mục tiêu tham khảo cá nhân từ hồ sơ đã lưu" : "Giá trị tham khảo chung FDA Daily Values; năng lượng theo hướng dẫn FDA lưu trong danh mục";
        boolean personal = personalized.isPresent();
        Map<String, BigDecimal> weeklyAverage = average(days);
        Map<String, Integer> attainmentDays = attainment(days, targets);
        return new MealPlanNutritionResponse(weekStart, date == null ? "WEEK" : "DAY", date, basis,
                personal ? "USDA/National Academies DRI and NIH ODS" : "U.S. FDA",
                personal ? "https://ods.od.nih.gov/HealthInformation/nutrientrecommendations/"
                        : "https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels",
                DISCLAIMER, targets, days, weeklyAverage, attainmentDays, missing);
    }

    private List<Target> targets(java.util.Optional<NutritionProfileResponse.Results> personalized) {
        if (personalized.isPresent()) {
            NutritionProfileResponse.Results results = personalized.get();
            Map<String, NutritionProfileResponse.Target> byKey = results.dailyTargets().stream()
                    .collect(Collectors.toMap(t -> normalize(t.key()), t -> t));
            Map<String, NutritionProfileResponse.Target> mapped = new LinkedHashMap<>(byKey);
            mapped.put("ENERGY_KCAL", new NutritionProfileResponse.Target("ENERGY_KCAL", "Năng lượng",
                    results.energyKcal(), results.energyKcal(), "kcal", "ESTIMATED_ENERGY", results.energySource()));
            return KEYS.stream().map(key -> {
                var target = mapped.get(key);
                return target == null ? new Target(key, label(key), null, null, unit(key), "NOT_AVAILABLE", "", "", "")
                    : new Target(key, target.label(), target.minimum(), target.maximum(), target.unit(), target.referenceType(),
                                target.source(), target.source(), "Personal profile calculation");
            }).toList();
        }
        return dailyValues.findAllById(KEYS).stream().map(value -> new Target(value.getNutrientKey(), value.getLabel(),
                value.getReferenceValue(), value.getReferenceValue(), value.getUnit(), value.getReferenceType(),
                value.getSourceName(), value.getSourceUrl(), value.getVersionLabel())).toList();
    }

    private static String normalize(String key) {
        return switch (key) {
            case "protein" -> "PROTEIN_G"; case "carbohydrate" -> "CARBOHYDRATE_G"; case "fat" -> "TOTAL_FAT_G";
            case "fiber" -> "FIBER_G"; case "calcium" -> "CALCIUM_MG"; case "iron" -> "IRON_MG";
            case "vitaminB12" -> "VITAMIN_B12_MCG"; case "zinc" -> "ZINC_MG"; default -> key;
        };
    }

    private static Map<String, BigDecimal> sum(List<Entry> entries) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String key : KEYS) {
            if (entries.isEmpty()) {
                totals.put(key, BigDecimal.ZERO.setScale(2));
                continue;
            }
            List<BigDecimal> knownValues = entries.stream().map(entry -> entry.nutrients().get(key))
                    .filter(java.util.Objects::nonNull).toList();
            totals.put(key, knownValues.isEmpty() ? null : knownValues.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP));
        }
        return totals;
    }
    private static Map<String, BigDecimal> average(List<Day> days) {
        if (days.size() != 7) return Map.of();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String key : KEYS) {
            List<BigDecimal> values = days.stream().map(day -> day.nutrients().get(key)).toList();
            result.put(key, values.stream().anyMatch(java.util.Objects::isNull) ? null
                    : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP));
        }
        return result;
    }
    private static Map<String, Integer> attainment(List<Day> days, List<Target> targets) {
        if (days.size() != 7) return Map.of();
        Map<String, Target> byKey = targets.stream().collect(Collectors.toMap(Target::key, target -> target));
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String key : KEYS) {
            Target target = byKey.get(key);
            int passed = target == null || target.minimum() == null ? 0 : (int) days.stream()
                    .map(day -> day.nutrients().get(key)).filter(java.util.Objects::nonNull)
                    .filter(value -> "AMDR".equals(target.referenceType())
                            ? value.compareTo(target.minimum()) >= 0 && target.maximum() != null && value.compareTo(target.maximum()) <= 0
                            : value.compareTo(target.minimum()) >= 0).count();
            result.put(key, passed);
        }
        return result;
    }
    private static String label(String key) { return key; }
    private static String unit(String key) { return key.endsWith("_MG") ? "mg" : key.endsWith("_MCG") ? "mcg" : key.endsWith("_G") ? "g" : "kcal"; }
}
