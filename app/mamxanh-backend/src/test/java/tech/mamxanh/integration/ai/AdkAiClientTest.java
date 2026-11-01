package tech.mamxanh.integration.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.integration.ai.adk.AdkSingleStepTaskRunner;
import tech.mamxanh.integration.ai.adk.AdkSingleStepTaskRunner.SingleStepResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdkAiClientTest {

    @Mock
    private AdkSingleStepTaskRunner taskRunner;

    @Mock
    private GeminiAiClient fallbackClient;

    private AutoCloseable mocks;
    private AdkAiClient primaryClient;
    private AdkAiClient fallbackModeClient;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(taskRunner.model()).thenReturn("gemini-3.8-flash");
        primaryClient = new AdkAiClient(taskRunner, fallbackClient, "ADK_PRIMARY");
        fallbackModeClient = new AdkAiClient(taskRunner, fallbackClient, "LEGACY_FALLBACK");
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @Test
    void generateRecipeContentRoutesToAdkRunnerWhenPrimary() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 100, 200, 300, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("RECIPE_TEXT"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"description\":\"Món chay ngon\",\"steps\":[]}", usage));

        var result = primaryClient.generateRecipeContent("Nấu món đậu phụ", List.of(1L, 2L));

        assertThat(result.json()).isEqualTo("{\"description\":\"Món chay ngon\",\"steps\":[]}");
        assertThat(result.promptTokens()).isEqualTo(100);
        assertThat(result.responseTokens()).isEqualTo(200);
        assertThat(result.totalTokens()).isEqualTo(300);
        verifyNoInteractions(fallbackClient);
    }

    @Test
    void generateRecipeContentDelegatesToFallbackWhenConfigured() {
        AiClient.RecommendationResult expected = new AiClient.RecommendationResult("{\"description\":\"Legacy\"}", 10, 20, 30);
        when(fallbackClient.generateRecipeContent(any(), any())).thenReturn(expected);

        var result = fallbackModeClient.generateRecipeContent("Prompt", List.of(1L));

        assertThat(result).isSameAs(expected);
        verify(fallbackClient).generateRecipeContent("Prompt", List.of(1L));
        verifyNoInteractions(taskRunner);
    }

    @Test
    void recommendRecipesRoutesToAdkRunner() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 50, 50, 100, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("RECOMMENDATIONS"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"recipeIds\":[10, 20]}", usage));

        var result = primaryClient.recommendRecipes("Gợi ý món", List.of(10L, 20L, 30L), 2);

        assertThat(result.json()).isEqualTo("{\"recipeIds\":[10, 20]}");
        assertThat(result.totalTokens()).isEqualTo(100);
    }

    @Test
    void classifyChatScopeFailClosedOnEmptyResult() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 10, 10, 20, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("CHAT_SCOPE"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("", usage));

        var result = primaryClient.classifyChatScope("gemini-3.8-flash", "Instruction", "Hỏi bệnh lý");

        assertThat(result.structuredJson()).contains("MX_CHAT_SENSITIVE", "OTHER_HIGH_RISK");
    }

    @Test
    void answerChatRoutesToAdkRunner() {
        AiTokenUsage usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 80, 120, 200, 0, 0, 0);
        when(taskRunner.executeTaskWithResult(any(), any(), eq("CHAT"), any(), any(), any(), anyInt(), any()))
                .thenReturn(new SingleStepResult("{\"action\":\"NONE\",\"answer\":\"Nấu súp lơ\",\"recommendations\":[]}", usage));

        var result = primaryClient.answerChat("gemini-3.8-flash", "Instruction", "Nấu canh súp lơ thế nào?");

        assertThat(result.structuredJson()).contains("Nấu súp lơ");
        assertThat(result.totalTokens()).isEqualTo(200);
    }
}
