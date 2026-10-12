package tech.mamxanh.nutrition.repository;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.nutrition.entity.CatalogStatus;
import tech.mamxanh.nutrition.entity.IngredientEntity;

public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<IngredientEntity> findByStatus(CatalogStatus status);
    List<IngredientEntity> findByIdInAndStatus(Collection<Long> ids, CatalogStatus status);
    List<IngredientEntity> findByNameContainingIgnoreCaseOrIngredientGroupContainingIgnoreCase(String name, String group);
}
