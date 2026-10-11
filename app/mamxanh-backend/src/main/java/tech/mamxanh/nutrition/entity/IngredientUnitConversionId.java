package tech.mamxanh.nutrition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class IngredientUnitConversionId implements Serializable {
    @Column(name = "ingredient_id") private Long ingredientId;
    @Column(name = "unit_id") private Integer unitId;
    protected IngredientUnitConversionId() { }
    public IngredientUnitConversionId(Long ingredientId, Integer unitId) { this.ingredientId = ingredientId; this.unitId = unitId; }
    public Long getIngredientId() { return ingredientId; }
    public Integer getUnitId() { return unitId; }
    @Override public boolean equals(Object other) { return other instanceof IngredientUnitConversionId that && Objects.equals(ingredientId, that.ingredientId) && Objects.equals(unitId, that.unitId); }
    @Override public int hashCode() { return Objects.hash(ingredientId, unitId); }
}
