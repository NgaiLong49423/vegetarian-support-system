package tech.mamxanh.nutrition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nutrition-owned view of the existing USER profile row. */
@Entity
@Table(name = "\"USER\"")
@Getter
@Setter
@NoArgsConstructor
public class NutritionProfileEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "account_status", nullable = false)
    private String accountStatus;

    @Column(name = "biological_sex")
    private String biologicalSex;

    @Column(name = "height_cm", precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "activity_level")
    private String activityLevel;

    @Column(name = "pregnant", nullable = false)
    private boolean pregnant;

    @Column(name = "breastfeeding", nullable = false)
    private boolean breastfeeding;

    @Column(name = "therapeutic_diet_required", nullable = false)
    private boolean therapeuticDietRequired;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nutrition_goal")
    private String nutritionGoal;

    @Column(name = "health_data_consent", nullable = false)
    private boolean healthDataConsent;

    @Column(name = "health_data_consent_at")
    private LocalDateTime healthDataConsentAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
