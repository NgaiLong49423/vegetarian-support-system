package tech.mamxanh.nutrition.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-31 ingredient to avoid or disliked ingredient of one Member. The display name is always kept
 * in {@code custom_ingredient_name}; {@code ingredient_id} is set when the name matches an active
 * standard ingredient.
 */
@Entity
@Table(name = "USER_INGREDIENT_PREFERENCE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIngredientPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Nationalized
    @Column(name = "custom_ingredient_name", length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 20)
    private PreferenceType preferenceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserIngredientPreferenceEntity of(Long userId, PreferenceType type, Long ingredientId, String name,
            LocalDateTime now) {
        UserIngredientPreferenceEntity preference = new UserIngredientPreferenceEntity();
        preference.userId = userId;
        preference.preferenceType = type;
        preference.ingredientId = ingredientId;
        preference.name = name;
        preference.createdAt = now;
        return preference;
    }
}
