package tech.mamxanh.recipe.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * FR-46 comment or reply on a Recipe Post ({@code COMMENT}). A reply copies its parent's recipe and
 * depth so {@code FK_COMMENT_PARENT} keeps the thread inside one recipe and {@code CK_COMMENT_depth}
 * caps it at five levels (BR-66). Deleting only sets {@code is_deleted}; timestamps are UTC.
 */
@Entity
@Table(name = "\"COMMENT\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeCommentEntity {
    public static final int MAX_DEPTH = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "parent_depth")
    private Integer parentDepth;

    @Nationalized
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static RecipeCommentEntity root(long recipeId, long userId, String content, LocalDateTime now) {
        RecipeCommentEntity comment = new RecipeCommentEntity();
        comment.recipeId = recipeId;
        comment.userId = userId;
        comment.content = content;
        comment.depth = 1;
        comment.createdAt = now;
        comment.updatedAt = now;
        return comment;
    }

    /** {@code parent} must be above the deepest level; the service attaches deeper replies one level up. */
    public static RecipeCommentEntity replyTo(RecipeCommentEntity parent, long userId, String content,
            LocalDateTime now) {
        RecipeCommentEntity comment = root(parent.recipeId, userId, content, now);
        comment.parentCommentId = parent.id;
        comment.parentDepth = parent.depth;
        comment.depth = parent.depth + 1;
        return comment;
    }

    public void edit(String content, LocalDateTime now) {
        this.content = content;
        this.updatedAt = now;
    }

    public void markDeleted(LocalDateTime now) {
        this.deleted = true;
        this.updatedAt = now;
    }

    /** AF-46.1 "(Đã chỉnh sửa)": the content changed after it was posted. */
    public boolean isEdited() {
        return !deleted && updatedAt.isAfter(createdAt);
    }
}
