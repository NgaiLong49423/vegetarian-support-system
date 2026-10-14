package tech.mamxanh.mealplan.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;

public interface MealPlanEntryRepository extends JpaRepository<MealPlanEntryEntity, Long> {
    List<MealPlanEntryEntity> findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(Long mealPlanId);
    Optional<MealPlanEntryEntity> findByIdAndMealPlanId(long id, long mealPlanId);
    boolean existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(
            long mealPlanId, LocalDate mealDate, String mealType, long recipeId, long excludedEntryId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT TOP (20) r.recipe_id FROM [RECIPE_POST] r "
            + "WHERE r.status = 'PUBLISHED' AND r.recipe_id <> :excludedRecipeId ORDER BY r.published_at DESC, r.recipe_id DESC", nativeQuery = true)
    List<Long> findPublishedCandidateIds(long excludedRecipeId);
}
