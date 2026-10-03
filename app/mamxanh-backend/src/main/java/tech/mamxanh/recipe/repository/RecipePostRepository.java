package tech.mamxanh.recipe.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.entity.RecipePostStatus;

public interface RecipePostRepository extends JpaRepository<RecipePostEntity, Long> {
    @EntityGraph(attributePaths = "media")
    @Query("select r from RecipePostEntity r where r.id = :id")
    java.util.Optional<RecipePostEntity> findWithMediaById(@Param("id") long id);

    @EntityGraph(attributePaths = "ingredients")
    @Query("select r from RecipePostEntity r where r.id = :id")
    java.util.Optional<RecipePostEntity> findWithIngredientsById(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecipePostEntity r where r.id = :id")
    java.util.Optional<RecipePostEntity> lockById(@Param("id") long id);

    Page<RecipePostEntity> findAllByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            RecipePostStatus status, String title, Pageable pageable);
}
