package tech.mamxanh.mealplan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "\"MEAL_PLAN_ENTRY\"")
@Getter
@Setter
@NoArgsConstructor
public class MealPlanEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_plan_entry_id")
    private Long id;

    @Column(name = "meal_plan_id", nullable = false)
    private Long mealPlanId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(name = "meal_type", nullable = false, length = 10)
    private String mealType;

    @Column(name = "planned_servings", nullable = false, precision = 5, scale = 1)
    private BigDecimal plannedServings;
}
