package tech.mamxanh.recipe.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"RECIPE_REACTION\"")
public class RecipeReactionEntity {
    @EmbeddedId private RecipeReactionId id;
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 10)
    private RecipeReactionType reactionType;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected RecipeReactionEntity() { }
    public RecipeReactionEntity(RecipeReactionId id, RecipeReactionType type, LocalDateTime now) {
        this.id = id;
        this.reactionType = type;
        this.createdAt = now;
        this.updatedAt = now;
    }
    public RecipeReactionId getId() { return id; }
    public RecipeReactionType getReactionType() { return reactionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void changeTo(RecipeReactionType type, LocalDateTime now) {
        if (reactionType != type) {
            reactionType = type;
            updatedAt = now;
        }
    }
}
