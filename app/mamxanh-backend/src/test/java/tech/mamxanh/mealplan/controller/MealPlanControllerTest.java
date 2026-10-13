package tech.mamxanh.mealplan.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.MediaType;
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.service.MealPlanReadService;
import tech.mamxanh.mealplan.service.MealPlanService;

@WebMvcTest({MealPlanController.class, MealPlanReadController.class})
@ActiveProfiles("test")
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class,
        SpringDocConfiguration.class, SpringDocSpecPropertiesConfiguration.class, SpringDocWebMvcConfiguration.class})
class MealPlanControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private MealPlanService service;
    @MockitoBean private MealPlanReadService reads;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private AccountStatusJwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void opensAWeekWithoutARequestBody() throws Exception {
        var monday = LocalDate.of(2026, 10, 12);
        when(service.openWeek(monday)).thenReturn(new MealPlanWeekResponse(monday, List.of()));
        mvc.perform(put("/api/v1/meal-plans").param("weekStartDate", monday.toString()).with(user("17").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weekStartDate").value(monday.toString()))
                .andExpect(jsonPath("$.entries").isEmpty());
    }

    @Test
    void retainsTheExistingReadContract() throws Exception {
        var monday = LocalDate.of(2026, 10, 12);
        when(reads.getWeek(monday)).thenReturn(new MealPlanWeekResponse(monday, List.of()));
        mvc.perform(get("/api/v1/meal-plans").param("weekStartDate", monday.toString()).with(user("17")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries").isEmpty());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-date", "2026-02-30"})
    void rejectsMissingOrMalformedDate(String date) throws Exception {
        var request = put("/api/v1/meal-plans").with(user("17"));
        if (!date.isEmpty()) request.param("weekStartDate", date);
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void translatesTheMondayValidationError() throws Exception {
        when(service.openWeek(LocalDate.of(2026, 10, 13))).thenThrow(new AppException(ErrorCode.VALIDATION_FAILED));
        mvc.perform(put("/api/v1/meal-plans").param("weekStartDate", "2026-10-13").with(user("17")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesGuestBeforeInvokingTheService() throws Exception {
        mvc.perform(put("/api/v1/meal-plans").param("weekStartDate", "2026-10-12"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void publishesTheGeneratedContract() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].put.parameters[0].name").value("weekStartDate"))
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].put.requestBody").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].put.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].put.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].put.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].get.responses['400']").exists())
                .andExpect(jsonPath("$.components.schemas.MealPlanWeekResponse.properties.entries").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"mealDate\":\"2026-10-12\",\"mealType\":\"SNACK\",\"recipeId\":1}",
            "{\"mealDate\":\"2026-10-12\",\"mealType\":\"LUNCH\",\"recipeId\":0}",
            "{\"mealDate\":\"bad-date\",\"mealType\":\"LUNCH\",\"recipeId\":1}"
    })
    void rejectsInvalidEntryPayload(String json) throws Exception {
        mvc.perform(post("/api/v1/meal-plans/2026-10-12/entries").with(user("17"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void addsAndReplacesEntriesWithTheDocumentedStatuses() throws Exception {
        var week = LocalDate.of(2026, 10, 12);
        var entry = new MealPlanWeekResponse.Entry(31, week, "LUNCH", java.math.BigDecimal.ONE,
                1, "Món thử", null, "SOUP", 25, false, null);
        when(service.addEntry(week, new AddMealPlanEntryRequest(week, "LUNCH", 1L))).thenReturn(entry);
        when(service.replaceEntry(week, 31, 1)).thenReturn(entry);
        mvc.perform(post("/api/v1/meal-plans/2026-10-12/entries").with(user("17"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealDate\":\"2026-10-12\",\"mealType\":\"LUNCH\",\"recipeId\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.entryId").value(31))
                .andExpect(jsonPath("$.plannedServings").value(1));
        mvc.perform(put("/api/v1/meal-plans/2026-10-12/entries/31").with(user("17"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"recipeId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entryId").value(31));
    }

    @Test
    void removesWithNoResponseBody() throws Exception {
        mvc.perform(delete("/api/v1/meal-plans/2026-10-12/entries/31").with(user("17")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).removeEntry(LocalDate.of(2026, 10, 12), 31);
    }

    @Test
    void returnsTheDuplicateCodeAndOwnershipError() throws Exception {
        var week = LocalDate.of(2026, 10, 12);
        when(service.replaceEntry(week, 31, 1)).thenThrow(new AppException(ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE));
        mvc.perform(put("/api/v1/meal-plans/2026-10-12/entries/31").with(user("17"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"recipeId\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEAL_PLAN_RECIPE_DUPLICATE"));
        doThrow(new AppException(ErrorCode.ACCESS_DENIED)).when(service).removeEntry(week, 31);
        mvc.perform(delete("/api/v1/meal-plans/2026-10-12/entries/31").with(user("17")))
                .andExpect(status().isForbidden());
    }

    @Test
    void guestCannotCallAnyEntryMutation() throws Exception {
        mvc.perform(post("/api/v1/meal-plans/2026-10-12/entries").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/meal-plans/2026-10-12/entries/31").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/meal-plans/2026-10-12/entries/31")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void entryMutationsAppearInGeneratedOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries/{entryId}'].put.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries/{entryId}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.components.schemas.AddMealPlanEntryRequest.properties.recipeId.minimum").value(1));
    }
}
