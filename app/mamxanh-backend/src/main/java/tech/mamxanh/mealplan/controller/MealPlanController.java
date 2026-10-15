package tech.mamxanh.mealplan.controller;

import java.time.LocalDate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.awt.Color;
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
            description = "Generates a current-plan A4 report on demand. The report table continues on additional pages when needed.")
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
                writeWeeklyReport(document, report);
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

    private static void writeWeeklyReport(PDDocument document, MealPlanNutritionResponse report) throws IOException {
        PDType0Font reportFont = font(document);
        float pageWidth = PDRectangle.A4.getHeight();
        float contentWidth = pageWidth - 72;
        float[] widths = {132, 62, 62, 62, 62, 62, 62, 62, 70, 58, contentWidth - 694};
        String[] headers = {"Chỉ tiêu / bữa", weeklyDayHeader(report.days().get(0)),
                weeklyDayHeader(report.days().get(1)), weeklyDayHeader(report.days().get(2)),
                weeklyDayHeader(report.days().get(3)), weeklyDayHeader(report.days().get(4)),
                weeklyDayHeader(report.days().get(5)), weeklyDayHeader(report.days().get(6)),
                "Trung bình\n7 ngày", "Ngày đạt", "Mức tham khảo"};
        try (PdfTable table = new PdfTable(document, reportFont, true,
                "BÁO CÁO DINH DƯỠNG TUẦN " + report.weekStartDate(), report, headers, widths)) {
            table.section("DINH DƯỠNG DỰ KIẾN");
            for (Target target : report.targets()) {
                String[] cells = new String[headers.length];
                cells[0] = target.label() + "\n" + target.unit();
                for (int index = 0; index < 7; index++) {
                    cells[index + 1] = printableValue(report.days().get(index).nutrients().get(target.key()));
                }
                cells[8] = printableValue(report.weeklyAverage().get(target.key()));
                Integer attainedDays = report.attainmentDays().get(target.key());
                cells[9] = attainedDays == null ? "Thiếu" : attainedDays + "/7";
                cells[10] = targetTextForTable(target);
                table.row(cells, 8, true);
            }
            table.section("THỰC ĐƠN DỰ KIẾN VÀ CẢNH BÁO");
            for (String mealType : java.util.List.of("BREAKFAST", "LUNCH", "DINNER")) {
                String[] cells = new String[headers.length];
                cells[0] = "Bữa " + mealLabel(mealType).toLowerCase(java.util.Locale.ROOT);
                for (int index = 0; index < 7; index++) {
                    cells[index + 1] = mealPlanCell(report.days().get(index), mealType);
                }
                cells[8] = "—";
                cells[9] = "—";
                cells[10] = "—";
                table.row(cells, 7, false);
            }
            table.footer(report.disclaimer());
        }
    }

    private static PDPage landscapePage(PDDocument document) {
        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(page);
        return page;
    }

    private static String weeklyDayHeader(Day day) {
        int weekday = day.date().getDayOfWeek().getValue();
        String label = weekday == 7 ? "CN" : "T" + (weekday + 1);
        return label + "\n" + day.date().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
    }

    private static String mealPlanCell(Day day, String mealType) {
        var matching = day.entries().stream().filter(entry -> entry.mealType().equals(mealType)).toList();
        if (matching.isEmpty()) return "Chưa xếp món";
        return matching.stream().map(entry -> {
            String detail = entry.recipeTitle() + " · " + entry.plannedServings().toPlainString() + " khẩu phần";
            if (!entry.complete() || !entry.ingredientsMissingData().isEmpty()) {
                String missing = entry.ingredientsMissingData().isEmpty()
                        ? "Thiếu dữ liệu dinh dưỡng" : "Thiếu dữ liệu: " + String.join(", ", entry.ingredientsMissingData());
                detail += "\n" + missing;
            }
            return detail;
        }).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static void drawTableCell(PDPageContentStream stream, PDType0Font font, float x, float y,
            float width, float height, String value, int fontSize, boolean centered, Color background)
            throws IOException {
        stream.setNonStrokingColor(background);
        stream.addRect(x, y, width, height);
        stream.fill();
        stream.setStrokingColor(new Color(190, 198, 203));
        stream.setLineWidth(0.5f);
        stream.addRect(x, y, width, height);
        stream.stroke();

        java.util.List<String> lines = wrapLines(font, value, fontSize, width - 8);
        float lineHeight = fontSize + 2;
        float textY = y + (height + (lines.size() - 1) * lineHeight) / 2 - fontSize * 0.35f;
        stream.setNonStrokingColor(new Color(35, 42, 46));
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            float textWidth = font.getStringWidth(line) * fontSize / 1000f;
            float textX = centered ? x + (width - textWidth) / 2 : x + 5;
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(textX, textY - index * lineHeight);
            stream.showText(line);
            stream.endText();
        }
    }

    private static java.util.List<String> wrapLines(PDType0Font font, String value, int fontSize, float maxWidth)
            throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String paragraph : (value == null ? "" : value).split("\\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                if (word.isEmpty()) continue;
                if (!line.isEmpty() && font.getStringWidth(line + " " + word) * fontSize / 1000f <= maxWidth) {
                    line.append(' ').append(word);
                    continue;
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                for (int index = 0; index < word.length();) {
                    int codePoint = word.codePointAt(index);
                    String glyph = new String(Character.toChars(codePoint));
                    if (!line.isEmpty()
                            && font.getStringWidth(line + glyph) * fontSize / 1000f > maxWidth) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    line.append(glyph);
                    index += Character.charCount(codePoint);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static void writeDailyReport(PDDocument document, MealPlanNutritionResponse report) throws IOException {
        PDType0Font reportFont = font(document);
        Day day = report.days().get(0);
        float contentWidth = PDRectangle.A4.getWidth() - 72;
        float[] widths = {132, 180, 109, contentWidth - 421};
        String[] headers = {"Chỉ tiêu / bữa", "Kế hoạch / món", "Mức tham khảo", "Chênh lệch / ghi chú"};
        try (PdfTable table = new PdfTable(document, reportFont, false,
                "BÁO CÁO DINH DƯỠNG NGÀY " + report.selectedDate(), report, headers, widths)) {
            table.section("DINH DƯỠNG DỰ KIẾN");
            for (Target target : report.targets()) {
                BigDecimal value = day.nutrients().get(target.key());
                String delta = value == null || target.minimum() == null ? "Chưa đủ dữ liệu"
                        : printableValue(value.subtract(target.minimum())) + " " + target.unit();
                table.row(new String[] {target.label() + "\n" + target.unit(),
                        value == null ? "Chưa đủ dữ liệu" : printableValue(value) + " " + target.unit(),
                        targetTextForTable(target), delta}, 8, true);
            }
            table.section("THỰC ĐƠN DỰ KIẾN VÀ CẢNH BÁO");
            for (String mealType : java.util.List.of("BREAKFAST", "LUNCH", "DINNER")) {
                table.row(new String[] {"Bữa " + mealLabel(mealType).toLowerCase(java.util.Locale.ROOT),
                        mealPlanCell(day, mealType), "—", "—"}, 8, false);
            }
            table.footer(report.disclaimer());
        }
    }

    private static final class PdfTable implements AutoCloseable {
        private static final float MARGIN = 36;
        private static final float HEADER_HEIGHT = 32;
        private final PDDocument document;
        private final PDType0Font font;
        private final boolean landscape;
        private final String title;
        private final MealPlanNutritionResponse report;
        private final String[] headers;
        private final float[] widths;
        private final float bottomLimit;
        private PDPageContentStream stream;
        private float y;
        private int rowsOnPage;
        private int rowNumber;

        private PdfTable(PDDocument document, PDType0Font font, boolean landscape, String title,
                MealPlanNutritionResponse report, String[] headers, float[] widths) throws IOException {
            this.document = document;
            this.font = font;
            this.landscape = landscape;
            this.title = title;
            this.report = report;
            this.headers = headers;
            this.widths = widths;
            float contentWidth = (landscape ? PDRectangle.A4.getHeight() : PDRectangle.A4.getWidth()) - 2 * MARGIN;
            this.bottomLimit = MARGIN + 15 + wrapLines(font, report.disclaimer(), 7, contentWidth).size() * 9;
            startPage(true);
        }

        private void startPage(boolean first) throws IOException {
            if (stream != null) stream.close();
            PDPage page = landscape ? landscapePage(document) : new PDPage(PDRectangle.A4);
            if (!landscape) document.addPage(page);
            stream = new PDPageContentStream(document, page);
            float contentWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
            y = page.getMediaBox().getHeight() - MARGIN;
            write(stream, font, y, first ? title : title + " (tiếp)", first ? 14 : 12);
            y -= first ? 21 : 25;
            if (first) {
                y = writeWrapped(stream, font, y, report.referenceBasis(), 8, contentWidth);
                y = writeWrapped(stream, font, y - 3,
                        "Nguồn: " + report.source() + " · " + report.sourceUrl(), 7, contentWidth);
                y -= 7;
            }
            float x = MARGIN;
            for (int column = 0; column < headers.length; column++) {
                drawTableCell(stream, font, x, y - HEADER_HEIGHT, widths[column], HEADER_HEIGHT,
                        headers[column], 8, true, new Color(232, 237, 240));
                x += widths[column];
            }
            y -= HEADER_HEIGHT;
            rowsOnPage = 0;
        }

        private void section(String name) throws IOException {
            if (y - bottomLimit < 65) startPage(false);
            float width = 0;
            for (float columnWidth : widths) width += columnWidth;
            drawTableCell(stream, font, MARGIN, y - 27, width, 27,
                    name, 8, false, new Color(222, 230, 234));
            y -= 27;
            rowsOnPage++;
        }

        private void row(String[] cells, int fontSize, boolean centerValues) throws IOException {
            java.util.List<java.util.List<String>> wrapped = new java.util.ArrayList<>();
            int maxLines = 1;
            for (int column = 0; column < cells.length; column++) {
                var lines = wrapLines(font, cells[column], fontSize, widths[column] - 10);
                wrapped.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }
            int lineHeight = fontSize + 2;
            float fullHeight = Math.max(27, maxLines * lineHeight + 10);
            if (fullHeight > y - bottomLimit && rowsOnPage > 0) startPage(false);
            Color background = rowNumber++ % 2 == 0 ? Color.WHITE : new Color(247, 249, 250);
            int offset = 0;
            while (offset < maxLines) {
                int fittingLines = (int) Math.floor((y - bottomLimit - 10) / lineHeight);
                if (fittingLines < 1) {
                    startPage(false);
                    continue;
                }
                int count = Math.min(maxLines - offset, fittingLines);
                float height = Math.max(27, count * lineHeight + 10);
                if (height > y - bottomLimit) {
                    startPage(false);
                    continue;
                }
                float x = MARGIN;
                for (int column = 0; column < cells.length; column++) {
                    var lines = wrapped.get(column);
                    String fragment = offset >= lines.size() ? ""
                            : String.join("\n", lines.subList(offset, Math.min(offset + count, lines.size())));
                    if (column == 0 && offset > 0 && fragment.isEmpty()) {
                        fragment = cells[0] + " (tiếp)";
                    }
                    drawTableCell(stream, font, x, y - height, widths[column], height,
                            fragment, fontSize, centerValues && column > 0, background);
                    x += widths[column];
                }
                y -= height;
                rowsOnPage++;
                offset += count;
                if (offset < maxLines) startPage(false);
            }
        }

        private void footer(String disclaimer) throws IOException {
            float contentWidth = (landscape ? PDRectangle.A4.getHeight() : PDRectangle.A4.getWidth()) - 2 * MARGIN;
            int lines = wrapLines(font, disclaimer, 7, contentWidth).size();
            if (y - (lines * 9 + 15) < MARGIN) startPage(false);
            y = writeWrapped(stream, font, y - 13, disclaimer, 7, contentWidth);
            write(stream, font, y - 2, "Tạo lúc: " + Instant.now(), 7);
        }

        @Override
        public void close() throws IOException {
            if (stream != null) stream.close();
        }
    }

    private static String printableValue(BigDecimal value) {
        return value == null ? "Thiếu" : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String targetTextForTable(Target target) {
        if (target.minimum() == null) return "Chưa có mức tham khảo";
        String reference = printableValue(target.minimum());
        if (target.maximum() != null && target.maximum().compareTo(target.minimum()) != 0) {
            reference += "–" + printableValue(target.maximum());
        }
        return reference + " " + target.unit();
    }

    private static String mealLabel(String mealType) {
        return switch (mealType) { case "BREAKFAST" -> "Sáng"; case "LUNCH" -> "Trưa"; default -> "Tối"; };
    }

    private static float writeWrapped(PDPageContentStream stream, PDType0Font font, float y,
            String text, int size, float maxWidth) throws IOException {
        for (String line : wrapLines(font, text, size, maxWidth)) {
            y = write(stream, font, y, line, size) - size - 2;
        }
        return y;
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
