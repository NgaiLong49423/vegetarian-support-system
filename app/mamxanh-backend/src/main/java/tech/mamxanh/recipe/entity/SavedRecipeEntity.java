package tech.mamxanh.recipe.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"SAVED_RECIPE\"")
public class SavedRecipeEntity {
    @EmbeddedId private SavedRecipeId id;
    @Column(name = "saved_at", nullable = false) private LocalDateTime savedAt;
    protected SavedRecipeEntity() { }
    public SavedRecipeId getId() { return id; }
    public LocalDateTime getSavedAt() { return savedAt; }
}
