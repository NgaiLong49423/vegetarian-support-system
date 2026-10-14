package tech.mamxanh.mealplan.controller;

import java.time.LocalDate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.math.BigDecimal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.ReplaceMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.UpdateMealPlanServingsRequest;
import tech.mamxanh.mealplan.dto.request.MoveMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.ApplyAiWeeklyMenuRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionRecommendationsResponse;
import tech.mamxanh.mealplan.service.MealPlanService;
import tech.mamxanh.mealplan.service.MealPlanNutritionService;
import tech.mamxanh.mealplan.service.MealPlanNutritionAiService;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Day;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse.Target;

@RestController
@RequestMapping("/api/v1/meal-plans")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(responseCode = "400", description = "Invalid date, meal type or request fields.",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "401", description = "Authentication is required.",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "403", description = "An active Member and ownership of the entry are required.",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
public class MealPlanController {
    private final MealPlanService service;
    private final MealPlanNutritionService nutrition;
    private final MealPlanNutritionAiService nutritionAi;

    public MealPlanController(MealPlanService service, MealPlanNutritionService nutrition,
            MealPlanNutritionAiService nutritionAi) {
        this.service = service;
        this.nutrition = nutrition;
        this.nutritionAi = nutritionAi;
    }

    @Operation(summary = "Apply a confirmed AI weekly menu to the member's plan",
            description = "Requires an active Pro Member. Atomically adds exactly one recipe to each of 21 day/meal positions and preserves existing "
                    + "entries. If any recipe is no longer public or collides with the same recipe in an existing "
                    + "day/meal, the request fails without writing any entries.")
    @ApiResponse(responseCode = "200", description = "The updated weekly plan.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.class)))
    @ApiResponse(responseCode = "404", description = "A selected recipe is no longer published.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The selected recipe already exists in a target day/meal.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "An active Pro subscription is required.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{weekStartDate}/ai-menu")
    public MealPlanWeekResponse applyAiMenu(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @Valid @RequestBody ApplyAiWeeklyMenuRequest request) {
        return service.applyAiMenu(weekStartDate, request);
    }

    @Operation(summary = "Create or open the current member's weekly meal plan",
            description = "Idempotent for the authenticated member and Monday weekStartDate. "
                    + "Preserves existing entries. No request body is required; the owner is derived from authentication.")
    @ApiResponse(responseCode = "200", description = "The existing or newly created week.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.class)))
    @ApiResponse(responseCode = "400", description = "The date is missing, malformed or is not Monday.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentication is required.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "An active Member account is required.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping
    public MealPlanWeekResponse openWeek(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return service.openWeek(weekStartDate);
    }

    @Operation(summary = "Add a published recipe to the member's meal",
            description = "Creates the week if needed. Default plannedServings is 1. Different recipes may share a meal.")
    @ApiResponse(responseCode = "201", description = "The new meal-plan entry.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.Entry.class)))
    @ApiResponse(responseCode = "404", description = "The recipe is unavailable.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "MEAL_PLAN_RECIPE_DUPLICATE: recipe already belongs to this day/meal.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{weekStartDate}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public MealPlanWeekResponse.Entry addEntry(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @Valid @RequestBody AddMealPlanEntryRequest request) {
        return service.addEntry(weekStartDate, request);
    }

    @Operation(summary = "Replace the recipe in an owned meal-plan entry",
            description = "Preserves the entry's day, meal type and planned servings. Also supports replacing a tombstone.")
    @ApiResponse(responseCode = "200", description = "The updated entry.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.Entry.class)))
    @ApiResponse(responseCode = "404", description = "The replacement recipe is unavailable.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "MEAL_PLAN_RECIPE_DUPLICATE: recipe already belongs to this day/meal.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{weekStartDate}/entries/{entryId}")
    public MealPlanWeekResponse.Entry replaceEntry(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @PathVariable @Min(1) long entryId, @Valid @RequestBody ReplaceMealPlanEntryRequest request) {
        return service.replaceEntry(weekStartDate, entryId, request.recipeId());
    }

    @Operation(summary = "Update the number of servings to prepare for an owned meal-plan entry",
            description = "Accepts planned servings from 0.5 to 10 in increments of 0.5. "
                    + "Preserves the recipe, date and meal type. Hidden or deleted recipes retain neutral unavailable fields.")
    @ApiResponse(responseCode = "200", description = "The entry with the saved plannedServings.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.Entry.class)))
    @PatchMapping("/{weekStartDate}/entries/{entryId}/servings")
    public MealPlanWeekResponse.Entry updateServings(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @PathVariable @Min(1) long entryId, @Valid @RequestBody UpdateMealPlanServingsRequest request) {
        return service.updateServings(weekStartDate, entryId, request.plannedServings());
    }

    @Operation(summary = "Move an owned entry within the same week",
            description = "Preserves identity, recipe and planned servings. Rejects a duplicate destination atomically.")
    @ApiResponse(responseCode = "200", description = "The moved entry.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.Entry.class)))
    @ApiResponse(responseCode = "409", description = "MEAL_PLAN_RECIPE_DUPLICATE: destination already contains this recipe.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{weekStartDate}/entries/{entryId}/position")
    public MealPlanWeekResponse.Entry moveEntry(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @PathVariable @Min(1) long entryId, @Valid @RequestBody MoveMealPlanEntryRequest request) {
        return service.moveEntry(weekStartDate, entryId, request);
    }

    @Operation(summary = "Remove an owned entry, including an unavailable recipe reference",
            description = "Removes only the meal-plan entry. Does not delete the recipe or unsave it.")
    @ApiResponse(responseCode = "204", description = "Entry removed.", content = @Content)
    @DeleteMapping("/{weekStartDate}/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEntry(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @PathVariable @Min(1) long entryId) {
        service.removeEntry(weekStartDate, entryId);
    }

    @Operation(summary = "Analyze nutrition for the authenticated member's meal plan",
            description = "Returns 9 catalog-based nutrient values for one day or the complete 7-day week, with reference targets and missing-data warnings.")
    @ApiResponse(responseCode = "200", description = "Nutrition analysis.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanNutritionResponse.class)))
    @GetMapping("/{weekStartDate}/nutrition")
    public MealPlanNutritionResponse analyzeNutrition(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return nutrition.analyze(weekStartDate, date);
    }

    @Operation(summary = "Export the authenticated member's daily or weekly nutrition report as PDF",
            description = "Generates a current-plan A4 report on demand. Week reports contain exactly two pages; day reports contain exactly one page.")
    @ApiResponse(responseCode = "200", description = "PDF report.", content = @Content(mediaType = "application/pdf"))
    @GetMapping(value = "/{weekStartDate}/nutrition/report", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportNutritionReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MealPlanNutritionResponse report = nutrition.analyze(weekStartDate, date);
        byte[] bytes = renderPdf(report);
        String filename = date == null ? "bao-cao-dinh-duong-tuan-" + weekStartDate + ".pdf"
                : "bao-cao-dinh-duong-ngay-" + date + ".pdf";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    @Operation(summary = "Request Pro nutrition analysis recommendations from existing recipes",
            description = "The server supplies catalog-computed nutrition and validates all selected IDs. Applying a suggestion requires a separate member confirmation.")
    @ApiResponse(responseCode = "200", description = "AI recipe suggestions.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanNutritionRecommendationsResponse.class)))
    @PostMapping("/{weekStartDate}/nutrition/recommendations/{entryId}")
    public MealPlanNutritionRecommendationsResponse nutritionRecommendations(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @PathVariable @Min(1) long entryId) {
        return nutritionAi.recommend(weekStartDate, entryId);
    }

    private static byte[] renderPdf(MealPlanNutritionResponse report) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if ("WEEK".equals(report.scope())) {
                writeWeeklyMatrix(document, report);
                writeWeeklyMeals(document, report);
            } else {
                writeDailyReport(document, report);
            }
            document.save(output);
            if (output.size() < 100) throw new IllegalStateException("PDF output is empty");
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot generate nutrition report PDF", exception);
        }
    }

    private static PDType0Font font(PDDocument document) throws IOException {
        var fontPath = java.util.List.of("/usr/share/fonts/noto/NotoSans-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf", "C:/Windows/Fonts/arial.ttf")
                .stream().map(java.nio.file.Path::of).filter(java.nio.file.Files::isRegularFile).findFirst()
                .orElseThrow(() -> new IllegalStateException("Unicode font is unavailable for PDF rendering"));
        try (var input = java.nio.file.Files.newInputStream(fontPath)) {
            return PDType0Font.load(document, input);
        }
    }

    private static PDPageContentStream newPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return new PDPageContentStream(document, page);
    }

    private static void writeWeeklyMatrix(PDDocument document, MealPlanNutritionResponse report) throws IOException {
        PDType0Font font = font(document);
        try (PDPageContentStream stream = newPage(document)) {
            float y = 800;
            y = write(stream, font, y, "BÁO CÁO DINH DƯỠNG TUẦN " + report.weekStartDate(), 13);
            y = write(stream, font, y - 8, report.referenceBasis(), 7);
            y = writeWrapped(stream, font, y - 8, "Nguồn: " + report.source() + " · " + report.sourceUrl(), 7, 520);
            y = write(stream, font, y - 10, "Chỉ tiêu | T2 | T3 | T4 | T5 | T6 | T7 | CN | TB/7 ngày | Ngày đạt", 7);
            for (Target target : report.targets()) {
                var values = report.days().stream().map(day -> day.nutrients().get(target.key())).toList();
                String cells = values.stream().map(MealPlanController::shortValue).collect(java.util.stream.Collectors.joining(" | "));
                String average = shortValue(report.weeklyAverage().get(target.key()));
                y = write(stream, font, y - 12, fit(target.label() + " (" + format(target.minimum()) + target.unit() + ")", 22) + " | " + cells + " | " + average + " | "
                        + report.attainmentDays().getOrDefault(target.key(), 0) + "/7", 6);
            }
            y = writeWrapped(stream, font, y - 14, report.disclaimer(), 6, 520);
            write(stream, font, y - 6, "Tạo lúc: " + Instant.now(), 6);
        }
    }

    private static void writeWeeklyMeals(PDDocument document, MealPlanNutritionResponse report) throws IOException {
        PDType0Font font = font(document);
        try (PDPageContentStream stream = newPage(document)) {
            float y = 800;
            y = write(stream, font, y, "THỰC ĐƠN DỰ KIẾN VÀ CẢNH BÁO", 13);
            for (Day day : report.days()) {
                y = write(stream, font, y - 12, day.date().toString(), 9);
                if (day.entries().isEmpty()) y = write(stream, font, y - 8, "Chưa xếp món (tính 0 trong trung bình tuần).", 7);
                for (var entry : day.entries()) {
                    y = writeWrapped(stream, font, y - 8, mealLabel(entry.mealType()) + " · " + entry.recipeTitle()
                            + " · " + entry.plannedServings() + " khẩu phần", 7, 520);
                }
            }
            if (!report.incompleteEntries().isEmpty()) {
                y = write(stream, font, y - 10, "CẢNH BÁO DỮ LIỆU THIẾU", 9);
                for (var entry : report.incompleteEntries()) {
                    y = writeWrapped(stream, font, y - 7, entry.date() + " · " + entry.recipeTitle() + ": "
                            + String.join(", ", entry.ingredientsMissingData()), 7, 520);
                }
            }
            y = writeWrapped(stream, font, y - 14, report.disclaimer(), 6, 520);
            write(stream, font, y - 6, "Tạo lúc: " + Instant.now(), 6);
        }
    }

    private static void writeDailyReport(PDDocument document, MealPlanNutritionResponse report) throws IOException {
        PDType0Font font = font(document);
        Day day = report.days().get(0);
        try (PDPageContentStream stream = newPage(document)) {
            float y = 800;
            y = write(stream, font, y, "BÁO CÁO DINH DƯỠNG NGÀY " + report.selectedDate(), 13);
            y = write(stream, font, y - 7, report.referenceBasis(), 7);
            y = writeWrapped(stream, font, y - 7, "Nguồn: " + report.source() + " · " + report.sourceUrl(), 6, 520);
            y = write(stream, font, y - 10, "Chỉ tiêu | Kế hoạch | Mức tham khảo | Chênh lệch", 8);
            for (Target target : report.targets()) {
                BigDecimal value = day.nutrients().get(target.key());
                String delta = value == null || target.minimum() == null ? "Chưa đủ dữ liệu"
                        : value.subtract(target.minimum()).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " " + target.unit();
                y = write(stream, font, y - 12, target.label() + " | " + format(value) + " | "
                        + targetText(target) + " | " + delta, 7);
            }
            y = write(stream, font, y - 14, "THỰC ĐƠN DỰ KIẾN", 9);
            for (var entry : day.entries()) {
                y = writeWrapped(stream, font, y - 9, mealLabel(entry.mealType()) + " · " + entry.recipeTitle()
                        + " · " + entry.plannedServings() + " khẩu phần", 8, 520);
            }
            if (!report.incompleteEntries().isEmpty()) {
                y = write(stream, font, y - 10, "CẢNH BÁO NGUYÊN LIỆU THIẾU DỮ LIỆU", 8);
                for (var entry : report.incompleteEntries()) {
                    y = writeWrapped(stream, font, y - 8, entry.recipeTitle() + ": "
                            + String.join(", ", entry.ingredientsMissingData()), 7, 520);
                }
            }
            y = writeWrapped(stream, font, y - 14, report.disclaimer(), 6, 520);
            write(stream, font, y - 6, "Tạo lúc: " + Instant.now(), 6);
        }
    }

    private static String mealLabel(String mealType) {
        return switch (mealType) { case "BREAKFAST" -> "Sáng"; case "LUNCH" -> "Trưa"; default -> "Tối"; };
    }

    private static String shortValue(BigDecimal value) {
        return value == null ? "Thiếu" : value.stripTrailingZeros().toPlainString();
    }

    private static String format(BigDecimal value) { return value == null ? "Chưa đủ dữ liệu" : value.stripTrailingZeros().toPlainString(); }

    private static String targetText(Target target) {
        if (target.minimum() == null) return "Chưa có mức tham khảo";
        String range = format(target.minimum());
        if (target.maximum() != null && target.maximum().compareTo(target.minimum()) != 0) range += "–" + format(target.maximum());
        return range + " " + target.unit();
    }

    private static String fit(String text, int max) { return text.length() <= max ? text : text.substring(0, max - 1) + "…"; }

    private static float writeWrapped(PDPageContentStream stream, PDType0Font font, float y,
            String text, int size, float maxWidth) throws IOException {
        StringBuilder line = new StringBuilder();
        for (String word : (text == null ? "" : text).split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) * size / 1000f > maxWidth && !line.isEmpty()) {
                y = write(stream, font, y, line.toString(), size) - size - 2;
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        return write(stream, font, y, line.toString(), size) - size - 2;
    }

    private static float write(PDPageContentStream stream, PDType0Font font, float y, String value, int size)
            throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(36, y);
        stream.showText(value == null ? "" : value);
        stream.endText();
        return y;
    }

}
