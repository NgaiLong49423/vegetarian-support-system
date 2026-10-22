package tech.mamxanh.shopping.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItemEntity, Long> {
    List<ShoppingListItemEntity> findByShoppingListIdOrderByIdAsc(Long listId);
    Optional<ShoppingListItemEntity> findByIdAndShoppingListId(Long id, Long listId);
    long deleteByShoppingListIdAndBoughtTrue(Long listId);
}
