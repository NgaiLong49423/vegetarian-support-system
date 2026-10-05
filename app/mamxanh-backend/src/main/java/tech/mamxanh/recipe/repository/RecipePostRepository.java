package tech.mamxanh.recipe.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import tech.mamxanh.recipe.entity.RecipePostEntity;

import java.util.List;

public interface RecipePostRepository extends JpaRepository<RecipePostEntity, Long> {
    java.util.Optional<RecipePostEntity> findByIdAndStatus(Long id, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecipePostEntity r where r.id = :id")
    java.util.Optional<RecipePostEntity> lockById(@Param("id") long id);

    Page<RecipePostEntity> findAllByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            String status, String title, Pageable pageable);

    Page<RecipePostEntity> findAllByAuthorIdAndStatusInOrderByUpdatedAtDesc(
            Long authorId, List<String> statuses, Pageable pageable);
}
