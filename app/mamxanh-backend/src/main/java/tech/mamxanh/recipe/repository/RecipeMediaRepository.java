package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.mamxanh.recipe.entity.RecipeMedia;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for RecipeMedia (FR-14).
 */
@Repository
public interface RecipeMediaRepository extends JpaRepository<RecipeMedia, Long> {

    List<RecipeMedia> findByRecipeIdOrderByDisplayOrderAsc(Long recipeId);

    Optional<RecipeMedia> findByRecipeIdAndIsCoverTrue(Long recipeId);

    long countByRecipeId(Long recipeId);

    void deleteByRecipeId(Long recipeId);
}
