package tech.mamxanh.mealplan.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;

public interface MealPlanEntryRepository extends JpaRepository<MealPlanEntryEntity, Long> {
    List<MealPlanEntryEntity> findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(Long mealPlanId);
    Optional<MealPlanEntryEntity> findByIdAndMealPlanId(long id, long mealPlanId);
    boolean existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(
            long mealPlanId, LocalDate mealDate, String mealType, long recipeId, long excludedEntryId);

    @Modifying
    @Query("delete from MealPlanEntryEntity entry where entry.mealPlanId = :mealPlanId")
    int deleteAllByMealPlanId(@Param("mealPlanId") long mealPlanId);
}
