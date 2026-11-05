package tech.mamxanh.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    Optional<SubscriptionEntity> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<SubscriptionEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SubscriptionEntity> findFirstByUserIdOrderByEndsAtDesc(Long userId);

    @Query(value = """
            SELECT CAST(CASE WHEN EXISTS (
                SELECT 1 FROM [SUBSCRIPTION]
                WHERE user_id = :userId AND status = 'ACTIVE' AND tier IN ('PLUS', 'PRO')
                  AND starts_at <= :now AND ends_at > :now
            ) THEN 1 ELSE 0 END AS bit)
            """, nativeQuery = true)
    boolean hasActivePaidSubscription(@Param("userId") long userId, @Param("now") LocalDateTime now);

    @Query(value = """
            SELECT CAST(CASE WHEN EXISTS (
                SELECT 1 FROM [SUBSCRIPTION]
                WHERE user_id = :userId AND status = 'ACTIVE' AND tier = 'PRO'
                  AND starts_at <= :now AND ends_at > :now
            ) THEN 1 ELSE 0 END AS bit)
            """, nativeQuery = true)
    boolean hasActiveProSubscription(@Param("userId") long userId, @Param("now") LocalDateTime now);
}
