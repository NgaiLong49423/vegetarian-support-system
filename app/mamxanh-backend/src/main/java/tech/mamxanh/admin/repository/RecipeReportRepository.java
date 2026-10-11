package tech.mamxanh.admin.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.admin.entity.RecipeReportEntity;

public interface RecipeReportRepository extends JpaRepository<RecipeReportEntity, Long> {
    Optional<RecipeReportEntity> findFirstByReporterIdAndRecipeIdAndStatusInOrderByIdAsc(
            long reporterId, long recipeId, List<String> statuses);
}
