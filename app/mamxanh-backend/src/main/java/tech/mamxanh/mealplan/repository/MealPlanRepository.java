package tech.mamxanh.mealplan.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.mealplan.entity.MealPlanEntity;

public interface MealPlanRepository extends JpaRepository<MealPlanEntity, Long> {
    Optional<MealPlanEntity> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MealPlanEntity p where p.userId = :userId and p.weekStartDate = :weekStartDate")
    Optional<MealPlanEntity> lockWeek(@Param("userId") long userId, @Param("weekStartDate") LocalDate weekStartDate);

    /** Serialize creation of the same owner/week, including a key that does not exist yet. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO [MEAL_PLAN] (user_id, week_start_date)
            SELECT :userId, :weekStartDate
            WHERE NOT EXISTS (
                SELECT 1 FROM [MEAL_PLAN] WITH (UPDLOCK, HOLDLOCK)
                WHERE user_id = :userId AND week_start_date = :weekStartDate
            )
            """, nativeQuery = true)
    int createWeekIfAbsent(@Param("userId") long userId, @Param("weekStartDate") LocalDate weekStartDate);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE [MEAL_PLAN] SET updated_at = SYSUTCDATETIME() WHERE meal_plan_id = :planId", nativeQuery = true)
    int touchWeek(@Param("planId") long planId);
}
