package tech.mamxanh.nutrition.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"NUTRITION_DAILY_VALUE\"")
@Getter
@NoArgsConstructor
public class NutritionDailyValueEntity {
    @Id
    @Column(name = "nutrient_key", length = 32)
    private String nutrientKey;

    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Column(name = "reference_value", nullable = false, precision = 12, scale = 3)
    private BigDecimal referenceValue;

    @Column(name = "unit", nullable = false, length = 12)
    private String unit;

    @Column(name = "reference_type", nullable = false, length = 24)
    private String referenceType;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "version_label", nullable = false, length = 80)
    private String versionLabel;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
}
