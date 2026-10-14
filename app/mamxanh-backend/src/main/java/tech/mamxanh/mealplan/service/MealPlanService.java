package tech.mamxanh.mealplan.service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.entity.MealPlanEntity;
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.MoveMealPlanEntryRequest;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.recipe.service.AiRecipeRecommendationService;
import tech.mamxanh.mealplan.dto.request.ApplyAiWeeklyMenuRequest;

@Service
public class MealPlanService {
    private final MealPlanRepository repository;
    private final CurrentUserService currentUserService;
    private final MealPlanReadService readService;
    private final MealPlanEntryRepository entries;
    private final RecipePostService recipes;
    private final AiRecipeRecommendationService aiRecommendations;

    public MealPlanService(MealPlanRepository repository, CurrentUserService currentUserService,
            MealPlanReadService readService, MealPlanEntryRepository entries, RecipePostService recipes,
            AiRecipeRecommendationService aiRecommendations) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.readService = readService;
        this.entries = entries;
        this.recipes = recipes;
        this.aiRecommendations = aiRecommendations;
    }

    @Transactional
    public MealPlanWeekResponse applyAiMenu(LocalDate weekStartDate, ApplyAiWeeklyMenuRequest request) {
        long owner = currentUserService.requireActiveMember().id();
        MealPlanReadService.validateWeekStart(weekStartDate);
        if (request == null || request.slots() == null || request.slots().size() != 21) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        java.util.Set<String> positions = new java.util.HashSet<>();
        for (var slot : request.slots()) {
            if (slot.dayOffset() < 0 || slot.dayOffset() > 6
                    || !List.of("BREAKFAST", "LUNCH", "DINNER").contains(slot.mealType())
                    || !positions.add(slot.dayOffset() + ":" + slot.mealType())) {
                throw new AppException(ErrorCode.VALIDATION_FAILED);
            }
        }
        if (positions.size() != 21) throw new AppException(ErrorCode.VALIDATION_FAILED);

        repository.createWeekIfAbsent(owner, weekStartDate);
        var plan = requireWeek(owner, weekStartDate);
        List<Long> recipeIds = request.slots().stream().map(ApplyAiWeeklyMenuRequest.Slot::recipeId).toList();
        for (Long recipeId : recipeIds.stream().distinct().sorted().toList()) {
            recipes.requirePublishedMealPlanRecipe(recipeId);
        }
        aiRecommendations.requireWeeklyApplyEligible(owner, recipeIds);
        Map<Integer, Long> recipeByPosition = request.slots().stream().collect(java.util.stream.Collectors.toMap(
                slot -> slot.dayOffset() * 3 + List.of("BREAKFAST", "LUNCH", "DINNER").indexOf(slot.mealType()),
                ApplyAiWeeklyMenuRequest.Slot::recipeId));
        for (int position = 1; position < 21; position++) {
            if (recipeByPosition.get(position).equals(recipeByPosition.get(position - 1))) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Hai bữa liền nhau không thể dùng cùng một công thức.");
            }
        }
        for (var slot : request.slots()) {
            LocalDate date = weekStartDate.plusDays(slot.dayOffset());
            if (entries.existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(
                    plan.getId(), date, slot.mealType(), slot.recipeId(), 0)) {
                throw new AppException(ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE);
            }
        }

        List<MealPlanEntryEntity> additions = request.slots().stream().map(slot -> {
            var entry = new MealPlanEntryEntity();
            entry.setMealPlanId(plan.getId());
            entry.setMealDate(weekStartDate.plusDays(slot.dayOffset()));
            entry.setMealType(slot.mealType());
            entry.setRecipeId(slot.recipeId());
            entry.setPlannedServings(BigDecimal.ONE);
            return entry;
        }).toList();
        entries.saveAllAndFlush(additions);
        repository.touchWeek(plan.getId());
        return readService.getWeek(weekStartDate);
    }

    @Transactional
    public MealPlanWeekResponse openWeek(LocalDate weekStartDate) {
        var owner = currentUserService.requireActiveMember();
        MealPlanReadService.validateWeekStart(weekStartDate);
        repository.createWeekIfAbsent(owner.id(), weekStartDate);
        return readService.getWeek(weekStartDate);
    }

    @Transactional
    public MealPlanWeekResponse.Entry addEntry(LocalDate weekStartDate, AddMealPlanEntryRequest request) {
        long owner = currentUserService.requireActiveMember().id();
        MealPlanReadService.validateWeekStart(weekStartDate);
        if (request.mealDate().isBefore(weekStartDate) || request.mealDate().isAfter(weekStartDate.plusDays(6))) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ngày ăn phải thuộc tuần được chọn.");
        }
        repository.createWeekIfAbsent(owner, weekStartDate);
        var plan = requireWeek(owner, weekStartDate);
        rejectDuplicate(plan.getId(), request.mealDate(), request.mealType(), request.recipeId(), 0);
        recipes.requirePublishedMealPlanRecipe(request.recipeId());
        var entry = new MealPlanEntryEntity();
        entry.setMealPlanId(plan.getId());
        entry.setMealDate(request.mealDate());
        entry.setMealType(request.mealType());
        entry.setRecipeId(request.recipeId());
        entry.setPlannedServings(BigDecimal.ONE);
        var saved = entries.saveAndFlush(entry);
        repository.touchWeek(plan.getId());
        return response(saved);
    }

    @Transactional
    public MealPlanWeekResponse.Entry replaceEntry(LocalDate weekStartDate, long entryId, long recipeId) {
        var plan = ownedWeek(weekStartDate);
        var entry = requireEntry(plan.getId(), entryId);
        rejectDuplicate(plan.getId(), entry.getMealDate(), entry.getMealType(), recipeId, entryId);
        recipes.requirePublishedMealPlanRecipe(recipeId);
        entry.setRecipeId(recipeId);
        var saved = entries.saveAndFlush(entry);
        repository.touchWeek(plan.getId());
        return response(saved);
    }

    @Transactional
    public MealPlanWeekResponse.Entry moveEntry(LocalDate weekStartDate, long entryId, MoveMealPlanEntryRequest request) {
        var plan = ownedWeek(weekStartDate);
        var entry = requireEntry(plan.getId(), entryId);
        if (request.mealDate() == null || request.mealDate().isBefore(weekStartDate)
                || request.mealDate().isAfter(weekStartDate.plusDays(6))
                || request.mealType() == null
                || !List.of("BREAKFAST", "LUNCH", "DINNER").contains(request.mealType())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ chuyển món giữa ba bữa trong cùng tuần.");
        }
        rejectDuplicate(plan.getId(), request.mealDate(), request.mealType(), entry.getRecipeId(), entryId);
        entry.setMealDate(request.mealDate());
        entry.setMealType(request.mealType());
        var saved = entries.saveAndFlush(entry);
        repository.touchWeek(plan.getId());
        return response(saved);
    }

    @Transactional
    public MealPlanWeekResponse.Entry updateServings(LocalDate weekStartDate, long entryId, BigDecimal plannedServings) {
        var plan = ownedWeek(weekStartDate);
        var entry = requireEntry(plan.getId(), entryId);
        if (plannedServings == null || plannedServings.compareTo(BigDecimal.ONE) < 0
                || plannedServings.compareTo(new BigDecimal("9999")) > 0
                || plannedServings.stripTrailingZeros().scale() > 0) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Số phần cần nấu phải là số nguyên từ 1 đến 9.999.");
        }
        entry.setPlannedServings(plannedServings);
        var saved = entries.saveAndFlush(entry);
        repository.touchWeek(plan.getId());
        return response(saved);
    }

    @Transactional
    public void removeEntry(LocalDate weekStartDate, long entryId) {
        var plan = ownedWeek(weekStartDate);
        entries.delete(requireEntry(plan.getId(), entryId));
        entries.flush();
        repository.touchWeek(plan.getId());
    }

    private MealPlanEntity ownedWeek(LocalDate weekStartDate) {
        long owner = currentUserService.requireActiveMember().id();
        MealPlanReadService.validateWeekStart(weekStartDate);
        return repository.lockWeek(owner, weekStartDate).orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private MealPlanEntity requireWeek(long owner, LocalDate weekStartDate) {
        return repository.lockWeek(owner, weekStartDate).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private MealPlanEntryEntity requireEntry(long planId, long entryId) {
        // Return the same 403 for a missing or foreign entry; do not disclose another owner's data.
        return entries.findByIdAndMealPlanId(entryId, planId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private void rejectDuplicate(long planId, LocalDate date, String mealType, long recipeId, long excludedId) {
        if (entries.existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(planId, date, mealType, recipeId, excludedId)) {
            throw new AppException(ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE);
        }
    }

    private MealPlanWeekResponse.Entry response(MealPlanEntryEntity entry) {
        var recipe = recipes.findMealPlanReferences(List.of(entry.getRecipeId())).get(entry.getRecipeId());
        boolean available = recipe != null && "PUBLISHED".equals(recipe.status());
        boolean deleted = recipe != null && "DELETED".equals(recipe.status());
        return new MealPlanWeekResponse.Entry(entry.getId(), entry.getMealDate(), entry.getMealType(),
                entry.getPlannedServings(), entry.getRecipeId(), available ? recipe.title() : null, available ? recipe.coverUrl() : null,
                available ? recipe.dishCategory() : null, available ? recipe.totalTimeMinutes() : null, deleted,
                available ? null : "Công thức không còn khả dụng");
    }
}
