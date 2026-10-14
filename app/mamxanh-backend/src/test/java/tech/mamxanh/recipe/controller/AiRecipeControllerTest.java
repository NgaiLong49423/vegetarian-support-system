package tech.mamxanh.recipe.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.recipe.dto.response.AiRecipeRecommendationResponse;
import tech.mamxanh.recipe.service.AiRecipeRecommendationService;

@WebMvcTest(AiRecipeController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class,
        SpringDocConfiguration.class, SpringDocSpecPropertiesConfiguration.class, SpringDocWebMvcConfiguration.class})
class AiRecipeControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AiRecipeRecommendationService service;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private AccountStatusJwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void recommendationsRequireAuthenticationAndValidIngredientIds() throws Exception {
        mvc.perform(post("/api/v1/recipes/ai/recommendations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientIds\":[1]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/recipes/ai/recommendations").with(user("17").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ingredientIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void generatedOpenApiContainsAuthenticatedAiContracts() throws Exception {
        when(service.recommend(List.of(1L))).thenReturn(new AiRecipeRecommendationResponse(List.of()));
        mvc.perform(post("/api/v1/recipes/ai/recommendations").with(user("17").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ingredientIds\":[1]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai/recommendations'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai/weekly-menu'].post.responses['200']").exists())
                .andExpect(jsonPath("$.components.schemas.AiRecipeRecommendationRequest.properties.ingredientIds.maxItems").value(20))
                .andExpect(jsonPath("$.components.schemas.AiWeeklyMenuPreviewResponse.properties.slots").exists());
    }
}
