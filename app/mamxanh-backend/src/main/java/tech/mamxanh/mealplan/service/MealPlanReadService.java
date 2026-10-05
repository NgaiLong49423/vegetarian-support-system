package tech.mamxanh.mealplan.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.recipe.service.RecipePostService.MealPlanRecipeReference;

@Service
public class MealPlanReadService {
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanEntryRepository entryRepository;
    private final CurrentUserService currentUserService;
    private final RecipePostService recipePostService;

    public MealPlanReadService(MealPlanRepository mealPlanRepository, MealPlanEntryRepository entryRepository,
            CurrentUserService currentUserService, RecipePostService recipePostService) {
        this.mealPlanRepository = mealPlanRepository;
        this.entryRepository = entryRepository;
        this.currentUserService = currentUserService;
        this.recipePostService = recipePostService;
    }

    @Transactional(readOnly = true)
    public MealPlanWeekResponse getWeek(LocalDate weekStartDate) {
        var currentUser = currentUserService.requireActiveMember();
        if (weekStartDate == null || !weekStartDate.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ngày bắt đầu tuần phải là thứ Hai.");
        }
        var plan = mealPlanRepository.findByUserIdAndWeekStartDate(currentUser.id(), weekStartDate);
        if (plan.isEmpty()) return new MealPlanWeekResponse(weekStartDate, List.of());

        List<MealPlanEntryEntity> entries = entryRepository.findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(plan.get().getId());
        Map<Long, MealPlanRecipeReference> recipes = recipePostService.findMealPlanReferences(
                entries.stream().map(MealPlanEntryEntity::getRecipeId).distinct().toList());
        List<MealPlanWeekResponse.Entry> response = entries.stream().map(entry -> {
            MealPlanRecipeReference recipe = recipes.get(entry.getRecipeId());
            boolean deleted = recipe != null && "DELETED".equals(recipe.status());
            boolean available = recipe != null && "PUBLISHED".equals(recipe.status());
            return new MealPlanWeekResponse.Entry(entry.getId(), entry.getMealDate(), entry.getMealType(),
                    entry.getPlannedServings(), entry.getRecipeId(), available ? recipe.title() : null,
                    available ? recipe.coverUrl() : null, available ? recipe.dishCategory() : null,
                    available ? recipe.totalTimeMinutes() : null, deleted,
                    deleted ? "Công thức này đã bị xóa bởi tác giả"
                            : available ? null : "Công thức này hiện không còn khả dụng.");
        }).toList();
        return new MealPlanWeekResponse(weekStartDate, response);
    }
}
