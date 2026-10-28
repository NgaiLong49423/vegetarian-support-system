package tech.mamxanh.nutrition.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.nutrition.dto.response.IngredientNutritionResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientDetailResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogService;

@WebMvcTest(PublicIngredientBrowseController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocSpecPropertiesConfiguration.class,
        SpringDocWebMvcConfiguration.class})
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@ActiveProfiles("test")
class PublicIngredientBrowseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean IngredientCatalogService service;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AccountStatusJwtAuthenticationConverter converter;

    @Test void guestReadsNutritionWithoutAdministrativeMetadata() throws Exception {
        when(service.getPublicIngredient(42L)).thenReturn(new PublicIngredientDetailResponse(42L, "Phô mai", "Sữa", null,
                new IngredientNutritionResponse(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO,
                        null, null, null, null, new BigDecimal("0.0001"), null)));

        mvc.perform(get("/api/v1/ingredients/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phô mai"))
                .andExpect(jsonPath("$.nutrition.carbohydrateGramsPer100g").value(0))
                .andExpect(jsonPath("$.nutrition.totalFatGramsPer100g").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.nutrition.vitaminB12MicrogramsPer100g").value(0.0001))
                .andExpect(jsonPath("$.sourceName").doesNotExist())
                .andExpect(jsonPath("$.sourceUrl").doesNotExist())
                .andExpect(jsonPath("$.referenceDate").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$.nutritionSupported").doesNotExist());
    }

    @Test void unavailableIngredientReturnsPublicNotFoundProblem() throws Exception {
        when(service.getPublicIngredient(42L)).thenThrow(new ApiException(HttpStatus.NOT_FOUND,
                "INGREDIENT_NOT_FOUND", "Không tìm thấy nguyên liệu."));
        mvc.perform(get("/api/v1/ingredients/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
    }

    @Test void rejectsInvalidIdsBeforeServiceLookup() throws Exception {
        for (String id : new String[]{"0", "-1", "not-an-id"}) {
            mvc.perform(get("/api/v1/ingredients/" + id)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test void generatedContractDeclaresPublicReadAndMissingIngredientResponse() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/ingredients/{ingredientId}'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/ingredients/{ingredientId}'].get.responses['200'].content['*/*'].schema['$ref']").value("#/components/schemas/PublicIngredientDetailResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/ingredients/{ingredientId}'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ingredients/{ingredientId}'].get.responses['404'].content['application/problem+json'].schema['$ref']").value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.components.schemas.PublicIngredientDetailResponse.properties.nutrition").exists())
                .andExpect(jsonPath("$.components.schemas.PublicIngredientDetailResponse.properties.sourceName").doesNotExist());
    }
}
