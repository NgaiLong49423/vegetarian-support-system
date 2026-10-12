package tech.mamxanh.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.common.config.MethodSecurityConfiguration;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.IngredientNutritionResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import org.springframework.http.HttpStatus;
import tech.mamxanh.admin.service.AdminCatalogService;
import tech.mamxanh.auth.repository.UserRepository;

@WebMvcTest(AdminCatalogController.class)
@Import({MethodSecurityConfiguration.class, GlobalExceptionHandler.class})
class AdminCatalogControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminCatalogService catalogService;
    @MockitoBean private UserRepository userRepository;

    @Test void rejectsCustomerFromAdminCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ingredients").with(user("member").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/units").with(user("member").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/ingredient-unit-conversions").with(user("member").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/admin/units/1").with(user("member").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/ingredients/1/nutrition").with(user("member").roles("CUSTOMER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/ingredients/1/nutrition-support").with(user("member").roles("CUSTOMER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test void createsIngredientForAdministrator() throws Exception {
        when(catalogService.createIngredient(any())).thenReturn(new IngredientResponse(1L, "Chuối tây", "Trái cây", "USDA FoodData Central", null, LocalDate.of(2026, 9, 1), false, true));
        String request = """
                {"name":"Chuối tây","ingredientGroup":"Trái cây","sourceName":"USDA FoodData Central","referenceDate":"2026-09-01"}
                """;
        mockMvc.perform(post("/api/v1/admin/ingredients").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Chuối tây"));
    }

    @Test void rejectsNonPositiveConversionAtHttpBoundary() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ingredients/1/unit-conversions/2").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gramsPerUnit\":0,\"approximate\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test void rejectsConversionWithMoreFractionDigitsThanTheDatabaseStores() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ingredients/1/unit-conversions/2").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gramsPerUnit\":0.001,\"approximate\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test void acceptsConversionAtTheSmallestStoredScaleAtHttpBoundary() throws Exception {
        when(catalogService.createConversion(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(2), any()))
                .thenReturn(new tech.mamxanh.nutrition.dto.response.ConversionResponse(1L, 2, new BigDecimal("0.01"), true, true));

        mockMvc.perform(post("/api/v1/admin/ingredients/1/unit-conversions/2").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gramsPerUnit\":0.01,\"approximate\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gramsPerUnit").value(0.01));
    }

    @Test void searchesIngredientsForAdministrator() throws Exception {
        when(catalogService.listIngredients("đậu")).thenReturn(List.of(
                new IngredientResponse(2L, "Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.of(2026, 9, 1), false, true)));

        mockMvc.perform(get("/api/v1/admin/ingredients").param("query", "đậu").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Đậu phụ"));
    }

    @Test void supportsPagedCatalogSearchAndFiltersForAdministrator() throws Exception {
        when(catalogService.searchIngredients("đậu", "Đậu hạt", true, false, 0, 20))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/ingredients/search").param("query", "đậu")
                        .param("ingredientGroup", "Đậu hạt").param("active", "true")
                        .param("nutritionSupported", "false").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test void returnsBadRequestWhenIngredientSearchPageSizeIsOutsideBounds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ingredients/search").param("size", "101")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test void administratorCanUpdateNutritionAndToggleSupportThroughSeparateOperations() throws Exception {
        var nutrition = new IngredientNutritionResponse(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        when(catalogService.updateNutrition(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(new IngredientResponse(1L, "Đậu phụ", "Đậu hạt", "USDA FoodData Central",
                        "https://fdc.nal.usda.gov/", LocalDate.of(2026, 9, 1), false, true, nutrition));
        when(catalogService.setNutritionSupport(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(new IngredientResponse(1L, "Đậu phụ", "Đậu hạt", "USDA FoodData Central",
                        "https://fdc.nal.usda.gov/", LocalDate.of(2026, 9, 1), true, true, nutrition));

        String values = """
                {"energyKcalPer100g":10,"proteinGramsPer100g":1,"carbohydrateGramsPer100g":1,
                 "totalFatGramsPer100g":1,"fiberGramsPer100g":1,"calciumMgPer100g":1,
                 "ironMgPer100g":1,"vitaminB12MicrogramsPer100g":1,"zincMgPer100g":1}
                """;
        mockMvc.perform(put("/api/v1/admin/ingredients/1/nutrition").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(values))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nutrition.energyKcalPer100g").value(10))
                .andExpect(jsonPath("$.data.nutritionSupported").value(false));

        mockMvc.perform(patch("/api/v1/admin/ingredients/1/nutrition-support").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nutritionSupported").value(true));
    }

    @Test void createsUnitForAdministrator() throws Exception {
        when(catalogService.createUnit(any())).thenReturn(
                new UnitResponse(1, "g", "gam", MeasurementDimension.MASS, BigDecimal.ONE, true));

        mockMvc.perform(post("/api/v1/admin/units").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"g\",\"name\":\"gam\",\"dimension\":\"MASS\",\"baseFactor\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dimension").value("MASS"));
    }

    @Test void rejectsHardDeleteAndRequiresSoftDisableInstead() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "HARD_DELETE_NOT_SUPPORTED", "Không hỗ trợ xóa vĩnh viễn nguyên liệu; hãy chuyển sang trạng thái ngừng sử dụng."))
                .when(catalogService).deleteIngredient(1L);

        mockMvc.perform(delete("/api/v1/admin/ingredients/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HARD_DELETE_NOT_SUPPORTED"));
    }

    @Test void deactivatesIngredientForAdministrator() throws Exception {
        when(catalogService.setIngredientStatus(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(
                new IngredientResponse(1L, "Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.of(2026, 9, 1), false, false));

        mockMvc.perform(patch("/api/v1/admin/ingredients/1/status").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
