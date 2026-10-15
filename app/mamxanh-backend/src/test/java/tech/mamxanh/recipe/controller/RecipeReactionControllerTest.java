package tech.mamxanh.recipe.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.recipe.dto.response.RecipeReactionResponse;
import tech.mamxanh.recipe.entity.RecipeReactionType;
import tech.mamxanh.recipe.service.RecipeReactionService;

@WebMvcTest(RecipeReactionController.class)
@Import(GlobalExceptionHandler.class)
class RecipeReactionControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private RecipeReactionService service;
    @MockitoBean private UserRepository userRepository;

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveIdBeforeCallingService(long id) throws Exception {
        String path = "/api/v1/recipes/" + id + "/reaction";
        mvc.perform(get(path).with(user("member")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put(path).with(user("member")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(delete(path).with(user("member")).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"reactionType\":null}", "{\"reactionType\":\"LOVE\"}", "{"})
    void rejectsInvalidPayloadBeforeCallingService(String payload) throws Exception {
        mvc.perform(put("/api/v1/recipes/1/reaction").with(user("member")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsSetAndRemoveResultsWithoutCachingPersonalChoice() throws Exception {
        var selected = new RecipeReactionResponse(1L, RecipeReactionType.LIKE, 10, 1, 11, BigDecimal.valueOf(91));
        var removed = new RecipeReactionResponse(1L, null, 9, 1, 10, BigDecimal.valueOf(90));
        when(service.get(1)).thenReturn(selected);
        when(service.set(1, RecipeReactionType.LIKE)).thenReturn(selected);
        when(service.remove(1)).thenReturn(removed);
        mvc.perform(get("/api/v1/recipes/1/reaction").with(user("member")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.currentReaction").value("LIKE"));
        mvc.perform(put("/api/v1/recipes/1/reaction").with(user("member")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.likes").value(10)).andExpect(jsonPath("$.likePercentage").value(91));
        mvc.perform(delete("/api/v1/recipes/1/reaction").with(user("member")).with(csrf()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.currentReaction").doesNotExist()).andExpect(jsonPath("$.reactionCount").value(10));
    }
}
