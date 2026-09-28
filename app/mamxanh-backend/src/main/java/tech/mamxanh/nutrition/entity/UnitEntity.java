package tech.mamxanh.nutrition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "UNIT")
public class UnitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id") private Integer id;
    @Column(nullable = false, length = 20) private String code;
    @Column(nullable = false, length = 50) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private MeasurementDimension dimension;
    @Column(name = "base_factor", nullable = false, precision = 18, scale = 6) private BigDecimal baseFactor;
    @Column(name = "is_active", nullable = false) private boolean active;
    protected UnitEntity() { }
    public UnitEntity(String code, String name, MeasurementDimension dimension, BigDecimal baseFactor) {
        this.code = code; this.name = name; this.dimension = dimension; this.baseFactor = baseFactor; this.active = true;
    }
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public MeasurementDimension getDimension() { return dimension; }
    public BigDecimal getBaseFactor() { return baseFactor; }
    public boolean isActive() { return active; }
    public void update(String code, String name, MeasurementDimension dimension, BigDecimal baseFactor) {
        this.code = code; this.name = name; this.dimension = dimension; this.baseFactor = baseFactor;
    }
    public void setActive(boolean active) { this.active = active; }
}
