package tech.mamxanh.recipe.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "\"INGREDIENT_UNIT_CONVERSION\"")
@IdClass(RecipeConversionId.class)
@Getter
@NoArgsConstructor
public class RecipeConversionEntity {
    @Id
    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Id
    @Column(name = "unit_id")
    private Integer unitId;

    @Column(name = "grams_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal gramsPerUnit;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
