package tech.mamxanh.recipe.entity;

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
@Table(name = "\"RECIPE_MEDIA\"")
@Getter
@Setter
@NoArgsConstructor
public class RecipeMediaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "blob_url", nullable = false, length = 2048)
    private String blobUrl;

    @Column(name = "mime_type", nullable = false, length = 20)
    private String mimeType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_cover", nullable = false)
    private boolean cover;
}
