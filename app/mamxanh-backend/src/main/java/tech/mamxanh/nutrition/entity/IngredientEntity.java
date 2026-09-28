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

@Entity
@Table(name = "INGREDIENT")
public class IngredientEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id") private Long id;
    @Column(nullable = false, length = 200) private String name;
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
