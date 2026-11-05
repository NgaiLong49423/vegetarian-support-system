package tech.mamxanh.subscription.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    Optional<PaymentTransactionEntity> findByOrderCode(String orderCode);

    Optional<PaymentTransactionEntity> findByUserIdAndOrderCode(Long userId, String orderCode);

    @Query("select p.userId from PaymentTransactionEntity p where p.orderCode = :orderCode")
    Optional<Long> findUserIdByOrderCode(@Param("orderCode") String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentTransactionEntity p where p.orderCode = :orderCode")
    Optional<PaymentTransactionEntity> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    List<PaymentTransactionEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
