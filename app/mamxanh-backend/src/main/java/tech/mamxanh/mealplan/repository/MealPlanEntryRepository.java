package tech.mamxanh.mealplan.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;

public interface MealPlanEntryRepository extends JpaRepository<MealPlanEntryEntity, Long> {
    List<MealPlanEntryEntity> findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(Long mealPlanId);
}
