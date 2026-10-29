package tech.mamxanh.mealplan.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import tech.mamxanh.mealplan.service.MealPlanNutritionService;
import tech.mamxanh.mealplan.service.MealPlanNutritionAiService;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Day;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Target;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Entry;
import tech.mamxanh.mealplan.dto.request.MoveMealPlanEntryRequest;

@WebMvcTest({MealPlanController.class, MealPlanReadController.class, AiMenuController.class})
@ActiveProfiles("test")
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class,
        SpringDocConfiguration.class, SpringDocSpecPropertiesConfiguration.class, SpringDocWebMvcConfiguration.class})
class MealPlanControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private MealPlanService service;
    @MockitoBean private MealPlanReadService reads;
    @MockitoBean private MealPlanNutritionService nutrition;
    @MockitoBean private MealPlanNutritionAiService nutritionAi;
    @MockitoBean private tech.mamxanh.mealplan.service.AiMenuService aiMenus;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private AccountStatusJwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void generatedOpenApiDocumentsAtomicAiMenuApply() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/ai-menu'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/ai-menu'].post.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.ApplyAiWeeklyMenuRequest.properties.slots.minItems").value(21))
                .andExpect(jsonPath("$.components.schemas.ApplyAiWeeklyMenuRequest.properties.slots.maxItems").value(21));
    }

    @Test
    void savesServingsAndPublishesTheGeneratedContract() throws Exception {
        var week = LocalDate.of(2026, 10, 12);
        var quantity = new java.math.BigDecimal("1.5");
        when(service.updateServings(week, 31, quantity)).thenReturn(new MealPlanWeekResponse.Entry(31, week, "LUNCH",
                quantity, 1, "Món thử", null, "SOUP", 25, false, null));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/meal-plans/2026-10-12/entries/31/servings")
                        .with(user("17").roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content("{\"plannedServings\":1.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plannedServings").value(1.5));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries/{entryId}/servings'].patch.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries/{entryId}/servings'].patch.responses['200']").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateMealPlanServingsRequest.properties.plannedServings.minimum").value(0.5))
                .andExpect(jsonPath("$.components.schemas.UpdateMealPlanServingsRequest.properties.plannedServings.maximum").value(10))
                .andExpect(jsonPath("$.components.schemas.UpdateMealPlanServingsRequest.properties.plannedServings.multipleOf").value(0.5));
    }

    @Test
    void publishesNutritionAndPdfRoutesAndRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/meal-plans/2026-10-12/nutrition").param("date", "2026-10-12"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs").with(user("17").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/nutrition'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/nutrition/report'].get.responses['200'].content['application/pdf']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/nutrition/recommendations/{entryId}'].post.responses['200']").exists());
    }

    @Test
    void exportsUnifiedDailyAndWeeklyNutritionTables() throws Exception {
        var monday = LocalDate.of(2026, 10, 12);
        when(nutrition.analyze(monday, monday)).thenReturn(report(monday, "DAY", monday, List.of(day(monday))));
        when(nutrition.analyze(monday, null)).thenReturn(report(monday, "WEEK", null,
                java.util.stream.IntStream.range(0, 7).mapToObj(monday::plusDays).map(MealPlanControllerTest::day).toList()));

        byte[] daily = mvc.perform(get("/api/v1/meal-plans/2026-10-12/nutrition/report").param("date", "2026-10-12")
                        .with(user("17").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        try (var pdf = org.apache.pdfbox.Loader.loadPDF(daily)) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(pdf.getPage(0).getMediaBox().getWidth())
                    .isLessThan(pdf.getPage(0).getMediaBox().getHeight());
            String extracted = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
            assertThat(extracted).contains("BÁO CÁO DINH DƯỠNG NGÀY", "Chỉ tiêu / bữa", "Kế hoạch / món",
                    "Mức tham khảo", "Chênh lệch / ghi chú", "THỰC ĐƠN DỰ KIẾN VÀ CẢNH BÁO",
                    "Bữa sáng", "Bữa trưa", "Bữa tối", "Nấu thử", "Chưa xếp món", "Tạo lúc:");
        }

        byte[] weekly = mvc.perform(get("/api/v1/meal-plans/2026-10-12/nutrition/report")
                        .with(user("17").roles("CUSTOMER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var pdf = org.apache.pdfbox.Loader.loadPDF(weekly)) {
            assertThat(pdf.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            var stripper = new org.apache.pdfbox.text.PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                assertThat(pdf.getPage(page - 1).getMediaBox().getWidth())
                        .isGreaterThan(pdf.getPage(page - 1).getMediaBox().getHeight());
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertThat(stripper.getText(pdf)).contains("Chỉ tiêu / bữa", "Trung bình", "Ngày đạt");
            }
            stripper.setStartPage(1);
            stripper.setEndPage(pdf.getNumberOfPages());
            assertThat(stripper.getText(pdf)).contains("BÁO CÁO DINH DƯỠNG TUẦN", "12/10", "18/10",
                    "THỰC ĐƠN DỰ KIẾN VÀ CẢNH BÁO", "Bữa sáng", "Bữa trưa", "Bữa tối",
                    "Chưa xếp món", "Nấu thử", "Tạo lúc:");
        }
    }

    @Test
    void continuesLongWeeklyMealRowsWithoutLosingRecipesOrWarnings() throws Exception {
        var monday = LocalDate.of(2026, 10, 12);
        var days = java.util.stream.IntStream.range(0, 7).mapToObj(dayIndex -> {
            var date = monday.plusDays(dayIndex);
            var entries = java.util.stream.IntStream.range(0, 12).mapToObj(entryIndex ->
                    new Entry(entryIndex + 1, date, "LUNCH", entryIndex + 10,
                            "Món " + dayIndex + "-" + entryIndex + " với tên dài để kiểm tra xuống dòng trong bảng",
                            java.math.BigDecimal.TWO, entryIndex != 0,
                            entryIndex == 0 ? List.of("nguyên liệu thiếu dữ liệu") : List.of(),
                            java.util.Map.of())).toList();
            return new Day(date, java.util.Map.of(), entries);
        }).toList();
        when(nutrition.analyze(monday, null)).thenReturn(report(monday, "WEEK", null, days));

        byte[] bytes = mvc.perform(get("/api/v1/meal-plans/2026-10-12/nutrition/report")
                        .with(user("17").roles("CUSTOMER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isGreaterThan(2);
            var stripper = new org.apache.pdfbox.text.PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertThat(stripper.getText(pdf)).contains("Chỉ tiêu / bữa", "T2", "CN");
            }
            stripper.setStartPage(1);
            stripper.setEndPage(pdf.getNumberOfPages());
            String extracted = stripper.getText(pdf);
            assertThat(extracted).contains("Món 0-0", "Món 6-11", "Chưa xếp món", "Tạo lúc:");
            assertThat(extracted.replaceAll("\\s+", "")).contains("nguyênliệuthiếudữliệu");
        }
    }

    private static MealPlanNutritionResponse report(LocalDate monday, String scope, LocalDate selected, List<Day> days) {
        var keys = List.of("ENERGY_KCAL", "PROTEIN_G", "CARBOHYDRATE_G", "TOTAL_FAT_G", "FIBER_G", "CALCIUM_MG", "IRON_MG", "VITAMIN_B12_MCG", "ZINC_MG");
        var targets = keys.stream().map(key -> new Target(key, key, java.math.BigDecimal.TEN, java.math.BigDecimal.TEN,
                "g", "FDA_DV", "FDA", "https://www.fda.gov", "current")).toList();
        var avg = keys.stream().collect(java.util.stream.Collectors.toMap(key -> key, key -> java.math.BigDecimal.ZERO));
        var attainment = keys.stream().collect(java.util.stream.Collectors.toMap(key -> key, key -> 0));
        return new MealPlanNutritionResponse(monday, scope, selected, "FDA Daily Values", "FDA", "https://www.fda.gov",
                tech.mamxanh.mealplan.service.MealPlanNutritionService.DISCLAIMER, targets, days, avg, attainment, List.of());
    }

    private static Day day(LocalDate date) {
        var nutrients = java.util.Map.of("ENERGY_KCAL", java.math.BigDecimal.valueOf(100));
        var entry = new Entry(1, date, "LUNCH", 12, "Nấu thử", java.math.BigDecimal.ONE, true, List.of(), nutrients);
        return new Day(date, nutrients, List.of(entry));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"plannedServings\":null}", "{\"plannedServings\":0}", "{\"plannedServings\":-1}", "{\"plannedServings\":0.49}", "{\"plannedServings\":1.25}", "{\"plannedServings\":10.5}"})
    void rejectsInvalidServingsPayload(String json) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/meal-plans/2026-10-12/entries/31/servings")
                        .with(user("17")).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void servingsUpdateRejectsGuestsAndForeignEntries() throws Exception {
        var path = "/api/v1/meal-plans/2026-10-12/entries/31/servings";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(path)
                .contentType(MediaType.APPLICATION_JSON).content("{\"plannedServings\":4}"))
                .andExpect(status().isUnauthorized());
        doThrow(new AppException(ErrorCode.ACCESS_DENIED)).when(service).updateServings(LocalDate.of(2026, 10, 12), 31, new java.math.BigDecimal("4"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(path).with(user("18"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"plannedServings\":4}"))
                .andExpect(status().isForbidden());
    }

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

    @Test
    void aiMenuRequiresAuthenticationAndRejectsInvalidModeBeforeCallingService() throws Exception {
        var path = "/api/v1/meal-plans/ai-menu/previews";
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekStartDate\":\"2026-10-12\",\"mode\":\"PROFILE\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(user("17")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekStartDate\":\"2026-10-12\",\"mode\":\"UNSUPPORTED\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(aiMenus);
    }

    @Test
    void generatedOpenApiPublishesBothAiMenuOperationsAndSecurity() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/ai-menu/previews'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/ai-menu/previews'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/ai-menu'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.components.schemas.AiMenuPreviewResponse.properties.meals").exists())
                .andExpect(jsonPath("$.components.schemas.ApplyAiMenuRequest.properties.meals.maxItems").value(1050))
                .andExpect(jsonPath("$.components.schemas.AiMenuPreviewResponse.properties.dailyTargets").exists())
                .andExpect(jsonPath("$.components.schemas.AiMenuPreviewResponse.properties.energyTargetKcal").exists())
                .andExpect(jsonPath("$.components.schemas.AiMenuPreviewResponse.properties.bmi").exists());
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
        var artifact = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/generated-mealplan-openapi.json"),
                artifact.getResponse().getContentAsString());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries/{entryId}/position'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/entries/{entryId}/position'].patch.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{weekStartDate}/export']").doesNotExist());
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

    @Test
    void movesEntriesAndReturnsDuplicateConflicts() throws Exception {
        var week = LocalDate.of(2026, 10, 12);
        var target = new MoveMealPlanEntryRequest(week.plusDays(1), "DINNER");
        when(service.moveEntry(week, 31, target)).thenReturn(new MealPlanWeekResponse.Entry(31,
                target.mealDate(), target.mealType(), new java.math.BigDecimal("2.5"), 1, "Món thử", null, "SOUP", 25, false, null));
        var body = "{\"mealDate\":\"2026-10-13\",\"mealType\":\"DINNER\"}";
        mvc.perform(patch("/api/v1/meal-plans/2026-10-12/entries/31/position").with(user("17"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mealDate").value("2026-10-13"))
                .andExpect(jsonPath("$.plannedServings").value(2.5));
        when(service.moveEntry(week, 31, target)).thenThrow(new AppException(ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE));
        mvc.perform(patch("/api/v1/meal-plans/2026-10-12/entries/31/position").with(user("17"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEAL_PLAN_RECIPE_DUPLICATE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"mealDate\":\"2026-10-13\",\"mealType\":\"SNACK\"}",
            "{\"mealDate\":\"bad\",\"mealType\":\"DINNER\"}"})
    void rejectsInvalidMoveBeforeService(String body) throws Exception {
        mvc.perform(patch("/api/v1/meal-plans/2026-10-12/entries/31/position").with(user("17"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void guestCannotMove() throws Exception {
        mvc.perform(patch("/api/v1/meal-plans/2026-10-12/entries/31/position")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void tombstoneNullFieldsAreSerializedAndTheirPropertiesArePublished() throws Exception {
        var monday = LocalDate.of(2026, 10, 12);
        var tombstone = new MealPlanWeekResponse.Entry(31, monday, "LUNCH", java.math.BigDecimal.ONE,
                1, null, null, null, null, true, "Công thức không còn khả dụng");
        when(reads.getWeek(monday)).thenReturn(new MealPlanWeekResponse(monday, java.util.List.of(tombstone)));
        mvc.perform(get("/api/v1/meal-plans").param("weekStartDate", monday.toString()).with(user("17")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"recipeTitle\":null")))
                .andExpect(jsonPath("$.entries[0].recipeTitle").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.entries[0].recipeDeleted").value(true));
        var spec = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var json = tools.jackson.databind.json.JsonMapper.builder().build().readTree(spec.getResponse().getContentAsString());
        var fieldTypes = java.util.Map.of("recipeTitle", "string", "recipeCoverUrl", "string",
                "dishCategory", "string", "totalTimeMinutes", "integer", "unavailableMessage", "string");
        for (var field : fieldTypes.entrySet()) {
            var schema = json.at("/components/schemas/MealPlanWeekEntry/properties/" + field.getKey());
            assertThat(schema.path("type").toString()).contains("\"" + field.getValue() + "\"");
        }
    }
}
