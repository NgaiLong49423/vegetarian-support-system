package tech.mamxanh.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "\"INGREDIENT\"")
@Getter
@NoArgsConstructor
public class RecipeIngredientReferenceEntity {
    @Id
    @Column(name = "ingredient_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "ingredient_group", nullable = false, length = 100)
    private String ingredientGroup;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "nutrition_supported", nullable = false)
    private boolean nutritionSupported;

    @Column(name = "energy_kcal_100g") private BigDecimal energyKcalPer100g;
    @Column(name = "protein_g_100g") private BigDecimal proteinGramsPer100g;
    @Column(name = "carbohydrate_g_100g") private BigDecimal carbohydrateGramsPer100g;
    @Column(name = "total_fat_g_100g") private BigDecimal totalFatGramsPer100g;
    @Column(name = "fiber_g_100g") private BigDecimal fiberGramsPer100g;
    @Column(name = "calcium_mg_100g") private BigDecimal calciumMgPer100g;
    @Column(name = "iron_mg_100g") private BigDecimal ironMgPer100g;
    @Column(name = "vitamin_b12_mcg_100g") private BigDecimal vitaminB12MicrogramsPer100g;
    @Column(name = "zinc_mg_100g") private BigDecimal zincMgPer100g;
}
