package tech.mamxanh.recipe.entity;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"UNIT\"")
@Getter
@NoArgsConstructor
public class RecipeUnitReferenceEntity {
    @Id
    @Column(name = "unit_id")
    private Integer id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "dimension", nullable = false, length = 20)
    private String dimension;

    @Column(name = "base_factor", nullable = false, precision = 18, scale = 6)
    private BigDecimal baseFactor;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
