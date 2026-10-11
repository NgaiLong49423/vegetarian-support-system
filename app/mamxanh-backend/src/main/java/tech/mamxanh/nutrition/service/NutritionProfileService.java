package tech.mamxanh.nutrition.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.dto.request.UpdateNutritionEligibilityRequest;
import tech.mamxanh.nutrition.dto.response.NutritionEligibilityResponse;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse.Profile;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse.Results;
import tech.mamxanh.nutrition.entity.NutritionProfileEntity;
import tech.mamxanh.nutrition.entity.NutritionEligibilityStatus;
import tech.mamxanh.nutrition.repository.NutritionProfileRepository;

@Service
public class NutritionProfileService {
    private final NutritionProfileRepository repository;
    private final NutritionCalculationService calculations;
    private final Clock clock;

    @Autowired
    public NutritionProfileService(NutritionProfileRepository repository, NutritionCalculationService calculations) {
        this(repository, calculations, Clock.systemUTC());
    }

    NutritionProfileService(NutritionProfileRepository repository, NutritionCalculationService calculations, Clock clock) {
        this.repository = repository;
        this.calculations = calculations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NutritionProfileResponse getOwnProfile() {
        NutritionProfileEntity user = getAuthenticatedMember();
        requireEligible(user);
        if (!hasSavedProfile(user)) {
            return new NutritionProfileResponse(false, false, List.of(), null, null);
        }
        int age = calculations.ageOn(user.getDateOfBirth(), LocalDate.now(clock));
        List<String> reasons = outOfScopeReasons(age, false, false, false);
        if (!reasons.isEmpty()) {
            return new NutritionProfileResponse(true, false, reasons, null, null);
        }
        return new NutritionProfileResponse(true, false, List.of(),
                new Profile(user.getDateOfBirth(), user.getBiologicalSex(), user.getHeightCm(), user.getWeightKg(), user.getActivityLevel(), user.getNutritionGoal()), null);
    }

    @Transactional(readOnly = true)
    public NutritionProfileResponse calculateOwnResults(ConfirmNutritionEligibilityRequest confirmation) {
        NutritionProfileEntity user = getAuthenticatedMember();
        requireEligible(user);
        if (!hasSavedProfile(user)) {
            throw new NutritionProfileException(HttpStatus.CONFLICT, "NUTRITION_PROFILE_REQUIRED", "Lưu hồ sơ dinh dưỡng trước khi xem chỉ số tham khảo.");
        }
        int age = calculations.ageOn(user.getDateOfBirth(), LocalDate.now(clock));
        List<String> reasons = outOfScopeReasons(age, confirmation.pregnant(), confirmation.breastfeeding(), confirmation.therapeuticDietRequired());
        if (!reasons.isEmpty()) {
            throw new NutritionProfileException(HttpStatus.UNPROCESSABLE_ENTITY, "NUTRITION_PROFILE_OUT_OF_SCOPE",
                    "Không thể hiển thị hồ sơ hoặc chỉ số cho trường hợp ngoài phạm vi hỗ trợ: " + String.join(", ", reasons));
        }
        return response(user, age);
    }

    @Transactional
    public NutritionProfileResponse saveOwnProfile(SaveNutritionProfileRequest request) {
        NutritionProfileEntity user = getAuthenticatedMember();
        requireEligible(user);
        LocalDate today = LocalDate.now(clock);
        if (request.dateOfBirth().isBefore(LocalDate.of(1900, 1, 1)) || request.dateOfBirth().isAfter(today)) {
            throw new NutritionProfileException(HttpStatus.BAD_REQUEST, "INVALID_DATE_OF_BIRTH", "Ngày sinh phải từ 01/01/1900 đến hôm nay.");
        }
        if (request.heightCm() == null || request.heightCm().compareTo(new java.math.BigDecimal("100")) < 0
                || request.heightCm().compareTo(new java.math.BigDecimal("250")) > 0
                || request.weightKg() == null || request.weightKg().compareTo(new java.math.BigDecimal("30")) < 0
                || request.weightKg().compareTo(new java.math.BigDecimal("300")) > 0) {
            throw new NutritionProfileException(HttpStatus.BAD_REQUEST, "INVALID_ANTHROPOMETRIC_MEASUREMENT", "Chiều cao phải từ 100 đến 250 cm và cân nặng từ 30 đến 300 kg.");
        }
        if (!Boolean.TRUE.equals(request.consentAccepted())) {
            throw new NutritionProfileException(HttpStatus.BAD_REQUEST, "HEALTH_DATA_CONSENT_REQUIRED", "Cần đồng ý lưu dữ liệu tự khai báo trước khi lưu hồ sơ.");
        }
        if (!List.of("MALE", "FEMALE").contains(request.biologicalSex())) {
            throw new NutritionProfileException(HttpStatus.BAD_REQUEST, "INVALID_BIOLOGICAL_SEX", "Giới tính sinh học không hợp lệ.");
        }
        if (!List.of("SEDENTARY", "LIGHTLY_ACTIVE", "MODERATELY_ACTIVE", "VERY_ACTIVE").contains(request.activityLevel())) {
            throw new NutritionProfileException(HttpStatus.BAD_REQUEST, "INVALID_ACTIVITY_LEVEL", "Mức vận động không hợp lệ.");
        }
        if (!List.of("MAINTAIN_WEIGHT", "IMPROVE_HEALTH", "SUPPORT_TRAINING").contains(request.nutritionGoal())) {
            throw new NutritionProfileException(HttpStatus.BAD_REQUEST, "INVALID_NUTRITION_GOAL", "Mục tiêu dinh dưỡng không hợp lệ.");
        }
        int age = calculations.ageOn(request.dateOfBirth(), today);
        List<String> reasons = outOfScopeReasons(age, request.pregnant(), request.breastfeeding(), request.therapeuticDietRequired());
        if (!reasons.isEmpty()) {
            throw new NutritionProfileException(HttpStatus.UNPROCESSABLE_ENTITY, "NUTRITION_PROFILE_OUT_OF_SCOPE",
                    "Không thể lưu hồ sơ dinh dưỡng hoặc hiển thị chỉ số cho trường hợp ngoài phạm vi hỗ trợ: " + String.join(", ", reasons));
        }

        user.setDateOfBirth(request.dateOfBirth());
        user.setBiologicalSex(request.biologicalSex());
        user.setHeightCm(request.heightCm());
        user.setWeightKg(request.weightKg());
        user.setActivityLevel(request.activityLevel());
        user.setNutritionGoal(request.nutritionGoal());
        user.setHealthDataConsent(true);
        user.setHealthDataConsentAt(LocalDateTime.now(clock.withZone(ZoneOffset.UTC)));
        user.setUpdatedAt(LocalDateTime.now(clock.withZone(ZoneOffset.UTC)));
        NutritionProfileEntity saved = repository.save(user);
        return new NutritionProfileResponse(true, false, List.of(),
                new Profile(saved.getDateOfBirth(), saved.getBiologicalSex(), saved.getHeightCm(), saved.getWeightKg(), saved.getActivityLevel(), saved.getNutritionGoal()), null);
    }

    @Transactional(readOnly = true)
    public NutritionEligibilityResponse getOwnEligibility() {
        NutritionProfileEntity user = getAuthenticatedMember();
        return eligibilityResponse(user);
    }

    @Transactional
    public NutritionEligibilityResponse updateOwnEligibility(UpdateNutritionEligibilityRequest request) {
        NutritionProfileEntity user = getAuthenticatedMember();
        if (request == null || request.status() == null) {
            throw new NutritionProfileException(HttpStatus.BAD_REQUEST, "INVALID_NUTRITION_ELIGIBILITY_STATUS",
                    "Chỉ có thể xác nhận trạng thái đủ hoặc không đủ điều kiện dinh dưỡng.");
        }
        NutritionEligibilityStatus status = request.status().toEligibilityStatus();

        LocalDateTime confirmedAt = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        user.setNutritionEligibilityStatus(status);
        user.setNutritionEligibilityConfirmedAt(confirmedAt);
        user.setNutritionScopeConfirmed(true);
        user.setUpdatedAt(confirmedAt);
        NutritionProfileEntity saved = repository.save(user);
        return eligibilityResponse(saved);
    }

    /** Shared guard for personalized nutrition endpoints across FR-35/36/37. */
    public void requireEligible(long userId) {
        NutritionProfileEntity user = repository.findById(userId)
                .orElseThrow(() -> new NutritionProfileException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Không tìm thấy hồ sơ Member."));
        if (!"ACTIVE".equals(user.getAccountStatus()) || !List.of("CUSTOMER", "EXPERT").contains(user.getRole())) {
            throw new NutritionProfileException(HttpStatus.FORBIDDEN, "MEMBER_ACCESS_REQUIRED", "Chỉ Member có tài khoản hoạt động được truy cập hồ sơ dinh dưỡng.");
        }
        requireEligible(user);
    }

    private void requireEligible(NutritionProfileEntity user) {
        if (user.getNutritionEligibilityStatus() == NutritionEligibilityStatus.ELIGIBLE) return;
        if (user.getNutritionEligibilityStatus() == NutritionEligibilityStatus.INELIGIBLE) {
            throw new NutritionProfileException(HttpStatus.FORBIDDEN, "NUTRITION_ELIGIBILITY_INELIGIBLE",
                    "Tài khoản hiện không thuộc phạm vi hỗ trợ dinh dưỡng.");
        }
        throw new NutritionProfileException(HttpStatus.FORBIDDEN, "NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED",
                "Cần xác nhận phạm vi hỗ trợ dinh dưỡng trước khi sử dụng chức năng này.");
    }

    private NutritionEligibilityResponse eligibilityResponse(NutritionProfileEntity user) {
        return new NutritionEligibilityResponse(user.getNutritionEligibilityStatus(), user.getNutritionEligibilityConfirmedAt());
    }

    private NutritionProfileEntity getAuthenticatedMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new NutritionProfileException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Đăng nhập để truy cập hồ sơ dinh dưỡng cá nhân.");
        }
        long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new NutritionProfileException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Không xác định được Member từ phiên đăng nhập.");
        }
        NutritionProfileEntity user = repository.findById(userId)
                .orElseThrow(() -> new NutritionProfileException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Không tìm thấy hồ sơ Member."));
        if (!"ACTIVE".equals(user.getAccountStatus()) || !List.of("CUSTOMER", "EXPERT").contains(user.getRole())) {
            throw new NutritionProfileException(HttpStatus.FORBIDDEN, "MEMBER_ACCESS_REQUIRED", "Chỉ Member có tài khoản hoạt động được truy cập hồ sơ dinh dưỡng.");
        }
        return user;
    }

    private NutritionProfileResponse response(NutritionProfileEntity user, int age) {
        var bmi = calculations.bmi(user.getWeightKg(), user.getHeightCm());
        var energy = calculations.energyKcal(age, user.getBiologicalSex(), user.getHeightCm(), user.getWeightKg(), user.getActivityLevel());
        var profile = new Profile(user.getDateOfBirth(), user.getBiologicalSex(), user.getHeightCm(), user.getWeightKg(), user.getActivityLevel(), user.getNutritionGoal());
        var results = new Results(bmi, calculations.bmiCategory(bmi), energy,
                calculations.dailyTargets(age, user.getBiologicalSex(), energy), calculations.energySource(), calculations.nutrientSource());
        return new NutritionProfileResponse(true, true, List.of(), profile, results);
    }

    private boolean hasSavedProfile(NutritionProfileEntity user) {
        return user.getDateOfBirth() != null && user.getBiologicalSex() != null && user.getHeightCm() != null
                && user.getWeightKg() != null && user.getActivityLevel() != null && user.getNutritionGoal() != null
                && user.isHealthDataConsent();
    }

    private List<String> outOfScopeReasons(int age, boolean pregnant, boolean breastfeeding, boolean therapeuticDietRequired) {
        var reasons = new java.util.ArrayList<String>();
        if (age < 18 || age > 120) reasons.add("AGE_OUT_OF_SCOPE");
        if (pregnant) reasons.add("PREGNANT");
        if (breastfeeding) reasons.add("BREASTFEEDING");
        if (therapeuticDietRequired) reasons.add("THERAPEUTIC_DIET_REQUIRED");
        return reasons;
    }
}
