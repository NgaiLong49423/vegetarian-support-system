package tech.mamxanh.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the {@code SUBSCRIPTION} table storing paid membership periods (FR-13, Q9, Q10).
 */
@Entity
@Table(name = "\"SUBSCRIPTION\"")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 10)
    private SubscriptionTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SubscriptionEntity(Long userId, SubscriptionTier tier, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.userId = userId;
        this.tier = tier;
        this.status = SubscriptionStatus.ACTIVE;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActiveAt(LocalDateTime now) {
        return this.status == SubscriptionStatus.ACTIVE
                && !now.isBefore(this.startsAt)
                && now.isBefore(this.endsAt);
    }
}
