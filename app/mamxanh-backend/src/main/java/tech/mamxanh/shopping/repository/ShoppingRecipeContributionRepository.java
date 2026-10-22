package tech.mamxanh.shopping.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity;

public interface ShoppingRecipeContributionRepository extends JpaRepository<ShoppingRecipeContributionEntity, Long> {
    List<ShoppingRecipeContributionEntity> findByShoppingListIdOrderByIdAsc(Long listId);

    @Modifying
    @Query("delete from ShoppingRecipeContributionEntity c where c.shoppingListId = :listId")
    int deleteAllByShoppingListId(@Param("listId") long listId);
}
