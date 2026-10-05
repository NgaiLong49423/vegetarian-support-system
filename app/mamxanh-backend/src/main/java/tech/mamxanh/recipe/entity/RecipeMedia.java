package tech.mamxanh.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping for RECIPE_MEDIA table (FR-14, BR-11, BR-19, BR-20).
 * Stores Blob URL, MIME type, display order, and cover flag.
 */
@Entity
@Table(name = "RECIPE_MEDIA")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeMedia {

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
    private Boolean isCover = false;

    public RecipeMedia(Long recipeId, String blobUrl, String mimeType, Integer displayOrder, Boolean isCover) {
        this.recipeId = recipeId;
        this.blobUrl = blobUrl;
        this.mimeType = mimeType;
        this.displayOrder = displayOrder;
        this.isCover = isCover != null && isCover;
    }
}
