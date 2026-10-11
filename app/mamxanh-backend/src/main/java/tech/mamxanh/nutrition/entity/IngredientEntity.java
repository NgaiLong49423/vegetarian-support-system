package tech.mamxanh.nutrition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "INGREDIENT")
public class IngredientEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id") private Long id;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "energy_kcal_100g", precision = 10, scale = 2) private BigDecimal energyKcalPer100g;
    @Column(name = "protein_g_100g", precision = 10, scale = 2) private BigDecimal proteinGramsPer100g;
    @Column(name = "carbohydrate_g_100g", precision = 10, scale = 2) private BigDecimal carbohydrateGramsPer100g;
    @Column(name = "total_fat_g_100g", precision = 10, scale = 2) private BigDecimal totalFatGramsPer100g;
    @Column(name = "fiber_g_100g", precision = 10, scale = 2) private BigDecimal fiberGramsPer100g;
    @Column(name = "calcium_mg_100g", precision = 10, scale = 2) private BigDecimal calciumMgPer100g;
    @Column(name = "iron_mg_100g", precision = 10, scale = 2) private BigDecimal ironMgPer100g;
    @Column(name = "vitamin_b12_mcg_100g", precision = 10, scale = 4) private BigDecimal vitaminB12MicrogramsPer100g;
    @Column(name = "zinc_mg_100g", precision = 10, scale = 2) private BigDecimal zincMgPer100g;
    @Column(name = "ingredient_group", nullable = false, length = 100) private String ingredientGroup;
    @Column(name = "source_name", nullable = false, length = 200) private String sourceName;
    @Column(name = "source_url", length = 2048) private String sourceUrl;
    @Column(name = "reference_date", nullable = false) private LocalDate referenceDate;
    @Column(name = "nutrition_supported", nullable = false) private boolean nutritionSupported;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CatalogStatus status;

    protected IngredientEntity() { }

    public IngredientEntity(String name, String ingredientGroup, String sourceName, String sourceUrl, LocalDate referenceDate) {
        this.name = name;
        this.ingredientGroup = ingredientGroup;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.referenceDate = referenceDate;
        this.nutritionSupported = false;
        this.status = CatalogStatus.ACTIVE;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getEnergyKcalPer100g() { return energyKcalPer100g; }
    public BigDecimal getProteinGramsPer100g() { return proteinGramsPer100g; }
    public BigDecimal getCarbohydrateGramsPer100g() { return carbohydrateGramsPer100g; }
    public BigDecimal getTotalFatGramsPer100g() { return totalFatGramsPer100g; }
    public BigDecimal getFiberGramsPer100g() { return fiberGramsPer100g; }
    public BigDecimal getCalciumMgPer100g() { return calciumMgPer100g; }
    public BigDecimal getIronMgPer100g() { return ironMgPer100g; }
    public BigDecimal getVitaminB12MicrogramsPer100g() { return vitaminB12MicrogramsPer100g; }
    public BigDecimal getZincMgPer100g() { return zincMgPer100g; }
    public String getIngredientGroup() { return ingredientGroup; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public boolean isNutritionSupported() { return nutritionSupported; }
    public CatalogStatus getStatus() { return status; }
    public void update(String name, String group, String sourceName, String sourceUrl, LocalDate referenceDate) {
        this.name = name; this.ingredientGroup = group; this.sourceName = sourceName;
        this.sourceUrl = sourceUrl; this.referenceDate = referenceDate;
    }
    public void setActive(boolean active) { this.status = active ? CatalogStatus.ACTIVE : CatalogStatus.INACTIVE; }
}
