package tech.mamxanh.recipe.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecipeConversionId implements Serializable {
    private Long ingredientId;
    private Integer unitId;
}
