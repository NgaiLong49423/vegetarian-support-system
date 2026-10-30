package tech.mamxanh.shopping.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"SHOPPING_LIST_ITEM\"")
@Getter @Setter @NoArgsConstructor
public class ShoppingListItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopping_list_item_id") private Long id;
    @Column(name = "shopping_list_id", nullable = false) private Long shoppingListId;
    @Column(name = "ingredient_id") private Long ingredientId;
    @Column(name = "unit_id", nullable = false) private Integer unitId;
    @Column(name = "custom_ingredient_name", length = 200) private String customIngredientName;
    @Column(nullable = false, precision = 18, scale = 6) private BigDecimal quantity;
    @Column(name = "is_bought", nullable = false) private boolean bought;
}
