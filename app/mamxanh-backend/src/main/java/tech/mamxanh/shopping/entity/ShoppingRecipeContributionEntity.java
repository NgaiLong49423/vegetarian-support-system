package tech.mamxanh.shopping.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/** Original quantity/unit at add time; never refreshed from an edited source recipe. */
@Entity @Table(name = "\"SHOPPING_RECIPE_CONTRIBUTION\"")
@Getter @Setter @NoArgsConstructor
public class ShoppingRecipeContributionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contribution_id") private Long id;
    @Column(name = "shopping_list_id", nullable = false) private Long shoppingListId;
    @Column(name = "recipe_id") private Long recipeId;
    @Column(name = "ingredient_id") private Long ingredientId;
    @Column(name = "unit_id", nullable = false) private Integer unitId;
    @Column(nullable = false, precision = 18, scale = 6) private BigDecimal quantity;
    @Column(name = "shopping_list_item_id") private Long shoppingListItemId;
    @Column(name = "source_id", nullable = false) private UUID sourceId;
    @Column(name = "source_kind", nullable = false, length = 16) private String sourceKind = "RECIPE";
    @Column(name = "expected_ingredient_count") private Integer expectedIngredientCount;
    @Column(name = "servings", precision = 4, scale = 1) private BigDecimal servings;
    @Column(name = "meal_plan_entry_id") private Long mealPlanEntryId;
    @Column(name = "meal_date") private LocalDate mealDate;
    @Column(name = "meal_type", length = 20) private String mealType;
}
