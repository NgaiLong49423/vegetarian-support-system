package tech.mamxanh.recipe.entity;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "INGREDIENT_UNIT_CONVERSION")
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
