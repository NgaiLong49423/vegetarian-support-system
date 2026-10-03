package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveNutritionProfileRequest(
        @NotNull @PastOrPresent LocalDate dateOfBirth,
        @NotBlank String biologicalSex,
        @NotNull(message = "Nhập chiều cao.")
        @DecimalMin(value = "100.0", message = "Chiều cao tối thiểu là 100 cm.")
        @DecimalMax(value = "250.0", message = "Chiều cao tối đa là 250 cm.") BigDecimal heightCm,
        @NotNull(message = "Nhập cân nặng.")
        @DecimalMin(value = "30.0", message = "Cân nặng tối thiểu là 30 kg.")
        @DecimalMax(value = "300.0", message = "Cân nặng tối đa là 300 kg.") BigDecimal weightKg,
        @NotBlank String activityLevel,
        @NotBlank String nutritionGoal,
        @NotNull Boolean pregnant,
        @NotNull Boolean breastfeeding,
        @NotNull Boolean therapeuticDietRequired,
        @NotNull Boolean consentAccepted) {
}
