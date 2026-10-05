package tech.mamxanh.nutrition.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-31 view of the existing {@code USER} row (decision Q18): only the dietary-preference and
 * Onboarding columns are written, and only the changed ones ({@link DynamicUpdate}). The
 * {@code auth} module creates and deletes users; this entity is never inserted.
 */
@Entity
@Table(name = "\"USER\"")
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DietaryPreferenceEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role", nullable = false, insertable = false, updatable = false)
    private String role;

    @Column(name = "account_status", nullable = false, insertable = false, updatable = false)
    private String accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "vegetarian_type", length = 20)
    private VegetarianType vegetarianType;

    @Nationalized
    @Column(name = "cuisine_preference", length = 200)
    private String cuisinePreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_difficulty", length = 20)
    private CookingDifficulty preferredDifficulty;

    @Column(name = "max_cooking_time_min")
    private Integer maxCookingTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 20)
    private OnboardingStatus onboardingStatus;

    @Column(name = "avoid_none_confirmed", nullable = false)
    private boolean avoidNoneConfirmed;

    @Column(name = "dislike_none_confirmed", nullable = false)
    private boolean dislikeNoneConfirmed;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Members are customers and experts; administrators have no dietary profile. */
    public boolean isActiveMember() {
        return "ACTIVE".equals(accountStatus) && ("CUSTOMER".equals(role) || "EXPERT".equals(role));
    }

    /**
     * UC-31.1/UC-31.3: stores a validated profile. A "none" confirmation only stays set while its
     * list is empty (Data Dictionary rule 30), and saving completes the Onboarding.
     */
    public void update(VegetarianType vegetarianType, boolean avoidNoneConfirmed, boolean dislikeNoneConfirmed,
            String cuisinePreference, Integer maxCookingTimeMinutes, CookingDifficulty preferredDifficulty,
            LocalDateTime now) {
        this.vegetarianType = vegetarianType;
        this.avoidNoneConfirmed = avoidNoneConfirmed;
        this.dislikeNoneConfirmed = dislikeNoneConfirmed;
        this.cuisinePreference = cuisinePreference;
        this.maxCookingTimeMinutes = maxCookingTimeMinutes;
        this.preferredDifficulty = preferredDifficulty;
        this.onboardingStatus = OnboardingStatus.COMPLETED;
        this.updatedAt = now;
    }

    /** UC-31.2: closing an unanswered invitation; a completed profile keeps its status. */
    public void skipOnboarding(LocalDateTime now) {
        if (onboardingStatus == OnboardingStatus.NOT_STARTED) {
            onboardingStatus = OnboardingStatus.SKIPPED;
            updatedAt = now;
        }
    }
}
