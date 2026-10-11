package tech.mamxanh.mealplan.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.mealplan.entity.MealPlanEntity;

public interface MealPlanRepository extends JpaRepository<MealPlanEntity, Long> {
    Optional<MealPlanEntity> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
}
