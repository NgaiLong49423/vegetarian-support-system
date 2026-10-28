package tech.mamxanh.shopping.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.service.MealPlanReadService;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.shopping.dto.request.CreateMealPlanShoppingListRequest;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse;
import tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse.Warning;
import tech.mamxanh.shopping.entity.ShoppingListEntity;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;
import tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;
import tech.mamxanh.shopping.repository.ShoppingListRepository;

/** FR-53 AC-53.1: create a new, independent checklist from the current Member's meal plan. */
@Service
public class ShoppingListCreationService {
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999999.999999");
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);
    private final CurrentUserService users;
    private final MealPlanReadService mealPlans;
    private final RecipeService recipes;
    private final ShoppingAggregationService aggregation;
    private final ShoppingListRepository lists;
    private final ShoppingListItemRepository items;
    private final ShoppingListService checklists;
    private final EntityManager entityManager;
    private final Clock clock;

    public ShoppingListCreationService(CurrentUserService users, MealPlanReadService mealPlans,
            RecipeService recipes, ShoppingAggregationService aggregation, ShoppingListRepository lists,
            ShoppingListItemRepository items, ShoppingListService checklists, EntityManager entityManager, Clock clock) {
        this.users = users;
        this.mealPlans = mealPlans;
        this.recipes = recipes;
        this.aggregation = aggregation;
        this.lists = lists;
        this.items = items;
        this.checklists = checklists;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public ShoppingListCreationResponse createFromMealPlan(CreateMealPlanShoppingListRequest request) {
        long owner = users.requireActiveMember().id();
        validateDates(request);
        // getWeek derives ownership from authentication; no owner ID or foreign entry IDs are accepted.
        var week = mealPlans.getWeek(request.weekStartDate());
        var selected = request.selectedDates() == null ? null : new HashSet<>(request.selectedDates());
        var entries = week.entries().stream().filter(entry -> selected == null || selected.contains(entry.mealDate())).toList();
        if (entries.isEmpty()) throw invalid("Chưa có món trong các ngày đã chọn. Hãy thêm món vào lịch ăn trước.");
        var sources = recipes.findShoppingSources(entries.stream().map(entry -> entry.recipeId()).distinct().toList());
        var warnings = new ArrayList<Warning>();
        var lines = new ArrayList<ShoppingAggregationLine>();
        var origins = new ArrayList<Origin>();
        var contributions = new ArrayList<ShoppingRecipeContributionEntity>();
        for (var entry : entries) {
            var recipe = sources.get(entry.recipeId());
            if (recipe == null) {
                warnings.add(new Warning("UNAVAILABLE_RECIPE", "Bỏ qua công thức không còn công khai.", entry.entryId(), entry.recipeId()));
                continue;
            }
            UUID sourceId = UUID.randomUUID();
            for (var line : recipe.ingredients()) {
                if (line.ingredientId() == null) {
                    warnings.add(new Warning("INVALID_RECIPE_INGREDIENT", "Bỏ qua nguyên liệu công thức không có mã catalog.", entry.entryId(), entry.recipeId()));
                    continue;
                }
                BigDecimal quantity = line.quantity() == null ? null : line.quantity().multiply(entry.plannedServings())
                        .divide(BigDecimal.valueOf(recipe.servings()), MathContext.DECIMAL128);
                lines.add(new ShoppingAggregationLine(line.ingredientId(), null, quantity, line.unitId()));
                origins.add(new Origin(entry.entryId(), entry.recipeId()));
                var contribution = new ShoppingRecipeContributionEntity();
                contribution.setSourceId(sourceId);
                contribution.setRecipeId(entry.recipeId());
                contribution.setSourceKind("RECIPE");
                contribution.setExpectedIngredientCount(recipe.ingredients().size());
                contribution.setIngredientId(line.ingredientId());
                contribution.setUnitId(line.unitId());
                contribution.setQuantity(quantity);
                contribution.setServings(entry.plannedServings());
                contribution.setMealPlanEntryId(entry.entryId());
                contribution.setMealDate(entry.mealDate());
                contribution.setMealType(entry.mealType());
                contributions.add(contribution);
            }
        }
        var result = aggregation.aggregate(lines);
        for (var warning : result.warnings()) {
            var origin = origins.get(warning.sourceIndex());
            warnings.add(new Warning(warning.code(), warning.message(), origin.entryId(), origin.recipeId()));
        }
        var aggregated = result.groups().stream().flatMap(group -> group.items().stream()).toList();
        if (aggregated.isEmpty()) throw invalid("Không còn nguyên liệu hợp lệ để tạo danh sách mua sắm.");
        var rows = new ArrayList<ShoppingListItemEntity>();
        for (var item : aggregated) {
            // Only round after all source contributions have been summed. Never persist zero or overflow.
            BigDecimal quantity = item.quantity().setScale(6, RoundingMode.HALF_UP);
            if (quantity.signum() <= 0 || quantity.compareTo(MAX_QUANTITY) > 0) {
                throw invalid("Định lượng sau tổng hợp vượt giới hạn lưu trữ hoặc quá nhỏ: " + item.name());
            }
            if (quantity.compareTo(item.quantity()) != 0) {
                warnings.add(new Warning("QUANTITY_ROUNDED", "Lượng " + item.name() + " được làm tròn đến sáu chữ số thập phân khi lưu.", null, null));
            }
            var row = new ShoppingListItemEntity();
            row.setIngredientId(item.ingredientId());
            row.setUnitId(item.unitId());
            row.setQuantity(quantity);
            row.setBought(false);
            rows.add(row);
        }
        var list = new ShoppingListEntity();
        list.setUserId(owner);
        list.setName("Danh sách mua sắm " + clock.instant()
                .atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().format(FULL_DATE));
        lists.saveAndFlush(list);
        entityManager.refresh(list); // read database-generated timestamps, rather than fabricate response values
        rows.forEach(row -> row.setShoppingListId(list.getId()));
        items.saveAllAndFlush(rows);
        for (int i = 0; i < aggregated.size(); i++) {
            var target = rows.get(i);
            for (int sourceIndex : aggregated.get(i).sourceIndices()) {
                var contribution = contributions.get(sourceIndex);
                contribution.setShoppingListId(list.getId());
                contribution.setShoppingListItemId(target.getId());
                entityManager.persist(contribution);
            }
        }
        entityManager.flush();
        var incompatible = new ArrayList<Long>();
        for (int i = 0; i < rows.size(); i++) {
            if (aggregated.get(i).incompatibleUnits()) incompatible.add(rows.get(i).getId());
        }
        return new ShoppingListCreationResponse(checklists.get(list.getId()), warnings, incompatible);
    }

    private void validateDates(CreateMealPlanShoppingListRequest request) {
        if (request == null || request.weekStartDate() == null) throw invalid("Cần chọn tuần bắt đầu vào thứ Hai.");
        LocalDate start = request.weekStartDate();
        if (!java.time.DayOfWeek.MONDAY.equals(start.getDayOfWeek()) || start.getYear() < 1
                || start.isAfter(LocalDate.of(9999, 12, 31).minusDays(6))) throw invalid("Tuần không hợp lệ; ngày bắt đầu phải là thứ Hai.");
        var dates = request.selectedDates();
        if (dates != null && (dates.isEmpty() || dates.size() > 7 || new HashSet<>(dates).size() != dates.size()
                || dates.stream().anyMatch(date -> date == null || date.isBefore(start) || date.isAfter(start.plusDays(6))))) {
            throw invalid("Chọn từ 1 đến 7 ngày khác nhau trong tuần.");
        }
    }

    private record Origin(long entryId, long recipeId) { }
    private AppException invalid(String message) { return new AppException(ErrorCode.VALIDATION_FAILED, message); }
}
