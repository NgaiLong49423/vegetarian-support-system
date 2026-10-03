package tech.mamxanh.recipe.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RECIPE_INGREDIENT")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_ingredient_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private RecipePostEntity recipe;

    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Column(name = "unit_id", nullable = false)
    private int unitId;

    @Column(name = "custom_ingredient_name", length = 200)
    private String customName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    public RecipeIngredientEntity(Long ingredientId, int unitId, String customName, BigDecimal quantity) {
        this.ingredientId = ingredientId;
        this.unitId = unitId;
        this.customName = customName;
        this.quantity = quantity;
    }
}
