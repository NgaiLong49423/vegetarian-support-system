package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.mamxanh.recipe.entity.RecipeMediaEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for RecipeMediaEntity (FR-14, FR-25).
 */
@Repository
public interface RecipeMediaRepository extends JpaRepository<RecipeMediaEntity, Long> {

    List<RecipeMediaEntity> findByRecipeIdOrderByDisplayOrderAsc(Long recipeId);

    List<RecipeMediaEntity> findAllByRecipeIdOrderByDisplayOrderAsc(Long recipeId);

    Optional<RecipeMediaEntity> findByRecipeIdAndCoverTrue(Long recipeId);

    long countByRecipeId(Long recipeId);

    void deleteByRecipeId(Long recipeId);

    List<RecipeMediaEntity> findAllByRecipeIdInOrderByRecipeIdAscDisplayOrderAsc(java.util.Collection<Long> recipeIds);
}
