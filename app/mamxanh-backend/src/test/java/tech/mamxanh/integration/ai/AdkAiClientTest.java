package tech.mamxanh.integration.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.integration.ai.adk.AdkSingleStepTaskRunner;
import tech.mamxanh.integration.ai.adk.AdkSingleStepTaskRunner.SingleStepResult;
import tech.mamxanh.subscription.service.AiChatSettingsService;

import java.util.List;
import java.util.Optional;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdkAiClientTest {

    @Mock
    private AdkSingleStepTaskRunner taskRunner;

    @Mock
    private CurrentUserService users;

    @Mock
    private AiChatSettingsService chatSettings;

    private AutoCloseable mocks;
    private AdkAiClient client;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(taskRunner.model()).thenReturn("gemini-3.8-flash");
        client = new AdkAiClient(taskRunner, users, chatSettings);
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @Test
    void generateRecipeContentRoutesToAdkRunner() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 100, 200, 300, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("RECIPE_TEXT"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"description\":\"Món chay ngon\",\"steps\":[]}", usage));

        var result = client.generateRecipeContent("Nấu món đậu phụ", List.of(1L, 2L));

        assertThat(result.json()).isEqualTo("{\"description\":\"Món chay ngon\",\"steps\":[]}");
        assertThat(result.promptTokens()).isEqualTo(100);
        assertThat(result.responseTokens()).isEqualTo(200);
        assertThat(result.totalTokens()).isEqualTo(300);
    }

    @Test
    void generateRecipeTextDelegatesToGenerateRecipeContent() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 100, 200, 300, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("RECIPE_TEXT"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"description\":\"Đậu phụ chiên sả\",\"steps\":[]}", usage));

        String json = client.generateRecipeText("Nấu đậu phụ", List.of(1L));

        assertThat(json).isEqualTo("{\"description\":\"Đậu phụ chiên sả\",\"steps\":[]}");
    }

    @Test
    void recipeTaskUsesTheMembersAllowedModelInsteadOfTheFixedDefault() {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(chatSettings.resolve(7L, null)).thenReturn("gemini-3.5-flash-lite");
        AiTokenUsage usage = new AiTokenUsage("gemini-3.5-flash-lite", "STANDARD", 10, 20, 30, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), eq("gemini-3.5-flash-lite"), eq("RECIPE_TEXT"),
                any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"description\":\"Món chay\",\"steps\":[]}", usage));

        var result = client.generateRecipeContent("Nấu đậu phụ", List.of(1L));

        assertThat(result.usage().model()).isEqualTo("gemini-3.5-flash-lite");
        verify(chatSettings).resolve(7L, null);
    }

    @Test
    void recommendRecipesRoutesToAdkRunner() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 50, 50, 100, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("RECOMMENDATIONS"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"recipeIds\":[10, 20]}", usage));

        var result = client.recommendRecipes("Gợi ý món", List.of(10L, 20L, 30L), 2);

        assertThat(result.json()).isEqualTo("{\"recipeIds\":[10, 20]}");
        assertThat(result.totalTokens()).isEqualTo(100);
    }

    @Test
    void classifyChatScopeReportsEmptyResultAsUnavailable() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 10, 10, 20, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("CHAT_SCOPE"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("", usage));

        assertThatThrownBy(() -> client.classifyChatScope("gemini-3.8-flash", "Instruction", "Hỏi bệnh lý",
                Duration.ofSeconds(3)))
                .isInstanceOf(AiProviderException.class);
        verify(taskRunner).executeTaskWithResult(any(), eq("gemini-3.8-flash"), eq("CHAT_SCOPE"), any(), any(),
                any(), eq(1024), eq(Duration.ofSeconds(3)));
    }

    @Test
    void answerChatRoutesToAdkRunner() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 80, 120, 200, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("CHAT"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"action\":\"NONE\",\"answer\":\"Nấu súp lơ\",\"recommendations\":[]}", usage));

        var result = client.answerChat("gemini-3.8-flash", "Instruction", "Nấu canh súp lơ thế nào?");

        assertThat(result.structuredJson()).contains("Nấu súp lơ");
        assertThat(result.totalTokens()).isEqualTo(200);
    }
}
