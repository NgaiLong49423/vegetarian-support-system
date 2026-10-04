package tech.mamxanh.subscription.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    Optional<SubscriptionEntity> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<SubscriptionEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SubscriptionEntity> findFirstByUserIdOrderByEndsAtDesc(Long userId);
}
