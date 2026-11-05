package tech.mamxanh.recipe.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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
