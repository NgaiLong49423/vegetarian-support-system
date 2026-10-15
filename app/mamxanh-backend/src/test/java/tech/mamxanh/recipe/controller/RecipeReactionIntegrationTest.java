package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AccessTokenService;

class RecipeReactionIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository users;
    @Autowired private AccessTokenService tokens;
    private long memberId;
    private long authorId;
    private long recipeId;

    @BeforeEach
    void prepare() {
        memberId = insertUser("member", "CUSTOMER", "ACTIVE");
        authorId = insertUser("author", "EXPERT", "ACTIVE");
        recipeId = jdbc.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type,
                    difficulty, servings, prep_time_min, cook_time_min, status, published_at, updated_at)
                OUTPUT INSERTED.recipe_id VALUES (?, N'Issue56 công thức', N'Hướng dẫn chế biến đủ dài',
                    'SOUP', 'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME(), SYSUTCDATETIME())
                """, Long.class, authorId);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM [RECIPE_REACTION] WHERE recipe_id = ?", recipeId);
        jdbc.update("DELETE FROM [RECIPE_POST] WHERE recipe_id = ?", recipeId);
        jdbc.update("DELETE FROM [USER] WHERE email LIKE 'issue56-%@test.local'");
    }

    @Test
    void memberSetsSwitchesRemovesAndReadsPersistedChoice() throws Exception {
        mockMvc.perform(get(path()).with(principal(memberId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentReaction").value(nullValue()))
                .andExpect(jsonPath("$.likePercentage").value(nullValue()));
        set(memberId, "LIKE").andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.likePercentage").value(100));
        set(memberId, "LIKE").andExpect(status().isOk()).andExpect(jsonPath("$.reactionCount").value(1));
        mockMvc.perform(get(path()).with(principal(memberId)))
                .andExpect(jsonPath("$.currentReaction").value("LIKE"));
        set(memberId, "DISLIKE").andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(1)).andExpect(jsonPath("$.likePercentage").value(0));
        assertThat(count()).isEqualTo(1);
        set(memberId, "LIKE").andExpect(jsonPath("$.currentReaction").value("LIKE"));
        mockMvc.perform(delete(path()).with(principal(memberId))).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentReaction").value(nullValue())).andExpect(jsonPath("$.reactionCount").value(0));
        mockMvc.perform(delete(path()).with(principal(memberId))).andExpect(status().isOk());
        set(memberId, "DISLIKE").andExpect(status().isOk());
        mockMvc.perform(delete(path()).with(principal(memberId))).andExpect(status().isOk());
        assertThat(count()).isZero();
    }

    @Test
    void ac571RoundsTenLikesOutOfElevenToNinetyOne() throws Exception {
        for (int i = 0; i < 10; i++) {
            long voter = insertUser("voter" + i, "CUSTOMER", "ACTIVE");
            jdbc.update("INSERT INTO [RECIPE_REACTION] (user_id, recipe_id, reaction_type) VALUES (?, ?, ?)", voter, recipeId, i < 9 ? "LIKE" : "DISLIKE");
        }
        set(memberId, "LIKE").andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(10))
                .andExpect(jsonPath("$.dislikes").value(1)).andExpect(jsonPath("$.likePercentage").value(91));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", recipeId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statistics.likePercentage").value(91))
                .andExpect(jsonPath("$.statistics.reactionCount").value(11));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", "Issue56 công thức"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].likePercentage").value(91));
    }

    @Test
    void authorCannotMutateButCanReadStatistics() throws Exception {
        set(authorId, "LIKE").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_SELF_REACTION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.detail").value("Tác giả không được tự bình chọn bài viết của chính mình"));
        mockMvc.perform(delete(path()).with(principal(authorId))).andExpect(status().isForbidden());
        mockMvc.perform(get(path()).with(principal(authorId))).andExpect(status().isOk());
        assertThat(count()).isZero();
    }

    @Test
    void guestAdminAndLockedAccountCannotWriteAndExpertCanVoteOnAnotherAuthorsRecipe() throws Exception {
        mockMvc.perform(put(path()).contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(path())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(path())).andExpect(status().isUnauthorized());
        long admin = insertUser("admin", "ADMIN", "ACTIVE");
        long locked = insertUser("locked", "CUSTOMER", "LOCKED");
        set(admin, "LIKE").andExpect(status().isForbidden());
        set(locked, "LIKE").andExpect(status().isForbidden());
        long expert = insertUser("expert", "EXPERT", "ACTIVE");
        set(expert, "DISLIKE").andExpect(status().isOk());
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void validatesPayloadAndDoesNotTrustUserIdFromClient() throws Exception {
        for (String body : new String[]{"{}", "{\"reactionType\":null}", "{\"reactionType\":\"LOVE\"}", "{"}) {
            mockMvc.perform(put(path()).with(principal(memberId)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        mockMvc.perform(put(path()).with(principal(memberId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reactionType\":\"LIKE\",\"userId\":" + authorId + "}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT user_id FROM [RECIPE_REACTION] WHERE recipe_id = ?", Long.class, recipeId)).isEqualTo(memberId);
        mockMvc.perform(get("/api/v1/recipes/0/reaction").with(principal(memberId))).andExpect(status().isBadRequest());
    }

    @Test
    void isolatesCurrentReactionAndPreservesOtherMembersVote() throws Exception {
        long other = insertUser("other", "CUSTOMER", "ACTIVE");
        set(memberId, "LIKE").andExpect(status().isOk());
        mockMvc.perform(get(path()).with(principal(other))).andExpect(jsonPath("$.currentReaction").value(nullValue()))
                .andExpect(jsonPath("$.likes").value(1)).andExpect(jsonPath("$.userId").doesNotExist());
        set(other, "DISLIKE").andExpect(status().isOk());
        mockMvc.perform(delete(path()).with(principal(memberId))).andExpect(jsonPath("$.dislikes").value(1));
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void unavailableRecipesRejectAllOperationsAndKeepExistingVotes() throws Exception {
        set(memberId, "LIKE").andExpect(status().isOk());
        for (String state : new String[]{"HIDDEN", "DELETED"}) {
            jdbc.update("UPDATE [RECIPE_POST] SET status = ? WHERE recipe_id = ?", state, recipeId);
            set(memberId, "DISLIKE").andExpect(status().isNotFound());
            mockMvc.perform(get(path()).with(principal(memberId))).andExpect(status().isNotFound());
            mockMvc.perform(delete(path()).with(principal(memberId))).andExpect(status().isNotFound());
            assertThat(count()).isEqualTo(1);
            mockMvc.perform(get("/api/v1/recipes/{recipeId}", recipeId)).andExpect(status().isNotFound());
            for (String sort : new String[]{"MOST_LIKED", "MOST_ACTIVE", "TRENDING"}) {
                mockMvc.perform(get("/api/v1/recipes").param("keyword", "Issue56 công thức").param("sort", sort))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
            }
        }
        mockMvc.perform(get("/api/v1/recipes/9223372036854775807/reaction").with(principal(memberId))).andExpect(status().isNotFound());
    }

    @Test
    void realBearerTokenEnforcesImmediateLockAndInvalidTokenRejection() throws Exception {
        String token = tokens.issue(users.findById(memberId).orElseThrow()).value();
        mockMvc.perform(put(path()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isOk());
        jdbc.update("UPDATE [USER] SET account_status = 'LOCKED' WHERE user_id = ?", memberId);
        mockMvc.perform(put(path()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\":\"DISLIKE\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        mockMvc.perform(get(path()).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT reaction_type FROM [RECIPE_REACTION] WHERE recipe_id = ?", String.class, recipeId)).isEqualTo("LIKE");
    }

    @Test
    void generatedContractDocumentsPrivateOperationsAndProblemResponses() throws Exception {
        String operation = "$.paths['/api/v1/recipes/{recipeId}/reaction']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(operation + ".get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath(operation + ".put.requestBody.required").value(true))
                .andExpect(jsonPath(operation + ".delete.responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/RecipeReactionResponse"))
                .andExpect(jsonPath(operation + ".put.responses['403'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.components.schemas.SetRecipeReactionRequest.required[0]").value("reactionType"));
    }

    @Test
    void simultaneousFirstVotesDoNotCreateDuplicateRows() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Integer> vote = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
            return set(memberId, "LIKE").andReturn().getResponse().getStatus();
        };
        try {
            var first = pool.submit(vote);
            var second = pool.submit(vote);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(30, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(count()).isEqualTo(1);
        } finally {
            start.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private String path() { return "/api/v1/recipes/" + recipeId + "/reaction"; }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM [RECIPE_REACTION] WHERE recipe_id = ?", Integer.class, recipeId); }
    private org.springframework.test.web.servlet.ResultActions set(long userId, String type) throws Exception {
        return mockMvc.perform(put(path()).with(principal(userId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reactionType\":\"" + type + "\"}"));
    }
    private RequestPostProcessor principal(long id) {
        return user(Long.toString(id)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
    private long insertUser(String suffix, String role, String state) {
        jdbc.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, N'Issue56 test', ?, ?, 1)",
                "issue56-" + suffix + "@test.local", role, state);
        return jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, "issue56-" + suffix + "@test.local");
    }
}
