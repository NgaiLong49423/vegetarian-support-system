package tech.mamxanh.nutrition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "INGREDIENT_UNIT_CONVERSION")
public class IngredientUnitConversionEntity {
    @EmbeddedId private IngredientUnitConversionId id;
    @Column(name = "grams_per_unit", nullable = false, precision = 10, scale = 2) private BigDecimal gramsPerUnit;
    @Column(name = "is_approximate", nullable = false) private boolean approximate;
    @Column(name = "is_active", nullable = false) private boolean active;
    protected IngredientUnitConversionEntity() { }
    public IngredientUnitConversionEntity(IngredientUnitConversionId id, BigDecimal gramsPerUnit, boolean approximate) {
        this.id = id; this.gramsPerUnit = gramsPerUnit; this.approximate = approximate; this.active = true;
    }
    public IngredientUnitConversionId getId() { return id; }
    public BigDecimal getGramsPerUnit() { return gramsPerUnit; }
    public boolean isApproximate() { return approximate; }
    public boolean isActive() { return active; }
    public void update(BigDecimal gramsPerUnit, boolean approximate) { this.gramsPerUnit = gramsPerUnit; this.approximate = approximate; }
    public void setActive(boolean active) { this.active = active; }
}
