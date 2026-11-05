package tech.mamxanh.recipe.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecipeConversionId implements Serializable {
    private Long ingredientId;
    private Integer unitId;
}
