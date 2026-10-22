package tech.mamxanh.shopping.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity;

public interface ShoppingRecipeContributionRepository extends JpaRepository<ShoppingRecipeContributionEntity, Long> {
    List<ShoppingRecipeContributionEntity> findByShoppingListIdOrderByIdAsc(Long listId);
}
