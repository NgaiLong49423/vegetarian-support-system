package tech.mamxanh.recipe.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"RECIPE_POST\"")
@Getter
@Setter
@NoArgsConstructor
public class RecipePostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "instructions", nullable = false)
    private String instructions;

    @Column(name = "dish_category", nullable = false, length = 20)
    private String dishCategory;

    @Column(name = "vegetarian_type", nullable = false, length = 20)
    private String vegetarianType;

    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty;

    @Column(name = "servings", nullable = false)
    private Integer servings;

    @Column(name = "prep_time_min", nullable = false)
    private Integer prepTimeMinutes;

    @Column(name = "cook_time_min", nullable = false)
    private Integer cookTimeMinutes;

    @Column(name = "youtube_url", length = 2048)
    private String youtubeUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
