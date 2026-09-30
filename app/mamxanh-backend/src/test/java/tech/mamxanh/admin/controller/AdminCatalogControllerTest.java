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
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import org.springframework.http.HttpStatus;
import tech.mamxanh.nutrition.service.IngredientCatalogService;

@WebMvcTest(AdminCatalogController.class)
@Import({MethodSecurityConfiguration.class, GlobalExceptionHandler.class})
class AdminCatalogControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private IngredientCatalogService catalogService;

    @Test void rejectsCustomerFromAdminCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ingredients").with(user("member").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/units").with(user("member").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/ingredient-unit-conversions").with(user("member").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/admin/units/1").with(user("member").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test void createsIngredientForAdministrator() throws Exception {
        when(catalogService.createIngredient(any())).thenReturn(new IngredientResponse(1L, "Chuối tây", "Trái cây", "USDA", null, LocalDate.of(2026, 9, 1), false, true));
        String request = """
                {"name":"Chuối tây","ingredientGroup":"Trái cây","sourceName":"USDA","referenceDate":"2026-09-01"}
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

    @Test void acceptsAnyStrictlyPositiveConversionValueAtHttpBoundary() throws Exception {
        when(catalogService.createConversion(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(2), any()))
                .thenReturn(new tech.mamxanh.nutrition.dto.response.ConversionResponse(1L, 2, new BigDecimal("0.001"), true, true));

        mockMvc.perform(post("/api/v1/admin/ingredients/1/unit-conversions/2").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gramsPerUnit\":0.001,\"approximate\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gramsPerUnit").value(0.001));
    }

    @Test void searchesIngredientsForAdministrator() throws Exception {
        when(catalogService.listIngredients("đậu")).thenReturn(List.of(
                new IngredientResponse(2L, "Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.of(2026, 9, 1), false, true)));

        mockMvc.perform(get("/api/v1/admin/ingredients").param("query", "đậu").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Đậu phụ"));
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
                .when(catalogService).rejectHardDelete("nguyên liệu");

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
