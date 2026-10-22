package tech.mamxanh.shopping.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import tech.mamxanh.shopping.dto.response.ShoppingListResponse.Item;
import tech.mamxanh.shopping.service.ShoppingListService;

@WebMvcTest(ShoppingListController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class,
        SpringDocConfiguration.class, SpringDocSpecPropertiesConfiguration.class, SpringDocWebMvcConfiguration.class})
class ShoppingListControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ShoppingListService service;
    @MockitoBean tech.mamxanh.shopping.service.ShoppingListCreationService creation;
    @MockitoBean tech.mamxanh.shopping.service.RecipeShoppingListService recipeLists;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AccountStatusJwtAuthenticationConverter converter;

    @Test void allChecklistOperationsRequireAuthentication() throws Exception {
        mvc.perform(post("/api/v1/shopping-lists/from-meal-plan")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/shopping-lists")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/shopping-lists/options")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/shopping-lists/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/shopping-lists/1/items")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/shopping-lists/1/items/2")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/shopping-lists/1/items/2/bought")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/shopping-lists/1/items/2")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/shopping-lists/1/items?bought=true")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[]}",
            "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[null]}", "{\"weekStartDate\":\"invalid\"}"})
    void invalidCreationPayloadIsRejectedBeforeTheService(String body) throws Exception {
        mvc.perform(post("/api/v1/shopping-lists/from-meal-plan").with(user("17").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(creation);
    }

    @Test void creationReturnsPersistedChecklistAndLocation() throws Exception {
        var list = new tech.mamxanh.shopping.dto.response.ShoppingListResponse(28,"Đi chợ",null,null,java.util.List.of());
        when(creation.createFromMealPlan(any())).thenReturn(new tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse(
                list,java.util.List.of(),java.util.List.of()));
        mvc.perform(post("/api/v1/shopping-lists/from-meal-plan").with(user("17").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"weekStartDate\":\"2026-10-19\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location","/api/v1/shopping-lists/28"))
                .andExpect(jsonPath("$.shoppingList.id").value(28)).andExpect(jsonPath("$.warnings").isEmpty())
                .andExpect(jsonPath("$.incompatibleItemIds").isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {
        "{}", "{\"name\":\"\",\"quantity\":1,\"unitId\":1}",
        "{\"name\":\"Khăn\",\"quantity\":0,\"unitId\":1}",
        "{\"name\":\"Khăn\",\"quantity\":0.001,\"unitId\":1}",
        "{\"name\":\"Khăn\",\"quantity\":100000000,\"unitId\":1}",
        "{\"name\":\"Khăn\",\"quantity\":2,\"unitId\":0}"})
    void invalidManualItemsReturnFieldProblems(String body) throws Exception {
        mvc.perform(post("/api/v1/shopping-lists/1/items").with(user("17").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
        verifyNoInteractions(service);
    }

    @Test void addReturnsCreatedItemAndLocation() throws Exception {
        when(service.add(eq(1L), any())).thenReturn(new Item(2L, null, "Khăn giấy", "Nhóm khác",
                new BigDecimal("2"), 7, "cuộn", "cuộn", false));
        mvc.perform(post("/api/v1/shopping-lists/1/items").with(user("17").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Khăn giấy\",\"quantity\":2,\"unitId\":7}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/shopping-lists/1/items/2"))
                .andExpect(jsonPath("$.bought").value(false)).andExpect(jsonPath("$.unitCode").value("cuộn"));
    }

    @Test void validatesUpdatesAndExplicitBoughtState() throws Exception {
        mvc.perform(put("/api/v1/shopping-lists/1/items/2").with(user("17").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-1,\"unitId\":7}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/shopping-lists/1/items/2/bought").with(user("17").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/shopping-lists/1/items/2/bought").with(user("17").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bought\":false}"))
                .andExpect(status().isOk());
        verify(service).setBought(1, 2, false);
    }

    @Test void mapsServiceErrorsToExistingProblemConvention() throws Exception {
        when(service.get(1)).thenThrow(new AppException(ErrorCode.NOT_FOUND));
        mvc.perform(get("/api/v1/shopping-lists/1").with(user("17").roles("CUSTOMER")))
                .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test void cleanupRequiresExplicitTrueAndNeverDeletesEverything() throws Exception {
        mvc.perform(delete("/api/v1/shopping-lists/1/items?bought=true").with(user("17").roles("CUSTOMER")))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/shopping-lists/1/items?bought=false").with(user("17").roles("CUSTOMER")))
                .andExpect(status().isBadRequest());
        verify(service, times(1)).clearBought(1);
    }

    @Test void generatedContractDocumentsOnlyImplementedChecklistRoutes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/shopping-lists/{listId}/items'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/shopping-lists/{listId}/items'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/shopping-lists/{listId}/items/{itemId}/bought'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.components.schemas.ShoppingListItemResponse.properties.bought.type").value("boolean"))
                .andExpect(jsonPath("$.paths['/api/v1/shopping-lists/from-recipes'].post.operationId").value("addRecipesToPersonalShoppingList"))
                .andExpect(jsonPath("$.components.schemas.ShoppingListItemResponse.properties.sources.type").value("array"));
    }
}
