package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.integration.ai.AiClient;

/** Real application, SQL Server and published-context query; Gemini alone is controlled. */
@Transactional
class GuestAiChatIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AiClient ai;

    @Test
    void guestReadsOnlyCurrentPublicRecipeAndDoesNotCreateAnAccountOrChangeRecipeData() throws Exception {
        String email = "fr02-" + UUID.randomUUID() + "@test.local";
        jdbc.update("INSERT INTO [USER](email,display_name,role,account_status,email_verified) VALUES (?,'FR02 Expert','EXPERT','ACTIVE',1)", email);
        Long author = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email=?", Long.class, email);
        jdbc.update("""
                INSERT INTO [RECIPE_POST](author_id,title,description,instructions,dish_category,vegetarian_type,
                    difficulty,servings,prep_time_min,cook_time_min,status,published_at)
                VALUES (?,'FR02 Canh rau',N'Món canh',N'Nấu rau trong 10 phút.','SOUP','VEGAN','EASY',2,5,10,'PUBLISHED',SYSUTCDATETIME())
                """, author);
        Long recipeId = jdbc.queryForObject("SELECT recipe_id FROM [RECIPE_POST] WHERE author_id=?", Long.class, author);
        Long usersBefore = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM [USER]", Long.class);
        when(ai.classifyChatScope(anyString(), anyString())).thenReturn(
                new AiClient.ChatResult(null, "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 1, 1, 2));
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Công thức ghi 10 phút.", 1, 2, 3));
        String body = "{\"contextType\":\"RECIPE\",\"recipeId\":" + recipeId + ",\"question\":\"Nấu bao lâu?\"}";
        String id = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/recipes/ai/chat").header("X-Guest-Chat-Id", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("Công thức ghi 10 phút."));
        verify(ai).answerChat(anyString(), contains("Nấu rau trong 10 phút."));
        assertThat(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM [USER]", Long.class)).isEqualTo(usersBefore);
        assertThat(jdbc.queryForObject("SELECT instructions FROM [RECIPE_POST] WHERE recipe_id=?", String.class, recipeId))
                .isEqualTo("Nấu rau trong 10 phút.");
        jdbc.update("UPDATE [RECIPE_POST] SET status='HIDDEN' WHERE recipe_id=?", recipeId);
        mockMvc.perform(post("/api/v1/recipes/ai/chat").header("X-Guest-Chat-Id", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        verify(ai, times(1)).answerChat(anyString(), anyString());
    }
}
