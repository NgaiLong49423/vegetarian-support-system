package tech.mamxanh.expertapplication;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ReviewRequest;

class ExpertApplicationRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test void acceptsRequiredLengthsAndOptionalHttpPortfolio() {
        var request = new ExpertApplicationRequest("Kinh nghiệm nấu các món chay nhiều năm.", "VEGAN",
                "Công thức đậu hũ sốt nấm với hướng dẫn chế biến chi tiết.", "https://example.org/portfolio");
        assertThat(validator.validate(request)).isEmpty();
        assertThat(validator.validate(new ExpertApplicationRequest(request.experience(), request.vegetarianType(), request.sampleRecipeSummary(), null))).isEmpty();
    }

    @Test void rejectsShortFieldsInvalidPortfolioAndOutOfRangeRejectionReason() {
        var request = new ExpertApplicationRequest("short", "VEGAN", "too short", "ftp://example.org/file");
        assertThat(validator.validate(request)).hasSize(3);
        assertThat(validator.validate(new ReviewRequest("short"))).isNotEmpty();
        assertThat(validator.validate(new ReviewRequest("Lý do từ chối đủ độ dài."))).isEmpty();
    }
}
