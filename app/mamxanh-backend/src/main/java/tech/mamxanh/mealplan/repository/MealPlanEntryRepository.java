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
}
