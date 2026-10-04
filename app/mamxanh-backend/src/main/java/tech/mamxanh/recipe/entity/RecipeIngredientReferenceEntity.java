package tech.mamxanh.recipe.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "INGREDIENT")
@Getter
@NoArgsConstructor
public class RecipeIngredientReferenceEntity {
    @Id
    @Column(name = "ingredient_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
