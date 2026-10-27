package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.*;

class AiChatIntentTest {
    @ParameterizedTest
    @ValueSource(strings = {"Lập thực đơn tuần 7 ngày theo dinh dưỡng cá nhân cho tôi.",
            "Lên menu cho tuần này", "lap thuc don bay ngay", "Create a 7-day meal plan",
            "Make a personalized meal plan", "Tạo thực đơn theo hồ sơ", "Lập thực đơn tuần rồi explain tại sao",
            "What is a weekly meal plan? Generate one for me."})
    void routesExplicitAdvancedMenuOperationsBeforeProvider(String question) {
        assertThat(AiChatIntent.workflow(chat(question))).isEqualTo(AiChatIntent.Workflow.WEEKLY_MENU);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Thực đơn tuần là gì?", "What is a weekly meal plan?", "Đậu hũ có protein không?",
            "Hãy viết công thức mới cho tôi", "Biến tấu công thức này", "Thay sữa bò bằng gì?", "Bài viết này mô tả món gì?"})
    void generalQuestionsAndRecipeDraftsKeepTheirExistingChatRights(String question) {
        assertThat(AiChatIntent.workflow(chat(question))).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Viết mô tả cho bài đăng công thức", "Soạn bước làm cho bài viết", "Draft recipe post instructions",
            "Viết bài công thức mới để đăng"})
    void authoringIsSeparateFromAnExplicitChatRecipeDraft(String question) {
        assertThat(AiChatIntent.workflow(chat(question))).isEqualTo(AiChatIntent.Workflow.RECIPE_AUTHORING);
    }

    @Test
    void shortConfirmationKeepsAdvancedIntentButNewOrdinaryQuestionDoesNot() {
        var history = List.of(new Turn(Speaker.USER, "Lập thực đơn tuần"), new Turn(Speaker.ASSISTANT, "Mở Lịch ăn"));
        assertThat(AiChatIntent.workflow(new AiChatRequest(ContextType.GENERAL, null, "Làm đi", history)))
                .isEqualTo(AiChatIntent.Workflow.WEEKLY_MENU);
        assertThat(AiChatIntent.workflow(new AiChatRequest(ContextType.GENERAL, null, "Đậu hũ nấu sao?", history))).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"còn món khác không", "các món kho thử xem", "Gợi ý món chay dễ nấu", "CAC MON KHO THU XEM"})
    void naturalRecipeRequestsTriggerDiscovery(String question) {
        assertThat(AiChatIntent.discovery(chat(question))).isTrue();
    }

    @Test
    void moreDishesPreserveTheLatestUserCategoryAcrossSeveralTurns() {
        var history = List.of(new Turn(Speaker.USER, "các món kho thử xem"), new Turn(Speaker.ASSISTANT, "Món kho", List.of(3L)),
                new Turn(Speaker.USER, "còn món khác không"), new Turn(Speaker.ASSISTANT, "Món nữa", List.of(4L)));
        var request = new AiChatRequest(ContextType.GENERAL, null, "còn món khác không", history);
        assertThat(AiChatIntent.category(request)).isEqualTo("BRAISED");
        assertThat(AiChatIntent.category(new AiChatRequest(ContextType.GENERAL, null, "các món canh", history))).isEqualTo("SOUP");
        assertThat(AiChatIntent.category(chat("Gợi ý món ăn nhanh"))).isNull();
        assertThat(AiChatIntent.category(chat("Gợi ý món khó nấu trong kho công thức"))).isNull();
        assertThat(AiChatIntent.category(chat("Gợi ý món gỏi"))).isEqualTo("SALAD");
        assertThat(AiChatIntent.category(chat("Gợi ý món nước"))).isEqualTo("NOODLE_SOUP");
        assertThat(AiChatIntent.category(chat("Gợi ý gỏi cuốn"))).isEqualTo("ROLL");
    }

    private static AiChatRequest chat(String question) { return new AiChatRequest(ContextType.GENERAL, null, question, null); }

    @ParameterizedTest
    @ValueSource(strings = {"Gợi ý món khó nấu", "Gợi ý các món khổ qua", "Gợi ý món khô", "Món nào nấu lâu?", "Tôi gọi món gì?"})
    void accentsInIngredientNamesOrOrdinaryWordsDoNotBecomeDishCategories(String question) {
        assertThat(AiChatIntent.category(chat(question))).isNull();
    }
}
