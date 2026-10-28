package tech.mamxanh.shopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.shopping.dto.request.AddRecipeToShoppingListRequest;
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.request.CreateShoppingItemRequest;
import tech.mamxanh.shopping.dto.request.CreateShoppingListFromRecipeRequest;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse;
import tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse.Warning;
import tech.mamxanh.shopping.entity.ShoppingListEntity;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;
import tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;
import tech.mamxanh.shopping.repository.ShoppingListRepository;
import tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository;

/** FR-53 AC-53.6: add one public Recipe Post to a selected owned checklist. */
@Service
public class RecipeShoppingListService {
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("99999999.99");

    private final CurrentUserService users;
    private final RecipeService recipes;
    private final ShoppingAggregationService aggregation;
    private final ShoppingListRepository lists;
    private final ShoppingListItemRepository items;
    private final ShoppingRecipeContributionRepository contributions;
    private final ShoppingListService checklists;
    private final EntityManager em;
    private final Clock clock;

    public RecipeShoppingListService(CurrentUserService users, RecipeService recipes,
            ShoppingAggregationService aggregation, ShoppingListRepository lists,
            ShoppingListItemRepository items, ShoppingRecipeContributionRepository contributions,
            ShoppingListService checklists, EntityManager em, Clock clock) {
        this.users = users;
        this.recipes = recipes;
        this.aggregation = aggregation;
        this.lists = lists;
        this.items = items;
        this.contributions = contributions;
        this.checklists = checklists;
        this.em = em;
        this.clock = clock;
    }

    @Transactional
    public ShoppingListCreationResponse addRecipe(long listId, AddRecipeToShoppingListRequest request) {
        long owner = users.lockActiveMember().id();
        validateRecipeRequest(request == null ? null : request.recipeId(), request == null ? null : request.servings());
        var list = ownedList(listId, owner);
        return addRecipeToList(list, request.recipeId(), request.servings(),
                request.selectedIngredientIds(), request.manualItems());
    }

    @Transactional
    public ShoppingListCreationResponse createFromRecipe(CreateShoppingListFromRecipeRequest request) {
        if (request == null) throw invalid("Thông tin tạo danh sách không hợp lệ.");
        long owner = users.lockActiveMember().id();
        validateRecipeRequest(request.recipeId(), request.servings());

        // The checklist service joins this transaction, so a failed recipe add rolls back the new list.
        var created = checklists.createEmpty(new CreateEmptyShoppingListRequest(request.name()));
        var list = ownedList(created.id(), owner);
        return addRecipeToList(list, request.recipeId(), request.servings(),
                request.selectedIngredientIds(), request.manualItems());
    }

    private ShoppingListCreationResponse addRecipeToList(ShoppingListEntity list,
            long recipeId, Integer servings, List<Long> selectedIngredientIds,
            List<CreateShoppingItemRequest> manualItems) {
        var sources = recipes.findShoppingSources(List.of(recipeId));
        var source = sources.get(recipeId);
        if (source == null) {
            throw new AppException(ErrorCode.NOT_FOUND,
                    "Công thức không còn công khai hoặc không tồn tại. Hãy tải lại danh sách.");
        }
        if (source.servings() < 1) throw invalid("Khẩu phần gốc của công thức không hợp lệ.");

        var manual = manualItems == null ? List.<CreateShoppingItemRequest>of() : manualItems;
        var requestedIds = selectedIngredientIds == null ? null : new HashSet<>(selectedIngredientIds);
        if (requestedIds != null && requestedIds.size() != selectedIngredientIds.size()) {
            throw invalid("Nguyên liệu được chọn không được lặp lại.");
        }
        var availableIds = new HashSet<Long>();
        source.ingredients().forEach(line -> availableIds.add(line.ingredientId()));
        if (requestedIds != null && !availableIds.containsAll(requestedIds)) {
            throw invalid("Có nguyên liệu được chọn không thuộc công thức này.");
        }
        if (requestedIds != null && requestedIds.isEmpty() && manual.isEmpty()) {
            throw invalid("Hãy chọn ít nhất một nguyên liệu hoặc thêm mặt hàng thủ công.");
        }
        if (source.ingredients().isEmpty() && manual.isEmpty()) {
            throw invalid("Công thức không có nguyên liệu để thêm vào danh sách.");
        }

        var saved = new ArrayList<>(contributions.findByShoppingListIdOrderByIdAsc(list.getId()));
        boolean alreadyAdded = saved.stream().anyMatch(row -> "RECIPE".equals(row.getSourceKind())
                && java.util.Objects.equals(row.getRecipeId(), recipeId));
        if (alreadyAdded && manual.isEmpty()) {
            return new ShoppingListCreationResponse(checklists.get(list.getId()), List.of(), List.of());
        }

        var added = new ArrayList<ShoppingRecipeContributionEntity>();
        UUID sourceId = UUID.randomUUID();
        for (var line : source.ingredients()) {
            if (requestedIds != null && !requestedIds.contains(line.ingredientId())) continue;
            if (alreadyAdded) continue;
            if (line.ingredientId() == null || line.unitId() == null
                    || line.quantity() == null || line.quantity().signum() <= 0) {
                throw invalid("Công thức có nguyên liệu hoặc định lượng không hợp lệ.");
            }
            BigDecimal quantity = line.quantity();
            if (servings != null) {
                quantity = quantity.multiply(BigDecimal.valueOf(servings))
                        .divide(BigDecimal.valueOf(source.servings()), 8, RoundingMode.HALF_UP);
                quantity = storedQuantity(quantity);
            }
            var row = new ShoppingRecipeContributionEntity();
            row.setRecipeId(recipeId);
            row.setSourceKind("RECIPE");
            row.setExpectedIngredientCount(source.ingredients().size());
            row.setIngredientId(line.ingredientId());
            row.setUnitId(line.unitId());
            row.setQuantity(quantity);
            row.setSourceId(sourceId);
            row.setServings(BigDecimal.valueOf(servings == null ? source.servings() : servings));
            added.add(row);
        }

        Set<Long> ingredientIds = new HashSet<>();
        added.forEach(row -> ingredientIds.add(row.getIngredientId()));
        var previous = items.findByShoppingListIdOrderByIdAsc(list.getId()).stream()
                .filter(row -> row.getIngredientId() != null && ingredientIds.contains(row.getIngredientId()))
                .toList();
        var lines = new ArrayList<ShoppingAggregationLine>();
        previous.forEach(row -> lines.add(new ShoppingAggregationLine(
                row.getIngredientId(), null, row.getQuantity(), row.getUnitId())));
        added.forEach(row -> lines.add(new ShoppingAggregationLine(
                row.getIngredientId(), null, row.getQuantity(), row.getUnitId())));

        var result = aggregation.aggregate(lines);
        if (result.warnings().stream().anyMatch(w -> !w.code().equals("CONFLICTING_CONVERSION"))) {
            throw invalid("Không thể tổng hợp an toàn toàn bộ nguyên liệu đã chọn.");
        }
        var outputs = result.groups().stream().flatMap(group -> group.items().stream()).toList();
        outputs.forEach(output -> storedQuantity(output.quantity()));

        var warnings = new ArrayList<Warning>();
        result.warnings().forEach(warning -> warnings.add(
                new Warning(warning.code(), warning.message(), null, recipeId)));
        var retained = new HashSet<Long>();
        var incompatible = new ArrayList<Long>();
        for (var output : outputs) {
            BigDecimal quantity = storedQuantity(output.quantity());
            if (quantity.compareTo(output.quantity()) != 0) {
                warnings.add(new Warning("QUANTITY_ROUNDED",
                        "Lượng được làm tròn đến hai chữ số thập phân khi lưu.", null, recipeId));
            }
            var old = output.sourceIndices().stream()
                    .filter(index -> index < previous.size()).map(previous::get).toList();
            boolean hasNew = output.sourceIndices().stream().anyMatch(index -> index >= previous.size());
            var target = old.isEmpty() ? new ShoppingListItemEntity() : old.getFirst();
            target.setShoppingListId(list.getId());
            target.setIngredientId(output.ingredientId());
            target.setUnitId(output.unitId());
            target.setQuantity(quantity);
            target.setBought(!hasNew && old.stream().allMatch(ShoppingListItemEntity::isBought));
            items.save(target);
            retained.add(target.getId());
            if (output.incompatibleUnits()) incompatible.add(target.getId());

            var oldIds = new HashSet<Long>();
            old.forEach(row -> oldIds.add(row.getId()));
            saved.stream().filter(contribution -> oldIds.contains(contribution.getShoppingListItemId()))
                    .forEach(contribution -> contribution.setShoppingListItemId(target.getId()));
            for (int index : output.sourceIndices()) {
                if (index >= previous.size()) {
                    added.get(index - previous.size()).setShoppingListItemId(target.getId());
                }
            }
        }

        added.forEach(row -> row.setShoppingListId(list.getId()));
        if (!added.isEmpty()) {
            contributions.saveAllAndFlush(saved);
            contributions.saveAllAndFlush(added);
        }
        items.deleteAll(previous.stream().filter(row -> !retained.contains(row.getId())).toList());
        if (!added.isEmpty()) list.setUpdatedAt(LocalDateTime.now(clock));
        for (var item : manual) checklists.add(list.getId(), item);
        em.flush();
        return new ShoppingListCreationResponse(checklists.get(list.getId()), warnings, incompatible);
    }

    private ShoppingListEntity ownedList(long listId, long owner) {
        if (listId < 1) throw invalid("listId phải > 0.");
        return lists.lockOwned(listId, owner).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private void validateRecipeRequest(Long recipeId, Integer servings) {
        if (recipeId == null || recipeId < 1) throw invalid("recipeId phải > 0.");
        if (servings != null && (servings < 1 || servings > 50)) {
            throw invalid("Khẩu phần cần mua phải từ 1 đến 50.");
        }
    }

    private BigDecimal storedQuantity(BigDecimal value) {
        BigDecimal quantity = value.setScale(6, RoundingMode.HALF_UP);
        if (quantity.signum() <= 0 || quantity.compareTo(MAX_QUANTITY) > 0) {
            throw invalid("Định lượng sau tổng hợp vượt giới hạn lưu trữ hoặc quá nhỏ.");
        }
        return quantity;
    }

    private AppException invalid(String message) {
        return new AppException(ErrorCode.VALIDATION_FAILED, message);
    }
}
