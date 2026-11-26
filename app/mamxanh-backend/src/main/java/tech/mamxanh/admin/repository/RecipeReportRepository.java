package tech.mamxanh.admin.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.admin.entity.AppealStatus;
import tech.mamxanh.admin.entity.RecipeReportEntity;
import tech.mamxanh.admin.entity.ReportDecision;

import java.util.List;
import java.util.Optional;

public interface RecipeReportRepository extends JpaRepository<RecipeReportEntity, Long> {
    /** Row lock on a Recipe Post report, so concurrent changes to the same report run one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecipeReportEntity r where r.id = :id and r.recipeId is not null")
    Optional<RecipeReportEntity> lockRecipeReportById(@Param("id") long id);

    Optional<RecipeReportEntity> findFirstByReporterIdAndRecipeIdAndStatusInOrderByIdAsc(
            long reporterId, long recipeId, List<String> statuses);

    boolean existsByReporterIdAndRecipeIdAndStatus(long reporterId, long recipeId, String status);

    /** The latest hide decision on a Recipe Post; an appeal always targets this report (BR-27). */
    Optional<RecipeReportEntity> findFirstByRecipeIdAndDecisionOrderByHandledAtDescIdDesc(
            long recipeId, ReportDecision decision);

    boolean existsByRecipeIdAndDecisionAndAppealStatus(long recipeId, ReportDecision decision, AppealStatus appealStatus);
}
