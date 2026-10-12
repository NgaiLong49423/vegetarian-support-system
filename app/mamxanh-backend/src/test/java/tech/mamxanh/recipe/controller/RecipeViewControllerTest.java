package tech.mamxanh.recipe.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import tech.mamxanh.common.config.OpenApiConfig;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.recipe.dto.response.RecipeViewResponse;
import tech.mamxanh.recipe.service.RecipeViewService;

@WebMvcTest(RecipeViewController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocSpecPropertiesConfiguration.class,
        SpringDocWebMvcConfiguration.class})
@ActiveProfiles("test")
@EnableConfigurationProperties(SpringDocConfigProperties.class)
class RecipeViewControllerTest {
    private static final String GUEST = "abcdefab-1234-4567-89ab-123456789abc";
    @Autowired MockMvc mvc;
    @MockitoBean RecipeViewService service;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AccountStatusJwtAuthenticationConverter converter;

    @Test void guestCanPostWithoutLoginAndResponseIsNotCached() throws Exception {
        when(service.record(1, null, GUEST)).thenReturn(new RecipeViewResponse(1, true, 4));
        mvc.perform(post("/api/v1/recipes/1/views").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anonymousViewerId\":\"" + GUEST + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(true))
                .andExpect(jsonPath("$.viewCount").value(4)).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void memberIdentityComesFromJwtEvenWhenBodyClaimsAnotherUser() throws Exception {
        when(service.record(1, 7L, null)).thenReturn(new RecipeViewResponse(1, false, 4));
        mvc.perform(post("/api/v1/recipes/1/views").with(jwt().jwt(j -> j.subject("7")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":999,\"viewCount\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(false));
        verify(service).record(1, 7L, null);
    }

    @Test void zeroAndNegativeRecipeIdsAreBadRequestsBeforeService() throws Exception {
        for (int id : new int[]{0, -1}) {
            mvc.perform(post("/api/v1/recipes/" + id + "/views").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        verifyNoInteractions(service);
    }

    @Test void malformedJsonAndGuestUuidAreRejected() throws Exception {
        for (String body : new String[]{"{", "{\"anonymousViewerId\":\"bad\"}"}) {
            mvc.perform(post("/api/v1/recipes/1/views").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test void invalidBearerIsNotDowngradedToGuest() throws Exception {
        when(decoder.decode("invalid")).thenThrow(new BadJwtException("invalid test token"));
        mvc.perform(post("/api/v1/recipes/1/views").header("Authorization", "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void unavailableLockReturnsProblemInsteadOfSuccess() throws Exception {
        when(service.record(eq(1L), isNull(), eq(GUEST))).thenThrow(new AppException(ErrorCode.RECIPE_VIEW_UNAVAILABLE));
        mvc.perform(post("/api/v1/recipes/1/views").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anonymousViewerId\":\"" + GUEST + "\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("RECIPE_VIEW_UNAVAILABLE"));
    }

    @Test void generatedOpenApiDeclaresOptionalBearerAndViewResponse() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/views'].post.security[0]").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/views'].post.security[1].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/views'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/views'].post.responses['503']").exists())
                .andExpect(jsonPath("$.components.schemas.RecipeViewResponse.properties.counted.type").value("boolean"));
    }
}
