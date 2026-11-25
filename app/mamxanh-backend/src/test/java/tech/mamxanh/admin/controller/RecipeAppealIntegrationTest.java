package tech.mamxanh.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The author's side of an appeal on a hidden Recipe Post, on SQL Server (FR-28, BR-27, BR-28). */
class RecipeAppealIntegrationTest extends AbstractAppealIntegrationTest {
    private static final String NEW_COVER = "https://cdn.test/appeal-new-cover.jpg";
    private static final String MEDIA_BODY = """
            {"mediaItems":[{"blobUrl":"%s","mimeType":"image/jpeg","displayOrder":1,"isCover":true}]}
            """.formatted(NEW_COVER);

    @Test
    void authorSeesHideReasonWithoutReporter() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/{id}/appeal", recipeId).with(asAuthor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId))
                .andExpect(jsonPath("$.recipeId").value(recipeId))
                .andExpect(jsonPath("$.recipeTitle").value("Canh nấm hạt sen"))
                .andExpect(jsonPath("$.hideReason").value(HIDE_REASON))
                .andExpect(jsonPath("$.appealStatus").doesNotExist())
                .andExpect(jsonPath("$.reporter").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void authorSubmitsAppealOnce() throws Exception {
        submit(asAuthor(), MESSAGE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appealStatus").value("PENDING"))
                .andExpect(jsonPath("$.appealMessage").value(MESSAGE))
                .andExpect(jsonPath("$.appealedAt").exists());
        assertThat(appealStatus(reportId)).isEqualTo("PENDING");
        assertThat(recipeStatus()).isEqualTo("HIDDEN");

        submit(asAuthor(), MESSAGE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPEAL_NOT_ALLOWED"));
    }

    @Test
    void concurrentSubmitOnlyOneWins() throws Exception {
        assertConcurrentOnlyOneWins(() -> submit(asAuthor(), MESSAGE), 201);
        assertThat(appealStatus(reportId)).isEqualTo("PENDING");
    }

    @Test
    void concurrentResubmitOnlyOneWins() throws Exception {
        setAppealStatus(reportId, "EDIT_ALLOWED");

        assertConcurrentOnlyOneWins(() -> resubmit(asAuthor()), 200);
        assertThat(appealStatus(reportId)).isEqualTo("RESUBMITTED");
    }

    @Test
    void appealUsesTheLatestHideReport() throws Exception {
        long latest = insertHideReport(recipeId, "2026-11-21T08:00:00");

        submit(asAuthor(), MESSAGE).andExpect(status().isCreated()).andExpect(jsonPath("$.reportId").value(latest));
        assertThat(appealStatus(reportId)).isNull();
    }

    @Test
    void messageLengthValidated() throws Exception {
        submit(asAuthor(), "  ngắn quá  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        submit(asAuthor(), "a".repeat(1001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(appealStatus(reportId)).isNull();
    }

    @Test
    void otherExpertCannotSeeOrAppeal() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/{id}/appeal", recipeId).with(principal(otherExpertId, "ROLE_EXPERT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_EDIT_NOT_ALLOWED"))
                .andExpect(jsonPath("$.hideReason").doesNotExist());
        submit(principal(otherExpertId, "ROLE_EXPERT"), MESSAGE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_EDIT_NOT_ALLOWED"));
        assertThat(appealStatus(reportId)).isNull();
    }

    @Test
    void customerAndGuestRejected() throws Exception {
        submit(principal(customerId, "ROLE_CUSTOMER"), MESSAGE).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/recipes/{id}/appeal", recipeId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertThat(appealStatus(reportId)).isNull();
    }

    @Test
    void publishedOrMissingRecipeHasNoAppeal() throws Exception {
        long published = insertRecipe(authorId, "Gỏi cuốn rau củ", "PUBLISHED");

        mockMvc.perform(get("/api/v1/recipes/{id}/appeal", published).with(asAuthor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPEAL_NOT_FOUND"));
        submit(asAuthor(), published, MESSAGE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPEAL_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/recipes/{id}/appeal", 999_999_999L).with(asAuthor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
    }

    @Test
    void resubmitOnlyWhenEditAllowed() throws Exception {
        setAppealStatus(reportId, "PENDING");
        resubmit(asAuthor())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPEAL_STATE_INVALID"));

        setAppealStatus(reportId, "EDIT_ALLOWED");
        resubmit(asAuthor())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appealStatus").value("RESUBMITTED"))
                .andExpect(jsonPath("$.resubmittedAt").exists());
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
    }

    @Test
    void lockedOrDemotedAuthorCannotResubmit() throws Exception {
        setAppealStatus(reportId, "EDIT_ALLOWED");
        jdbcTemplate.update("UPDATE [USER] SET account_status = 'LOCKED' WHERE user_id = ?", authorId);
        resubmit(asAuthor()).andExpect(status().isForbidden());

        jdbcTemplate.update("UPDATE [USER] SET account_status = 'ACTIVE', role = 'CUSTOMER' WHERE user_id = ?", authorId);
        resubmit(principal(authorId, "ROLE_EXPERT")).andExpect(status().isForbidden());
        assertThat(appealStatus(reportId)).isEqualTo("EDIT_ALLOWED");
    }

    @Test
    void editAllowedAuthorUpdatesHiddenRecipeWhichStaysHidden() throws Exception {
        setAppealStatus(reportId, "EDIT_ALLOWED");

        mockMvc.perform(put("/api/v1/recipes/{id}", recipeId).with(asAuthor())
                        .contentType(MediaType.APPLICATION_JSON).content(update("Canh nấm hạt sen (đã sửa nhãn)")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Canh nấm hạt sen (đã sửa nhãn)"))
                .andExpect(jsonPath("$.status").value("HIDDEN"));
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
        assertThat(appealStatus(reportId)).isEqualTo("EDIT_ALLOWED");
    }

    @Test
    void editAllowedAuthorReplacesMedia() throws Exception {
        setAppealStatus(reportId, "EDIT_ALLOWED");

        mockMvc.perform(put("/api/v1/recipes/{id}/media", recipeId).with(asAuthor())
                        .contentType(MediaType.APPLICATION_JSON).content(MEDIA_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].blobUrl").value(NEW_COVER));
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
    }

    @Test
    void pendingOrResubmittedAppealStillBlocksEdit() throws Exception {
        for (String state : new String[] {null, "PENDING", "RESUBMITTED"}) {
            if (state != null) {
                setAppealStatus(reportId, state);
            }
            mockMvc.perform(put("/api/v1/recipes/{id}", recipeId).with(asAuthor())
                            .contentType(MediaType.APPLICATION_JSON).content(update("Không được sửa")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));
            mockMvc.perform(put("/api/v1/recipes/{id}/media", recipeId).with(asAuthor())
                            .contentType(MediaType.APPLICATION_JSON).content(MEDIA_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));
        }
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, recipeId))
                .isEqualTo("Canh nấm hạt sen");
    }

    @Test
    void editUnlockComesOnlyFromTheLatestHideDecision() throws Exception {
        setAppealStatus(reportId, "EDIT_ALLOWED");
        insertHideReport(recipeId, "2026-11-22T08:00:00");

        mockMvc.perform(put("/api/v1/recipes/{id}", recipeId).with(asAuthor())
                        .contentType(MediaType.APPLICATION_JSON).content(update("Không được sửa")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));
        mockMvc.perform(put("/api/v1/recipes/{id}/media", recipeId).with(asAuthor())
                        .contentType(MediaType.APPLICATION_JSON).content(MEDIA_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));
    }

    @Test
    void editAllowedOnAnotherPostDoesNotUnlockThisOne() throws Exception {
        long otherHidden = insertRecipe(authorId, "Gỏi cuốn rau củ", "HIDDEN");
        long otherReport = insertHideReport(otherHidden, "2026-11-20T09:00:00");
        setAppealStatus(otherReport, "EDIT_ALLOWED");

        mockMvc.perform(put("/api/v1/recipes/{id}", recipeId).with(asAuthor())
                        .contentType(MediaType.APPLICATION_JSON).content(update("Không được sửa")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));
    }

    private String update(String title) {
        jdbcTemplate.update("IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = ?) "
                + "INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, 'Appeal fixture', '2026-10-05')",
                INGREDIENT_NAME, INGREDIENT_NAME);
        long ingredient = jdbcTemplate.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class,
                INGREDIENT_NAME);
        int gram = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        return """
                {"title":"%s","description":"Mô tả cập nhật","instructions":"Nấu món này cùng gia vị đến khi thấm.",
                 "dishCategory":"BRAISED","vegetarianType":"VEGAN","difficulty":"EASY","servings":2,"prepTimeMin":10,
                 "cookTimeMin":15,"youtubeUrl":"","ingredients":[{"ingredientId":%d,"unitId":%d,"quantity":300}]}
                """.formatted(title, ingredient, gram);
    }

    /** Two identical requests released together: the row lock lets exactly one through, the other sees its result. */
    private static void assertConcurrentOnlyOneWins(Callable<ResultActions> call, int successStatus) throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return call.call().andReturn().getResponse();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS).getStatus());
            }
            assertThat(statuses).containsExactlyInAnyOrder(successStatus, 409);
        } finally {
            executor.shutdownNow();
        }
    }

    private ResultActions submit(RequestPostProcessor who, String message) throws Exception {
        return submit(who, recipeId, message);
    }

    private ResultActions submit(RequestPostProcessor who, long recipe, String message) throws Exception {
        String body = "{\"message\":\"" + message + "\"}";
        return mockMvc.perform(post("/api/v1/recipes/{id}/appeal", recipe).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions resubmit(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(post("/api/v1/recipes/{id}/appeal/resubmit", recipeId).with(who));
    }
}
