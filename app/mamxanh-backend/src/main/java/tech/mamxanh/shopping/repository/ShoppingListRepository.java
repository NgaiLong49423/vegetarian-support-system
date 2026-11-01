package tech.mamxanh.shopping.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.shopping.entity.ShoppingListEntity;

import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingListEntity, Long> {
    Page<ShoppingListEntity> findByUserId(Long userId, Pageable pageable);
    Optional<ShoppingListEntity> findByIdAndUserId(Long id, Long userId);
    Optional<ShoppingListEntity> findByUserIdAndKind(Long userId, String kind);

    /** Serialize mutations of a list, including cleanup and item changes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ShoppingListEntity l where l.id = :id and l.userId = :userId")
    Optional<ShoppingListEntity> lockOwned(@Param("id") long id, @Param("userId") long userId);
}
