package tech.mamxanh.shopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.shopping.dto.request.CreateShoppingItemRequest;
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.request.RenameShoppingListRequest;
import tech.mamxanh.shopping.dto.request.UpdateShoppingItemRequest;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse.Item;
import tech.mamxanh.shopping.entity.ShoppingListEntity;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;
import tech.mamxanh.shopping.repository.ShoppingListRepository;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;
import tech.mamxanh.shopping.service.ShoppingAggregationService;

/** FR-53 checklist operations on persisted lists; source creation is coordinated separately. */
@Service
public class ShoppingListService {
    private final ShoppingListRepository lists;
    private final ShoppingListItemRepository items;
    private final CurrentUserService users;
    private final IngredientCatalogLookupService catalog;
    private final Clock clock;
    private final tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository contributions;
    private final tech.mamxanh.recipe.service.RecipeService recipes;
    private final EntityManager entityManager;
    private final ShoppingAggregationService aggregation;

    public ShoppingListService(ShoppingListRepository lists, ShoppingListItemRepository items,
            CurrentUserService users, IngredientCatalogLookupService catalog, Clock clock,
            tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository contributions,
            tech.mamxanh.recipe.service.RecipeService recipes, EntityManager entityManager,
            ShoppingAggregationService aggregation) {
        this.lists = lists;
        this.items = items;
        this.users = users;
        this.catalog = catalog;
        this.clock = clock;
        this.contributions = contributions;
        this.recipes = recipes;
        this.entityManager = entityManager;
        this.aggregation = aggregation;
    }

    @Transactional
    public ShoppingListResponse createEmpty(CreateEmptyShoppingListRequest request) {
        long owner = users.requireActiveMember().id();
        String name = normalizeListName(request.name());
        var list = new ShoppingListEntity();
        list.setUserId(owner);
        list.setKind("MANUAL");
        list.setName(name);
        lists.saveAndFlush(list);
        entityManager.refresh(list);
        return new ShoppingListResponse(list.getId(), list.getName(), list.getCreatedAt(), list.getUpdatedAt(), List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<ShoppingListResponse.Summary> list(int page, int size) {
        long owner = users.requireActiveMember().id();
        if (page < 0 || size < 1 || size > 50) throw invalid("page phải >= 0; size từ 1 đến 50.");
        var result = lists.findByUserId(owner, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result.map(l -> new ShoppingListResponse.Summary(l.getId(), l.getName(),
                l.getCreatedAt(), l.getUpdatedAt())).getContent(), page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse get(long listId) {
        var list = owned(listId, false);
        var sourceRows = contributions.findByShoppingListIdOrderByIdAsc(listId);
        return new ShoppingListResponse(list.getId(), list.getName(), list.getCreatedAt(), list.getUpdatedAt(),
                resolve(items.findByShoppingListIdOrderByIdAsc(listId)), resolveRecipeSources(sourceRows));
    }

    @Transactional
    public ShoppingListResponse.Summary rename(long listId, RenameShoppingListRequest request) {
        var list = owned(listId, true);
        String name = normalizeListName(request.name());
        list.setName(name);
        touch(list);
        return new ShoppingListResponse.Summary(list.getId(), list.getName(), list.getCreatedAt(), list.getUpdatedAt());
    }

    @Transactional
    public void deleteList(long listId) {
        var list = owned(listId, true);
        // Contributions reference checklist rows with NO ACTION; remove them before
        // the list's owned items cascade, without touching source recipes or meal plans.
        contributions.deleteAllByShoppingListId(listId);
        lists.delete(list);
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> options() {
        users.requireActiveMember();
        return catalog.findActiveUnits();
    }

    @Transactional
    public Item add(long listId, CreateShoppingItemRequest request) {
        var list = owned(listId, true);
        String name = request.name() == null ? "" : request.name().replace('\u00a0', ' ').strip();
        if (name.isBlank() || name.length() > 200 || name.codePoints().anyMatch(c -> c == '<' || c == '>' || Character.isISOControl(c))) {
            throw invalid("Tên mặt hàng phải là văn bản thuần, không rỗng và tối đa 200 ký tự.");
        }
        validateQuantity(request.quantity());
        IngredientResponse selectedIngredient = null;
        if (request.ingredientId() != null) {
            selectedIngredient = catalog.findActiveIngredientsByIds(List.of(request.ingredientId())).stream()
                    .filter(value -> value.id().equals(request.ingredientId())).findFirst()
                    .orElseThrow(() -> invalid("Nguyên liệu đã chọn không còn trong danh mục. Hãy chọn lại."));
            if (!selectedIngredient.name().equals(name)) {
                throw invalid("Tên nguyên liệu phải khớp chính xác với mục đã chọn trong danh mục.");
            }
            if (catalog.findActiveIngredientUnits(selectedIngredient.id()).stream()
                    .noneMatch(unit -> unit.id().equals(request.unitId()))) {
                throw invalid("Đơn vị này không được hỗ trợ cho nguyên liệu đã chọn.");
            }
        } else {
            requireActiveUnit(request.unitId());
        }
        List<ShoppingListItemEntity> existing = items.findByShoppingListIdOrderByIdAsc(listId);
        Long selectedIngredientId = selectedIngredient == null ? null : selectedIngredient.id();
        ShoppingListItemEntity target = existing.stream().filter(row -> selectedIngredientId != null
                ? selectedIngredientId.equals(row.getIngredientId())
                : row.getIngredientId() == null && normalizeItemName(row.getCustomIngredientName()).equals(normalizeItemName(name)))
                .findFirst().orElse(null);
        var source = new tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity();
        source.setShoppingListId(listId);
        source.setSourceId(java.util.UUID.randomUUID());
        source.setSourceKind("MANUAL");
        source.setIngredientId(selectedIngredient == null ? null : selectedIngredient.id());
        source.setUnitId(request.unitId());
        source.setQuantity(request.quantity());
        if (target != null && target.getUnitId().equals(request.unitId())) {
            target.setQuantity(target.getQuantity().add(request.quantity()));
            source.setShoppingListItemId(target.getId());
            items.save(target);
        } else if (target != null && selectedIngredient != null) {
            var result = aggregation.aggregate(List.of(
                    new ShoppingAggregationLine(selectedIngredient.id(), null, target.getQuantity(), target.getUnitId()),
                    new ShoppingAggregationLine(selectedIngredient.id(), null, request.quantity(), request.unitId())));
            var output = result.groups().stream().flatMap(group -> group.items().stream())
                    .filter(row -> row.sourceIndices().contains(0) && row.sourceIndices().contains(1)).findFirst().orElse(null);
            if (output != null) {
                target.setQuantity(output.quantity());
                target.setUnitId(output.unitId());
                source.setShoppingListItemId(target.getId());
                items.save(target);
            } else {
                target = createItem(listId, selectedIngredient.id(), null, request.quantity(), request.unitId());
                source.setShoppingListItemId(target.getId());
            }
        } else {
            target = createItem(listId, selectedIngredient == null ? null : selectedIngredient.id(),
                    selectedIngredient == null ? name : null, request.quantity(), request.unitId());
            source.setShoppingListItemId(target.getId());
        }
        contributions.saveAndFlush(source);
        touch(list);
        return resolve(List.of(target)).getFirst();
    }

    @Transactional
    public Item update(long listId, long itemId, UpdateShoppingItemRequest request) {
        var list = owned(listId, true);
        var item = item(listId, itemId);
        var linkedSources = contributions.findByShoppingListIdOrderByIdAsc(listId).stream()
                .filter(source -> Long.valueOf(itemId).equals(source.getShoppingListItemId())).toList();
        validateQuantity(request.quantity());
        validateItemUnit(item, request.unitId());
        var recipeSources = linkedSources.stream().filter(source -> "RECIPE".equals(source.getSourceKind())).toList();
        var unknownSources = linkedSources.stream().filter(source -> "UNKNOWN".equals(source.getSourceKind())).toList();
        var manualSources = linkedSources.stream().filter(source -> "MANUAL".equals(source.getSourceKind())).toList();
        if (item.getIngredientId() != null) {
            var requestedAmount = aggregateSingle(List.of(new ShoppingAggregationLine(
                    item.getIngredientId(), null, request.quantity(), request.unitId())));
            var fixedAmount = recipeSources.isEmpty() ? null : aggregateSingle(recipeSources.stream()
                    .map(source -> new ShoppingAggregationLine(source.getIngredientId(), null,
                            source.getQuantity(), source.getUnitId())).toList());
            var unknownAmount = unknownSources.isEmpty() ? null : aggregateSingle(unknownSources.stream()
                    .map(source -> new ShoppingAggregationLine(source.getIngredientId(), null,
                            source.getQuantity(), source.getUnitId())).toList());
            var manualAmount = manualSources.isEmpty() ? null : aggregateSingle(manualSources.stream()
                    .map(source -> new ShoppingAggregationLine(source.getIngredientId(), null,
                            source.getQuantity(), source.getUnitId())).toList());
            int targetUnitId = request.unitId();
            BigDecimal requestedQuantity = convertIngredientQuantity(item.getIngredientId(), requestedAmount.quantity(), requestedAmount.unitId(), targetUnitId);
            BigDecimal fixedQuantity = fixedAmount == null ? BigDecimal.ZERO : convertIngredientQuantity(item.getIngredientId(), fixedAmount.quantity(), fixedAmount.unitId(), targetUnitId);
            BigDecimal oldUnknownQuantity = unknownAmount == null ? BigDecimal.ZERO : convertIngredientQuantity(item.getIngredientId(), unknownAmount.quantity(), unknownAmount.unitId(), targetUnitId);
            BigDecimal oldManualQuantity = manualAmount == null ? BigDecimal.ZERO : convertIngredientQuantity(item.getIngredientId(), manualAmount.quantity(), manualAmount.unitId(), targetUnitId);
            if (requestedQuantity.compareTo(fixedQuantity) < 0) {
                throw invalid("Đây là nguyên liệu tối thiểu để hoàn thành công thức: "
                        + fixedQuantity.stripTrailingZeros().toPlainString() + " " + requestedAmount.unitCode() + ".");
            }
            BigDecimal flexibleQuantity = requestedQuantity.subtract(fixedQuantity);
            BigDecimal targetUnknownQuantity = flexibleQuantity.subtract(oldManualQuantity).max(BigDecimal.ZERO)
                    .min(oldUnknownQuantity);
            item.setQuantity(requestedQuantity);
            item.setUnitId(targetUnitId);
            BigDecimal remainingUnknown = replaceUnknownContribution(listId, unknownSources,
                    targetUnknownQuantity, targetUnitId, item.getIngredientId());
            replaceManualContribution(listId, item, manualSources,
                    flexibleQuantity.subtract(remainingUnknown), targetUnitId);
        } else if (!manualSources.isEmpty()) {
            item.setQuantity(request.quantity());
            item.setUnitId(request.unitId());
            replaceManualContribution(listId, item, manualSources, request.quantity(), request.unitId());
        } else {
            item.setQuantity(request.quantity());
            item.setUnitId(request.unitId());
        }
        touch(list);
        return resolve(List.of(item)).getFirst();
    }

    private BigDecimal convertIngredientQuantity(long ingredientId, BigDecimal quantity, int fromUnitId, int toUnitId) {
        if (fromUnitId == toUnitId) return quantity;
        var options = catalog.findActiveIngredientUnits(ingredientId);
        var from = options.stream().filter(option -> option.id() == fromUnitId).findFirst().orElse(null);
        var to = options.stream().filter(option -> option.id() == toUnitId).findFirst().orElse(null);
        if (from == null || to == null || from.gramsPerUnit() == null || to.gramsPerUnit() == null
                || from.gramsPerUnit().signum() <= 0 || to.gramsPerUnit().signum() <= 0) {
            throw invalid("Không thể quy đổi đơn vị an toàn cho nguyên liệu này.");
        }
        return quantity.multiply(from.gramsPerUnit()).divide(to.gramsPerUnit(), 6, RoundingMode.HALF_UP);
    }

    private void validateItemUnit(ShoppingListItemEntity item, Integer requestedUnitId) {
        if (item.getIngredientId() == null) {
            if (!item.getUnitId().equals(requestedUnitId)) requireActiveUnit(requestedUnitId);
            return;
        }
        if (catalog.findActiveIngredientUnits(item.getIngredientId()).stream()
                .noneMatch(unit -> unit.id().equals(requestedUnitId))) {
            throw invalid("Đơn vị này không được hỗ trợ cho nguyên liệu đã chọn.");
        }
    }

    private tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item aggregateSingle(
            List<ShoppingAggregationLine> lines) {
        var output = aggregateMaybe(lines);
        if (output == null) throw invalid("Không thể quy đổi các định lượng này một cách an toàn.");
        return output;
    }

    private tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item aggregateMaybe(
            List<ShoppingAggregationLine> lines) {
        var result = aggregation.aggregate(lines);
        var outputs = result.groups().stream().flatMap(group -> group.items().stream()).toList();
        return outputs.size() == 1 && !outputs.getFirst().incompatibleUnits() ? outputs.getFirst() : null;
    }

    private void replaceManualContribution(long listId, ShoppingListItemEntity item,
            List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity> manualSources,
            BigDecimal quantity, Integer unitId) {
        if (quantity.signum() <= 0) {
            contributions.deleteAll(manualSources);
            return;
        }
        var source = manualSources.isEmpty()
                ? new tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity()
                : manualSources.getFirst();
        source.setShoppingListId(listId);
        source.setShoppingListItemId(item.getId());
        source.setSourceId(source.getSourceId() == null ? java.util.UUID.randomUUID() : source.getSourceId());
        source.setSourceKind("MANUAL");
        source.setRecipeId(null);
        source.setIngredientId(item.getIngredientId());
        source.setExpectedIngredientCount(null);
        source.setServings(null);
        source.setMealPlanEntryId(null);
        source.setMealDate(null);
        source.setMealType(null);
        source.setQuantity(quantity);
        source.setUnitId(unitId);
        contributions.save(source);
        if (manualSources.size() > 1) contributions.deleteAll(manualSources.subList(1, manualSources.size()));
    }

    /** Shrink orphaned recipe amounts proportionally; the unresolved bucket is never increased by an edit. */
    private BigDecimal replaceUnknownContribution(long listId,
            List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity> unknownSources,
            BigDecimal quantity, Integer unitId, long ingredientId) {
        if (unknownSources.isEmpty() || quantity.signum() == 0) {
            contributions.deleteAll(unknownSources);
            return BigDecimal.ZERO;
        }
        var existing = aggregateSingle(unknownSources.stream().map(source -> new ShoppingAggregationLine(
                source.getIngredientId(), null, source.getQuantity(), source.getUnitId())).toList());
        BigDecimal existingQuantity = convertIngredientQuantity(ingredientId, existing.quantity(), existing.unitId(), unitId);
        if (quantity.compareTo(existingQuantity) > 0) {
            throw invalid("Không thể tăng lượng mất xác định hoặc quy đổi lượng này an toàn.");
        }
        if (quantity.compareTo(existingQuantity) == 0) return existingQuantity;

        BigDecimal ratio = quantity.divide(existingQuantity, 18, RoundingMode.DOWN);
        var retained = new java.util.ArrayList<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity>();
        var removed = new java.util.ArrayList<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity>();
        for (var source : unknownSources) {
            BigDecimal reduced = source.getQuantity().multiply(ratio).setScale(6, RoundingMode.DOWN);
            if (reduced.signum() == 0) removed.add(source);
            else {
                source.setQuantity(reduced);
                retained.add(source);
            }
        }
        contributions.deleteAll(removed);
        if (!retained.isEmpty()) contributions.saveAll(retained);
        if (retained.isEmpty()) return BigDecimal.ZERO;
        var remaining = aggregateSingle(retained.stream().map(source -> new ShoppingAggregationLine(
                source.getIngredientId(), null, source.getQuantity(), source.getUnitId())).toList());
        BigDecimal remainingQuantity = convertIngredientQuantity(ingredientId, remaining.quantity(), remaining.unitId(), unitId);
        if (remainingQuantity.compareTo(quantity) > 0) {
            throw invalid("Không thể cập nhật lượng mất xác định với đơn vị đã chọn.");
        }
        return remainingQuantity;
    }

    @Transactional
    public Item updateManualSource(long listId, java.util.UUID sourceId, UpdateShoppingItemRequest request) {
        var list = owned(listId, true);
        var source = manualSource(listId, sourceId);
        validateQuantity(request.quantity());
        if (!source.getUnitId().equals(request.unitId())) requireActiveUnit(request.unitId());
        var item = item(listId, source.getShoppingListItemId());
        if (item.getIngredientId() != null && catalog.findActiveIngredientUnits(item.getIngredientId()).stream()
                .noneMatch(unit -> unit.id().equals(request.unitId()))) {
            throw invalid("Đơn vị này không được hỗ trợ cho nguyên liệu đã chọn.");
        }
        source.setQuantity(request.quantity());
        source.setUnitId(request.unitId());
        recalculateFromSources(listId, item);
        touch(list);
        return resolve(List.of(item(listId, item.getId()))).getFirst();
    }

    @Transactional
    public void deleteManualSource(long listId, java.util.UUID sourceId) {
        var list = owned(listId, true);
        var source = manualSource(listId, sourceId);
        Long itemId = source.getShoppingListItemId();
        contributions.delete(source);
        if (itemId != null) {
            var linked = contributions.findByShoppingListIdOrderByIdAsc(listId).stream()
                    .filter(row -> itemId.equals(row.getShoppingListItemId())).toList();
            if (linked.isEmpty()) items.deleteById(itemId);
            else recalculateFromSources(listId, item(listId, itemId));
        }
        touch(list);
    }

    @Transactional
    public Item setBought(long listId, long itemId, boolean bought) {
        var list = owned(listId, true);
        var item = item(listId, itemId);
        if (item.isBought() != bought) {
            item.setBought(bought);
            touch(list);
        }
        return resolve(List.of(item)).getFirst();
    }

    @Transactional
    public void delete(long listId, long itemId) {
        var list = owned(listId, true);
        var row = item(listId, itemId);
        detachSources(listId, java.util.Set.of(row.getId()));
        items.delete(row);
        touch(list);
    }

    @Transactional
    public void clearBought(long listId) {
        var list = owned(listId, true);
        var bought = items.findByShoppingListIdOrderByIdAsc(listId).stream().filter(ShoppingListItemEntity::isBought).toList();
        if (!bought.isEmpty()) {
            detachSources(listId, bought.stream().map(ShoppingListItemEntity::getId).collect(Collectors.toSet()));
            items.deleteAll(bought);
            touch(list);
        }
    }

    private ShoppingListEntity owned(long id, boolean lock) {
        long owner = users.requireActiveMember().id();
        if (id < 1) throw invalid("listId phải > 0.");
        // Same response for missing and foreign resources; never disclose their contents.
        return (lock ? lists.lockOwned(id, owner) : lists.findByIdAndUserId(id, owner))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private ShoppingListItemEntity item(long listId, long itemId) {
        if (itemId < 1) throw invalid("itemId phải > 0.");
        return items.findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private void requireActiveUnit(Integer id) {
        if (id == null || id < 1 || catalog.findActiveUnits().stream().noneMatch(u -> u.id().equals(id))) {
            throw invalid("Chọn đơn vị đang hoạt động trong danh mục.");
        }
    }

    private tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity manualSource(long listId, java.util.UUID sourceId) {
        if (sourceId == null) throw new AppException(ErrorCode.NOT_FOUND);
        return contributions.findByShoppingListIdOrderByIdAsc(listId).stream()
                .filter(row -> "MANUAL".equals(row.getSourceKind()) && sourceId.equals(row.getSourceId()))
                .findFirst().orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private void recalculateFromSources(long listId, ShoppingListItemEntity item) {
        var linked = contributions.findByShoppingListIdOrderByIdAsc(listId).stream()
                .filter(row -> item.getId().equals(row.getShoppingListItemId())).toList();
        if (item.getIngredientId() == null) {
            if (linked.isEmpty() || linked.stream().anyMatch(row -> row.getIngredientId() != null
                    || !row.getUnitId().equals(linked.getFirst().getUnitId()))) {
                throw invalid("Không thể gộp an toàn các đơn vị của mặt hàng tùy chỉnh.");
            }
            item.setQuantity(linked.stream().map(tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setUnitId(linked.getFirst().getUnitId());
            item.setBought(false);
            return;
        }
        var lines = linked.stream().filter(row -> row.getIngredientId() != null)
                .map(row -> new ShoppingAggregationLine(row.getIngredientId(), null, row.getQuantity(), row.getUnitId())).toList();
        if (lines.size() != linked.size() || lines.isEmpty()) {
            throw invalid("Không thể tính lại an toàn số lượng nguyên liệu đã gộp.");
        }
        var result = aggregation.aggregate(lines);
        var outputs = result.groups().stream().flatMap(group -> group.items().stream()).toList();
        if (outputs.size() != 1 || outputs.getFirst().quantity().signum() <= 0) {
            throw invalid("Không thể gộp các đơn vị của nguyên liệu này an toàn.");
        }
        item.setQuantity(outputs.getFirst().quantity());
        item.setUnitId(outputs.getFirst().unitId());
        item.setBought(false);
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 6
                || quantity.compareTo(new BigDecimal("999999999999.999999")) > 0) {
            throw invalid("Số lượng phải > 0, tối đa 99999999,99 và không quá 2 chữ số thập phân.");
        }
    }

    private void touch(ShoppingListEntity list) { list.setUpdatedAt(LocalDateTime.now(clock)); }

    private String normalizeListName(String value) {
        String name = value == null ? "" : value.replace('\u00a0', ' ').strip();
        if (name.isBlank() || name.length() > 200 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("Tên danh sách không được để trống và tối đa 200 ký tự.");
        }
        return name;
    }

    private void detachSources(long listId, java.util.Set<Long> ids) {
        var rows = contributions.findByShoppingListIdOrderByIdAsc(listId);
        var affected = rows.stream().filter(c -> c.getShoppingListItemId()!=null && ids.contains(c.getShoppingListItemId())).toList();
        var affectedRecipeRows = affected.stream().filter(c -> "RECIPE".equals(c.getSourceKind())).toList();
        var incompleteSourceIds = affectedRecipeRows.stream()
                .map(tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getSourceId)
                .collect(Collectors.toSet());
        var sourceRows = rows.stream().filter(c -> "RECIPE".equals(c.getSourceKind())
                && incompleteSourceIds.contains(c.getSourceId())).toList();
        for (var source : sourceRows) {
            if (source.getShoppingListItemId() != null && ids.contains(source.getShoppingListItemId())) {
                source.setShoppingListItemId(null); // retain a tombstone so the incomplete recipe stays hidden
            } else {
                source.setSourceKind("UNKNOWN"); // keep the surviving amount editable but distinct from manual
            }
        }
        contributions.saveAllAndFlush(sourceRows);
        contributions.deleteAll(affected.stream().filter(c -> "MANUAL".equals(c.getSourceKind())
                || "UNKNOWN".equals(c.getSourceKind())).toList());
    }

    private List<Item> resolve(List<ShoppingListItemEntity> rows) {
        var sourceRows = rows.isEmpty() ? List.<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity>of()
                : contributions.findByShoppingListIdOrderByIdAsc(rows.getFirst().getShoppingListId());
        var recipeIds = sourceRows.stream().filter(c -> "RECIPE".equals(c.getSourceKind())
                        || "UNKNOWN".equals(c.getSourceKind()))
                .map(tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getRecipeId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        var sourceRecipes = recipeIds.isEmpty() ? Map.<Long,tech.mamxanh.recipe.service.RecipeService.ShoppingReference>of()
                : recipes.findShoppingReferences(recipeIds);
        Map<Long, IngredientResponse> ingredients = catalog.findIngredientReferences(rows.stream()
                .map(ShoppingListItemEntity::getIngredientId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(IngredientResponse::id, Function.identity()));
        Map<Integer, UnitResponse> units = catalog.findUnitReferences(java.util.stream.Stream.concat(rows.stream()
                .map(ShoppingListItemEntity::getUnitId), sourceRows.stream().map(c -> c.getUnitId())).distinct().toList())
                .stream().collect(Collectors.toMap(UnitResponse::id, Function.identity()));
        return rows.stream().map(row -> {
            var ingredient = ingredients.get(row.getIngredientId());
            var unit = units.get(row.getUnitId());
            var linked = sourceRows.stream().filter(c -> row.getId().equals(c.getShoppingListItemId())).toList();
            var required = contributionAmount(linked, "RECIPE", row);
            var manual = contributionAmount(linked, "MANUAL", row);
            if (required != null && required.unitId() != row.getUnitId()) required = null;
            BigDecimal manualQuantity = manual == null ? null : manual.quantity();
            String manualUnitCode = manual == null ? null : manual.unitCode();
            return new Item(row.getId(), row.getIngredientId(),
                    ingredient == null ? row.getCustomIngredientName() : ingredient.name(),
                    ingredient == null ? "Nhóm khác" : ingredient.ingredientGroup(),
                    ingredient == null ? null : ingredient.imageUrl(), row.getQuantity(), row.getUnitId(),
                    unit == null ? null : unit.code(), unit == null ? null : unit.name(), row.isBought(),
                    required == null ? null : required.quantity(), required == null ? null : required.unitCode(),
                    manualQuantity, manualUnitCode,
                    linked.stream().map(c -> {
                        var recipe = c.getRecipeId() == null ? null : sourceRecipes.get(c.getRecipeId());
                        var originalUnit = units.get(c.getUnitId());
                        return new ShoppingListResponse.Source(c.getSourceKind(), c.getSourceId(), c.getRecipeId(),
                                c.getIngredientId(),
                                recipe == null ? null : recipe.title(), recipe != null && recipe.available(),
                                c.getQuantity(), c.getUnitId(), originalUnit == null ? null : originalUnit.code(),
                                c.getServings());
                    }).toList());
        }).toList();
    }

    private tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item contributionAmount(
            List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity> linked,
            String sourceKind, ShoppingListItemEntity item) {
        var selected = linked.stream().filter(source -> sourceKind.equals(source.getSourceKind())).toList();
        if (selected.isEmpty()) return null;
        if (item.getIngredientId() != null) {
            var amount = aggregateMaybe(selected.stream().map(source -> new ShoppingAggregationLine(
                    source.getIngredientId(), null, source.getQuantity(), source.getUnitId())).toList());
            if (amount == null) return null;
            var displayUnit = catalog.findUnitReferences(List.of(item.getUnitId())).stream().findFirst().orElse(null);
            if (displayUnit == null) return null;
            BigDecimal displayQuantity = convertIngredientQuantity(item.getIngredientId(), amount.quantity(), amount.unitId(), item.getUnitId());
            return new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item(amount.ingredientId(), amount.name(),
                    displayQuantity, displayUnit.id(), displayUnit.code(), displayUnit.name(), amount.incompatibleUnits(), amount.sourceIndices());
        }
        if (selected.stream().anyMatch(source -> !source.getUnitId().equals(selected.getFirst().getUnitId()))) return null;
        var unit = catalog.findUnitReferences(List.of(selected.getFirst().getUnitId())).stream().findFirst().orElse(null);
        if (unit == null) return null;
        return new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item(null, item.getCustomIngredientName(),
                selected.stream().map(tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), unit.id(), unit.code(), unit.name(), false,
                java.util.stream.IntStream.range(0, selected.size()).boxed().toList());
    }

    private List<ShoppingListResponse.RecipeSource> resolveRecipeSources(
            List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity> rows) {
        if (rows.isEmpty()) return List.of();
        var recipeRows = rows.stream().filter(c -> "RECIPE".equals(c.getSourceKind()) && c.getRecipeId() != null).toList();
        if (recipeRows.isEmpty()) return List.of();
        var references = recipes.findShoppingReferences(recipeRows.stream().map(c -> c.getRecipeId()).distinct().toList());
        var byRecipe = recipeRows.stream().collect(Collectors.groupingBy(
                tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getRecipeId,
                java.util.LinkedHashMap::new, Collectors.toList()));
        return byRecipe.entrySet().stream().map(entry -> {
            var recipeContributions = entry.getValue();
            var bySource = recipeContributions.stream().collect(Collectors.toMap(
                    tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getSourceId,
                    Function.identity(), (first, ignored) -> first, java.util.LinkedHashMap::new));
            boolean complete = bySource.values().stream().allMatch(source -> {
                List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity> sourceRows = recipeContributions.stream()
                        .filter(row -> row.getSourceId().equals(source.getSourceId())).toList();
                return source.getExpectedIngredientCount() != null
                        && sourceRows.size() == source.getExpectedIngredientCount()
                        && sourceRows.stream().allMatch(row -> row.getShoppingListItemId() != null);
            });
            if (!complete) return null;
            boolean knownServings = bySource.values().stream().allMatch(c -> c.getServings() != null);
            BigDecimal totalServings = knownServings ? bySource.values().stream()
                    .map(tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getServings)
                    .reduce(BigDecimal.ZERO, BigDecimal::add) : null;
            var first = recipeContributions.getFirst();
            var recipe = references.get(entry.getKey());
            return new ShoppingListResponse.RecipeSource(first.getSourceId(), entry.getKey(),
                    recipe == null ? null : recipe.title(), recipe != null && recipe.available(),
                    recipe == null ? null : recipe.coverUrl(), totalServings, bySource.size(), null, null);
        }).filter(java.util.Objects::nonNull).toList();
    }

    private ShoppingListItemEntity createItem(long listId, Long ingredientId, String customName,
            BigDecimal quantity, Integer unitId) {
        var item = new ShoppingListItemEntity();
        item.setShoppingListId(listId);
        item.setIngredientId(ingredientId);
        item.setCustomIngredientName(customName);
        item.setQuantity(quantity);
        item.setUnitId(unitId);
        item.setBought(false);
        return items.saveAndFlush(item);
    }

    private String normalizeItemName(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').strip().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

    private AppException invalid(String detail) { return new AppException(ErrorCode.VALIDATION_FAILED, detail); }
}
