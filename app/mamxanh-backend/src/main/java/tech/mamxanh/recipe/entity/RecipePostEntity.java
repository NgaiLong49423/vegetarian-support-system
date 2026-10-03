package tech.mamxanh.recipe.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RECIPE_POST")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipePostEntity {
    @Id
    @Column(name = "recipe_id")
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String instructions;

    @Column(name = "dish_category", nullable = false, length = 20)
    private String dishCategory;

    @Column(name = "vegetarian_type", nullable = false, length = 20)
    private String vegetarianType;

    @Column(nullable = false, length = 10)
    private String difficulty;

    @Column(nullable = false)
    private int servings;

    @Column(name = "prep_time_min", nullable = false)
    private int prepTimeMin;

    @Column(name = "cook_time_min", nullable = false)
    private int cookTimeMin;

    @Column(name = "youtube_url", length = 2048)
    private String youtubeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipePostStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "like_count", nullable = false, insertable = false)
    private int likeCount;

    @Column(name = "dislike_count", nullable = false, insertable = false)
    private int dislikeCount;

    @Column(name = "view_count", nullable = false, insertable = false)
    private int viewCount;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecipeMediaEntity> media = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecipeIngredientEntity> ingredients = new ArrayList<>();

    public void replaceMedia(List<RecipeMediaEntity> replacements) {
        media.clear();
        replacements.forEach(item -> {
            item.setRecipe(this);
            media.add(item);
        });
    }

    public void replaceIngredients(List<RecipeIngredientEntity> replacements) {
        ingredients.clear();
        replacements.forEach(item -> {
            item.setRecipe(this);
            ingredients.add(item);
        });
    }
}
