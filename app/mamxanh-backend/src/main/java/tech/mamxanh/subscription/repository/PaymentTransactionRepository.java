package tech.mamxanh.subscription.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    Optional<PaymentTransactionEntity> findByOrderCode(String orderCode);

    List<PaymentTransactionEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
