package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;

class AiIngredientMentionGuardTest {
    private static final List<IngredientResponse> CATALOG = List.of(
            ingredient(1L, "Đậu hũ"), ingredient(2L, "Cà chua"), ingredient(3L, "Nước tương"),
            ingredient(4L, "Nước"), ingredient(5L, "Dầu ăn"), ingredient(6L, "Nước sốt mè"),
            ingredient(7L, "Chao"), ingredient(8L, "Bơ"), ingredient(9L, "Lựu"), ingredient(10L, "Nấm"));
    private static final Set<Long> SELECTED_IDS = Set.of(1L, 2L, 3L);
    private static final Set<String> SELECTED_NAMES = Set.of("Đậu hũ", "Cà chua", "Nước tương");

    @Test
    void selectedCompoundNameDoesNotIntroduceItsShorterCatalogName() {
        assertThatCode(() -> validate("Thêm NƯỚC TƯƠNG vào phần cà chua, đảo nhẹ cùng đậu hũ."))
                .doesNotThrowAnyException();
    }

    @Test
    void sauceProducedFromSelectedIngredientsIsNotAddedWater() {
        assertThatCode(() -> validate("Kho đậu hũ trong nước sốt cà chua đến khi nước kho sánh lại."))
                .doesNotThrowAnyException();
    }

    @Test
    void cookwareAndCookingVerbsAreNotIngredientsWithDifferentVietnameseAccents() {
        assertThatCode(() -> validate("Làm nóng chảo, bỏ đậu hũ vào rồi đảo nhẹ tay."))
                .doesNotThrowAnyException();
        assertRejected("Thêm chao để nêm món ăn.");
    }

    @Test
    void dicingTomatoesDoesNotIntroducePomegranate() {
        assertThatCode(() -> validate("Cà chua rửa sạch rồi thái hạt lựu; cắt đậu hũ thành hạt lựu."))
                .doesNotThrowAnyException();
        assertRejected("Thêm hạt lựu lên trên để trang trí.");
        assertRejected("Thái nấm thành hạt lựu.");
    }

    @Test
    void rinsingWaterIsPreparationButAddingWaterAfterRinsingIsStillRejected() {
        assertThatCode(() -> validate("Rửa sạch cà chua dưới vòi nước rồi để ráo."))
                .doesNotThrowAnyException();
        assertRejected("Rửa cà chua rồi thêm nước vào nồi.");
        assertRejected("Rửa nấm bằng nước sạch.");
        assertRejected("Rửa bằng nước sốt mè.");
    }

    @Test
    void explicitlyAddingWaterStillRequiresTheWaterIngredient() {
        assertRejected("Thêm nước vào nồi cùng nước tương.");
    }

    @Test
    void unselectedSauceAndOilAreStillRejected() {
        assertRejected("Thêm nước sốt mè vào nồi.");
        assertRejected("Đun nóng dầu ăn rồi cho đậu hũ vào.");
    }

    @Test
    void selectingWaterDoesNotAllowAnUnselectedCompoundIngredient() {
        assertThatThrownBy(() -> AiIngredientMentionGuard.validate("Thêm nước tương.", Set.of(4L),
                Set.of("Nước"), CATALOG)).isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    private void validate(String text) {
        AiIngredientMentionGuard.validate(text, SELECTED_IDS, SELECTED_NAMES, CATALOG);
    }

    private void assertRejected(String text) {
        assertThatThrownBy(() -> validate(text)).isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    private static IngredientResponse ingredient(long id, String name) {
        return new IngredientResponse(id, name, "Test", "Test", null, LocalDate.of(2026, 10, 23), false, true);
    }
}
