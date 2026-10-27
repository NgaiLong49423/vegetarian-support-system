package tech.mamxanh.shopping.entity;

import java.math.BigDecimal;
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
    @Column(name = "recipe_id", nullable = false) private Long recipeId;
    @Column(name = "ingredient_id", nullable = false) private Long ingredientId;
    @Column(name = "unit_id", nullable = false) private Integer unitId;
    @Column(nullable = false, precision = 18, scale = 6) private BigDecimal quantity;
    @Column(name = "shopping_list_item_id") private Long shoppingListItemId;
}
