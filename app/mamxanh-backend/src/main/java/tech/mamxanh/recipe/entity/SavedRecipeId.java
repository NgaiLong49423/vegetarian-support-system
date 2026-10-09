package tech.mamxanh.recipe.entity;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SavedRecipeId implements Serializable {
    @Column(name = "user_id") private Long userId;
    @Column(name = "recipe_id") private Long recipeId;
    protected SavedRecipeId() { }
    public SavedRecipeId(Long userId, Long recipeId) { this.userId = userId; this.recipeId = recipeId; }
    public Long getUserId() { return userId; }
    public Long getRecipeId() { return recipeId; }
    @Override public boolean equals(Object value) {
        return value instanceof SavedRecipeId other && Objects.equals(userId, other.userId)
                && Objects.equals(recipeId, other.recipeId);
    }
    @Override public int hashCode() { return Objects.hash(userId, recipeId); }
}
