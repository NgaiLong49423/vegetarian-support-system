package tech.mamxanh.nutrition.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.nutrition.entity.CatalogStatus;
import tech.mamxanh.nutrition.entity.IngredientEntity;

public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {
    boolean existsByNameIgnoreCaseAndStatus(String name, CatalogStatus status);
    boolean existsByNameIgnoreCaseAndStatusAndIdNot(String name, CatalogStatus status, Long id);
    List<IngredientEntity> findByStatus(CatalogStatus status);
    List<IngredientEntity> findByIdInAndStatus(Collection<Long> ids, CatalogStatus status);
    List<IngredientEntity> findByNameContainingIgnoreCaseOrIngredientGroupContainingIgnoreCase(String name, String group);

    @Query("select distinct ingredient.ingredientGroup from IngredientEntity ingredient order by ingredient.ingredientGroup")
    List<String> findDistinctIngredientGroups();

    @Query("""
            select ingredient from IngredientEntity ingredient
            where (:query is null or lower(ingredient.name) like lower(concat('%', :query, '%'))
                or lower(ingredient.ingredientGroup) like lower(concat('%', :query, '%')))
              and (:ingredientGroup is null or lower(ingredient.ingredientGroup) = lower(:ingredientGroup))
              and (:status is null or ingredient.status = :status)
              and (:nutritionSupported is null or ingredient.nutritionSupported = :nutritionSupported)
            """)
    Page<IngredientEntity> search(@Param("query") String query,
            @Param("ingredientGroup") String ingredientGroup,
            @Param("status") CatalogStatus status,
            @Param("nutritionSupported") Boolean nutritionSupported,
            Pageable pageable);
}
